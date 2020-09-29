package com.gd.aiwnext.deal.Support.Managers

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.*
import com.gd.aiwnext.deal.BuildConfig
import com.gd.aiwnext.deal.R
import com.gd.aiwnext.deal.Support.Extensions.ex
import com.gd.aiwnext.deal.Support.Utils.Utils

class AuthManager(private val preferencesManager: PreferencesManager) {

    companion object {

        const val GOOGLE_WEB_CLIEND_ID =
            "removed"

        const val REMEOWNDER_LINK = "removed"
        const val EMAIL_CONFIRMED = "removed"
        const val PWD_CHANGED = "removed"
    }

    private var auth: FirebaseAuth? = null

    private var context: Context? = null

    fun initialize(context: Context) {
        auth = FirebaseAuth.getInstance()
        refresh { }
        this.context = context
    }

    fun checkAuth(): Boolean {
        if (!BuildConfig.DEBUG) {
            return auth?.currentUser?.isEmailVerified ?: false
        }
        return false
    }

    fun isGoogle(): Boolean {
        if (auth?.currentUser != null) {
            return auth?.currentUser!!.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
        }
        return false
    }

    fun getEmail() = auth?.currentUser?.email

    private fun downloadPhoto() {
        auth?.currentUser?.photoUrl?.toString()?.let {
            Utils.loadGooglePhoto(it) { photo ->
                if (photo != null) {
                    preferencesManager.set(PreferencesManager.PHOTO_GOOGLE, Utils.imageToString(photo))
                }
            }
        }
    }

    private fun createProfileImage() {
        val rand = (0..6).random()
        preferencesManager.set(PreferencesManager.PROFILE_PICTURE, rand)
        preferencesManager.set(PreferencesManager.SETTINGS_LOCALLY, true)
    }

    fun signUpEmail(email: String, password: String, onSignUp: () -> Unit, onError: (err: String?) -> Unit) {
        auth?.createUserWithEmailAndPassword(email, password)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    verifyEmail()
                    createProfileImage()
                    onSignUp()
                }
            }?.addOnFailureListener { e ->
                if (e != null) {
                    ex ({ onError(getErrorMessage(e))}, {context?.resources?.getString(R.string.UnknownError)})
                } else {
                    onError(context?.resources?.getString(R.string.UnknownError))
                }
            }
    }

    fun verifyEmail() {
        val options = ActionCodeSettings.newBuilder()
            .setUrl("$REMEOWNDER_LINK$EMAIL_CONFIRMED")
            .setAndroidPackageName(context!!.packageName, true, null)
            .build()
        auth?.currentUser?.sendEmailVerification(options)
    }

    fun signInEmail(email: String, password: String, onLogin: () -> Unit, onEmailNotVerified: () -> Unit, onError: (err: String?) -> Unit) {
        auth?.signInWithEmailAndPassword(email, password)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth?.currentUser
                    if (user!!.isEmailVerified) {
                        listProviders()
                        onLogin()
                    } else {
                        verifyEmail()
                        onEmailNotVerified()
                    }
                }
            }?.addOnFailureListener { e ->
                if (e != null) {
                    ex ({ onError(getErrorMessage(e))}, {context?.resources?.getString(R.string.UnknownError)})
                } else {
                    onError(context?.resources?.getString(R.string.UnknownError))
                }
            }
    }

    fun signInGoogle(account: GoogleSignInAccount, onLogin: () -> Unit, onError: (err: String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                listProviders()
                onLogin()
                downloadPhoto()
            }
        }?.addOnFailureListener { e ->
            if (e != null) {
                ex ({ onError(getErrorMessage(e))}, {context?.resources?.getString(R.string.UnknownError)})
            } else {
                onError(context?.resources?.getString(R.string.UnknownError))
            }
        }
    }

    fun signOut() {
        auth?.signOut()
        if (auth?.currentUser == null) { } else { }
    }

    fun status() {
        listProviders()
    }

    fun changePass(email: String, onLinkSent: () -> Unit) {
        val options = ActionCodeSettings.newBuilder()
            .setUrl("$REMEOWNDER_LINK$PWD_CHANGED")
            .setAndroidPackageName(context!!.packageName, true, null)
            .build()
        auth?.sendPasswordResetEmail(email, options)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onLinkSent()
            }
        }
    }

    fun refresh(onReload: () -> Unit) {
        if (!BuildConfig.DEBUG) {
            auth?.currentUser?.reload()?.addOnCompleteListener { task ->
                onReload()
            }
        }
    }

    fun uid() = auth?.currentUser?.uid

    private fun listProviders() {
        var providers = ""
        if (auth?.currentUser != null) {
            for (p in auth?.currentUser!!.providerData) {
                if (p.providerId == EmailAuthProvider.PROVIDER_ID || p.providerId == GoogleAuthProvider.PROVIDER_ID) {
                    providers += p.providerId + " "
                }
            }
        }
    }

    private fun getErrorMessage(e: Exception) = when ((e as FirebaseAuthException).errorCode) {
        "ERROR_USER_NOT_FOUND" -> context?.resources?.getString(R.string.UserNotExist)
        "ERROR_USER_TOKEN_EXPIRED" -> context?.resources?.getString(R.string.ChangedSomething)
        "ERROR_EMAIL_ALREADY_IN_USE" -> context?.resources?.getString(R.string.EmailAlreadyUsed)
        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> context?.resources?.getString(R.string.LogInGoogle)
        "ERROR_WRONG_PASSWORD" -> context?.resources?.getString(R.string.WrongPwd)
        else -> context?.resources?.getString(R.string.UnknownError)
    }
}
