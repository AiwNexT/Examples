package com.gd.aiwnext.deal.Support.Managers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.fragment.app.FragmentManager
import com.gd.aiwnext.deal.Dialogs.BatteryOptimizationSheet
import com.gd.aiwnext.deal.Support.Extensions.ex

class OptimizationManager(private val context: Context,
                          private val preferencesManager: PreferencesManager) {

    private var pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun isAppWhitelisted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return pm.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true
    }

    private fun openBatterySettings() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ex { context.startActivity(intent) }
    }

    fun showOptimizationSheet(mgr: FragmentManager) {
        BatteryOptimizationSheet().apply {
            attachListeners({ openBatterySettings() },
                { preferencesManager.set(PreferencesManager.OPTIMIZATION_DIALOG_SHOWN, true) })
            isCancelable = false
            show(mgr, "")
        }
    }
}
