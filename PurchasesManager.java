package com.gd.aiwnext.deal.Support.Managers;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.gd.aiwnext.deal.App;
import com.gd.aiwnext.deal.Support.Utils.Listener;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class PurchasesManager implements PurchasesUpdatedListener {

    public static final int MONTHLY = 1;
    public static final int HALF_YEAR = 2;
    public static final int ANNUAL = 3;

    private final String MONTHLY_SUB = "removed";
    private final String SIX_MONTHS_SUB = "removed";
    private final String ANNUAL_SUB = "removed";

    private List<SkuDetails> productsDetails = new ArrayList<>();

    private BillingClient billingClient;

    private App app;

    private Listener onPaymentFailed;
    private Listener onPaymentSuccessful;

    private SharedPreferences DealPrefs;

    private PreferencesManager preferencesManager;

    public PurchasesManager(App app) {
        this.app = app;
        preferencesManager = app.preferencesManager;
    }

    public void initialize() {
        DealPrefs = app.getSharedPreferences(PreferencesManager.DEAL_PREFS, MODE_PRIVATE);
        billingClient = BillingClient.newBuilder(app).setListener(this).enablePendingPurchases().build();
        startConnection();
    }

    public void setOnPaymentListener(Listener onPaymentFailed, Listener onPaymentSuccessful) {
        this.onPaymentFailed = onPaymentFailed;
        this.onPaymentSuccessful = onPaymentSuccessful;
    }

    public void removeListeners() {
        onPaymentFailed = null;
        onPaymentSuccessful = null;
    }

    private void startConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    querySkuDetails();
                    queryPurchases();
                }
            }
            @Override
            public void onBillingServiceDisconnected() { }
        });
    }

    public boolean purchaseProduct(int type, AppCompatActivity activity) {
        SkuDetails productSkuDetails = null;
        for (SkuDetails skuDetails : productsDetails) {
            if (skuDetails.getSku().equals(type == MONTHLY ? MONTHLY_SUB : type == HALF_YEAR ? SIX_MONTHS_SUB : ANNUAL_SUB)) {
                productSkuDetails = skuDetails;
                break;
            }
        }
        if (productSkuDetails != null) {
            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(productSkuDetails)
                    .build();
            int responseCode = billingClient.launchBillingFlow(activity, flowParams).getResponseCode();
            return responseCode == BillingClient.BillingResponseCode.OK;
        } else {
            return false;
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK & purchases != null) {
            for (Purchase purchase: purchases) {
                handlePurchase(purchase);
            }
        } else {
            if (onPaymentFailed != null) {
                try {
                   onPaymentFailed.onEvent();
                } catch (Exception ignored) {}
            }
        }
    }

    private void querySkuDetails() {
        List<String> skuList = new ArrayList<>();
        skuList.add(MONTHLY_SUB);
        skuList.add(SIX_MONTHS_SUB);
        skuList.add(ANNUAL_SUB);
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS);
        billingClient.querySkuDetailsAsync(params.build(), (billingResult, skuDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                productsDetails.clear();
                productsDetails.addAll(skuDetailsList);
            }
        });
    }

    private void queryPurchases() {
        Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
        List<Purchase> purchases = new ArrayList<>();
        List <Purchase> inapps = purchasesResult.getPurchasesList();
        if (inapps != null) {
            purchases.addAll(inapps);
        }
        if (billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).getResponseCode() == BillingClient.BillingResponseCode.OK) {
            Purchase.PurchasesResult subscriptionsResult = billingClient.queryPurchases(BillingClient.SkuType.SUBS);
            if (subscriptionsResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                List <Purchase> subs = subscriptionsResult.getPurchasesList();
                if (subs != null) {
                    purchases.addAll(subs);
                }
            }
        }
        if (purchases.isEmpty()) {
            if (!preferencesManager.get(PreferencesManager.TEST_MODE, false) |
                    !preferencesManager.get(PreferencesManager.ETERNAL_PRO, false)) {
                DealPrefs.edit().putBoolean(PreferencesManager.DEAL_PREFS, false).apply();
            }
        } else {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        }
    }

    public String getSubPrice(int type) {
        String prefsKey = type == MONTHLY ? PreferencesManager.LAST_MONTHLY_PRICE :
                type == HALF_YEAR ? PreferencesManager.LAST_SIXMONTHS_PRICE : PreferencesManager.LAST_ANNUAL_PRICE;
        if (productsDetails.size() > 0) {
            for (SkuDetails product: productsDetails) {
                if (product.getSku().equals(type == MONTHLY ? MONTHLY_SUB : type == HALF_YEAR ? SIX_MONTHS_SUB : ANNUAL_SUB)) {
                    preferencesManager.set(prefsKey, product.getPrice());
                    return product.getPrice();
                }
            }
            return null;
        } else {
            String lastPrice = preferencesManager.get(prefsKey, "");
            if (!lastPrice.isEmpty()) {
                return lastPrice;
            } else {
                return type == MONTHLY ? "$2" : type == HALF_YEAR ? "$9" : "$14";
            }
        }
    }

    public String getMonthlyIntroductoryPrice() {
        String prefsKey = PreferencesManager.LAST_INTRODUCTORY_PRICE;
        if (productsDetails.size() > 0) {
            for (SkuDetails product: productsDetails) {
                if (product.getSku().equals(MONTHLY_SUB)) {
                    preferencesManager.set(prefsKey, product.getIntroductoryPrice());
                    return product.getIntroductoryPrice();
                }
            }
            return null;
        } else {
            return preferencesManager.get(prefsKey, "$1");
        }
    }

    public void queryRequest() {
        if (billingClient.isReady()) {
            querySkuDetails();
            queryPurchases();
        } else {
            startConnection();
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {

            if (!DealPrefs.getBoolean(PreferencesManager.DEAL_PREFS, false)) {
                DealPrefs.edit().putBoolean(PreferencesManager.DEAL_PREFS, true).apply();

                if (onPaymentSuccessful != null) {
                    try {
                        onPaymentSuccessful.onEvent();
                    } catch (Exception ignored) { }
                }
            }

            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) { }
                });
            }
        }
    }
}
