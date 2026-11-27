package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.FirebaseInitState
import com.example.MurshidabadApplication
import com.example.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

object AuthHelper {
    private const val TAG = "MurshidabadAuth"

    fun createCredentialManager(context: Context): CredentialManager? = try {
        CredentialManager.create(context)
    } catch (t: Throwable) {
        Log.e(TAG, "Failed to initialize CredentialManager: ${t.message}", t)
        null
    }

    fun getSafeFirebaseAuth(context: Context): FirebaseAuth? {
        val initState = MurshidabadApplication.ensureFirebaseInitialized(context)
        if (initState !is FirebaseInitState.Ready) {
            return null
        }
        return try {
            Firebase.auth
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to obtain FirebaseAuth instance: ${t.message}", t)
            null
        }
    }

    fun authStateFlow(auth: FirebaseAuth): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun isPlayServicesAvailable(context: Context): Pair<Boolean, String?> = try {
        val api = GoogleApiAvailability.getInstance()
        val status = api.isGooglePlayServicesAvailable(context)
        if (status == ConnectionResult.SUCCESS) {
            true to null
        } else {
            val statusStr = api.getErrorString(status)
            Log.w(TAG, "Google Play Services unavailable (code=$status, msg=$statusStr)")
            false to "Google Play Services is unavailable or outdated ($statusStr). Update Play Services to sign in with Google."
        }
    } catch (t: Throwable) {
        Log.e(TAG, "Play Services check failed: ${t.message}", t)
        false to "Unable to verify Google Play Services on this device."
    }

    private fun isNetworkAvailable(context: Context): Boolean = try {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork
        val caps = if (network != null) cm.getNetworkCapabilities(network) else null
        caps != null && (
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            )
    } catch (t: Throwable) {
        Log.w(TAG, "Network connectivity check failed: ${t.message}")
        true
    }

    private fun resolveWebClientId(context: Context): String? {
        val clientId = try {
            context.getString(R.string.default_web_client_id).trim()
        } catch (e: Exception) {
            Log.e(TAG, "Missing R.string.default_web_client_id in generated resources", e)
            return null
        }
        if (clientId.isBlank() || clientId.contains("YOUR_") || !clientId.endsWith(".apps.googleusercontent.com")) {
            Log.e(TAG, "Invalid default_web_client_id value: '$clientId'")
            return null
        }
        return clientId
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager?,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        val auth = getSafeFirebaseAuth(context)
        if (auth == null) {
            Log.w(TAG, "Skipping auto sign-in: FirebaseAuth not initialized")
            onUnauthenticated()
            return
        }
        if (auth.currentUser != null) {
            Log.d(TAG, "User already signed in: uid=${auth.currentUser?.uid}")
            onAuthSuccess()
            return
        }
        if (credentialManager == null) {
            Log.w(TAG, "Skipping auto sign-in: CredentialManager unavailable")
            onUnauthenticated()
            return
        }
        val (gmsOk, gmsMsg) = isPlayServicesAvailable(context)
        if (!gmsOk) {
            Log.w(TAG, "Skipping auto sign-in: $gmsMsg")
            onUnauthenticated()
            return
        }
        if (!isNetworkAvailable(context)) {
            Log.i(TAG, "Skipping auto sign-in: device appears offline")
            onUnauthenticated()
            return
        }
        val activity = context.findActivity()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Log.w(TAG, "Skipping auto sign-in: no active host Activity")
            onUnauthenticated()
            return
        }
        val clientId = resolveWebClientId(context)
        if (clientId == null) {
            onUnauthenticated()
            return
        }

