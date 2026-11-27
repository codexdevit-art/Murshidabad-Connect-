package com.example

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.data.local.UserPreferences
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

sealed interface FirebaseInitState {
    data object Ready : FirebaseInitState
    data class Failed(val message: String, val cause: Throwable? = null) : FirebaseInitState
}

class MurshidabadApplication : Application() {

    val userPreferences: UserPreferences by lazy {
        UserPreferences(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        installCrashDiagnosticLogger()
        logAppStartupDiagnostics()
        ensureFirebaseInitialized(this)
    }

    private fun installCrashDiagnosticLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(
                CRASH_TAG,
                "FATAL UNCAUGHT EXCEPTION on thread=${thread.name}: ${throwable.message}",
                throwable
            )
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun logAppStartupDiagnostics() {
        val versionName = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionName
            }
        }.getOrNull() ?: "unknown"

        Log.i(
            TAG,
            "Startup Murshidabad Connect | package=$packageName | version=$versionName | sdk=${Build.VERSION.SDK_INT}"
        )

        runCatching {
            val gmsStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
            Log.i(TAG, "Google Play Services availability code=$gmsStatus (SUCCESS=${ConnectionResult.SUCCESS})")
        }.onFailure { e ->
            Log.w(TAG, "Unable to query Google Play Services status: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "MurshidabadApp"
        private const val CRASH_TAG = "MurshidabadCrash"

        @Synchronized
        fun ensureFirebaseInitialized(context: Context): FirebaseInitState {
            val appContext = context.applicationContext ?: context
            return try {
                val existing = FirebaseApp.getApps(appContext)
                val app = if (existing.isNotEmpty()) {
                    existing.first()
                } else {
                    Log.i(TAG, "FirebaseApp not yet initialized; calling FirebaseApp.initializeApp()")
                    FirebaseApp.initializeApp(appContext)
                }

                if (app == null) {
                    val msg = "Firebase configuration missing. Verify google-services.json matches package ${appContext.packageName}."
                    Log.e(TAG, msg)
                    FirebaseInitState.Failed(msg)
                } else {
                    val dbId = runCatching { appContext.getString(R.string.firestore_database_id) }.getOrDefault("")
                    Log.i(
                        TAG,
                        "Firebase initialized: projectId=${app.options.projectId}, appId=${app.options.applicationId}, dbId=$dbId"
                    )
                    if (dbId.isBlank()) {
                        val msg = "Firestore database ID resource (firestore_database_id) is missing or blank."
                        Log.e(TAG, msg)
                        FirebaseInitState.Failed(msg)
                    } else {
                        // Validate Firestore instance retrieval eagerly with clear diagnostics
                        FirebaseFirestore.getInstance(app, dbId)
                        FirebaseInitState.Ready
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Firebase initialization failed: ${t.message}", t)
                FirebaseInitState.Failed(
                    t.localizedMessage ?: "Failed to initialize Firebase services.",
                    t
                )
            }
        }
    }
}