        val googleIdOption = try {
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to build GetGoogleIdOption: ${t.message}", t)
            onUnauthenticated()
            return
        }

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val cred = credentialManager.getCredential(activity, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    Log.i(TAG, "Auto Google Sign-In succeeded: uid=${auth.currentUser?.uid}")
                    onAuthSuccess()
                } else {
                    Log.w(TAG, "Auto sign-in returned unsupported credential type: ${cred.type}")
                    onUnauthenticated()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: NoCredentialException) {
                Log.i(TAG, "No previously authorized Google credential found on device; showing Sign-In screen")
                onUnauthenticated()
            } catch (e: GetCredentialCancellationException) {
                Log.i(TAG, "Auto sign-in dismissed by user")
                onUnauthenticated()
            } catch (e: GetCredentialProviderConfigurationException) {
                Log.w(TAG, "Credential Manager provider unavailable on device: ${e.message}", e)
                onUnauthenticated()
            } catch (e: GetCredentialUnsupportedException) {
                Log.w(TAG, "Credential Manager unsupported on this device: ${e.message}", e)
                onUnauthenticated()
            } catch (e: GetCredentialException) {
                Log.w(TAG, "Auto sign-in GetCredentialException (${e.type}): ${e.errorMessage ?: e.message}", e)
                onUnauthenticated()
            } catch (t: Throwable) {
                Log.w(TAG, "Auto sign-in failed safely: ${t.message}", t)
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager?,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        val auth = getSafeFirebaseAuth(context)
        if (auth == null) {
            onAuthError("Firebase Authentication is not initialized. Check google-services.json and restart the app.")
            return
        }
        if (credentialManager == null) {
            onAuthError("Google Credential Manager is not supported on this device.")
            return
        }
        val (gmsOk, gmsMsg) = isPlayServicesAvailable(context)
        if (!gmsOk) {
            onAuthError(gmsMsg ?: "Google Play Services is unavailable.")
            return
        }
        if (!isNetworkAvailable(context)) {
            onAuthError("No internet connection. Connect to Wi-Fi or mobile data and try again.")
            return
        }
        val activity = context.findActivity()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Log.e(TAG, "Google Sign-In clicked without valid Activity context")
            onAuthError("Unable to open Google Sign-In dialog from current screen.")
            return
        }
        val clientId = resolveWebClientId(context)
        if (clientId == null) {
            onAuthError("Google Sign-In OAuth client ID is missing or invalid (default_web_client_id).")
            return
        }

        val signInOption = try {
            GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to build GetSignInWithGoogleOption: ${t.message}", t)
            onAuthError("Invalid Google OAuth configuration: ${t.localizedMessage ?: "check client ID"}")
            return
        }

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val cred = credentialManager.getCredential(activity, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                    auth.signInWithCredential(GoogleAuthProvider.getCredential(googleCred.idToken, null)).await()
                    Log.i(TAG, "Interactive Google Sign-In succeeded: uid=${auth.currentUser?.uid}")
                    onAuthSuccess()
                } else {
                    Log.e(TAG, "Unexpected credential type: ${cred.type}")
                    onAuthError("Unsupported credential type returned by Google Sign-In.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GetCredentialCancellationException) {
                Log.i(TAG, "Google Sign-In cancelled by user")
                onAuthCancelled()
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No Google account available or app SHA-1/SHA-256 mismatch: ${e.message}", e)
                onAuthError(
                    "No Google account credential found. Ensure a Google account is signed in on this device and this APK's SHA-1/SHA-256 fingerprint is registered in Firebase Console."
                )
            } catch (e: GetCredentialProviderConfigurationException) {
                Log.e(TAG, "Credential Manager provider configuration error: ${e.message}", e)
                onAuthError("Google Play Services Credential Provider is unavailable or outdated on this device.")
            } catch (e: GetCredentialUnsupportedException) {
                Log.e(TAG, "Credential Manager unsupported: ${e.message}", e)
                onAuthError("Credential Manager is not supported on this device.")
            } catch (e: GoogleIdTokenParsingException) {
                Log.e(TAG, "Invalid Google ID token received: ${e.message}", e)
                onAuthError("Failed to verify Google ID token. Please try again.")
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential Manager error type=${e.type}: ${e.errorMessage}", e)
                val msg = e.errorMessage?.toString().orEmpty()
                val friendly = when {
                    msg.contains("DEVELOPER_ERROR", ignoreCase = true) || msg.contains("10:") ->
                        "Google Sign-In configuration error (SHA-1/SHA-256 or OAuth client ID mismatch in Firebase Console)."
                    msg.contains("network", ignoreCase = true) || msg.contains("7:") ->
                        "Network error while contacting Google Sign-In. Please check your internet connection."
                    else -> e.localizedMessage ?: "Google Sign-In failed (${e.type})."
                }
                onAuthError(friendly)
            } catch (e: FirebaseNetworkException) {
                Log.e(TAG, "Firebase network error during sign-in: ${e.message}", e)
                onAuthError("Network unavailable while signing in to Firebase. Please try again.")
            } catch (e: FirebaseAuthException) {
                Log.e(TAG, "FirebaseAuth error code=${e.errorCode}: ${e.message}", e)
                onAuthError("Firebase authentication failed (${e.errorCode}): ${e.localizedMessage ?: "try again"}")
            } catch (t: Throwable) {
                Log.e(TAG, "Unexpected Google Sign-In error: ${t.message}", t)
                onAuthError(t.localizedMessage ?: "Google Sign-In failed unexpectedly.")
            }
        }
    }

    fun signOut(
        context: Context,
        credentialManager: CredentialManager?,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager?.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state on sign-out: ${e.message}", e)
                } finally {
                    try {
                        if (FirebaseApp.getApps(context.applicationContext).isNotEmpty()) {
                            Firebase.auth.signOut()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sign out of FirebaseAuth: ${e.message}", e)
                    }
                    onSignOutComplete()
                }
            }
        }
    }
}
