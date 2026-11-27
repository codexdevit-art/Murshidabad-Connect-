package com.example.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.MurshidabadApplication
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MurshidabadMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM token refreshed")
        try {
            if (FirebaseApp.getApps(applicationContext).isEmpty()) {
                MurshidabadApplication.ensureFirebaseInitialized(applicationContext)
            }
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                Log.d(TAG, "FCM token received before user signed in; skipping Firestore sync")
                return
            }
            val dbId = applicationContext.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "fcmToken" to token,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .addOnFailureListener { err ->
                    Log.w(TAG, "Failed to update FCM token in Firestore: ${err.message}", err)
                }
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to sync FCM token safely: ${t.message}", t)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        try {
            val title = remoteMessage.notification?.title
                ?: remoteMessage.data["title"]
                ?: getString(R.string.app_name)
            val body = remoteMessage.notification?.body
                ?: remoteMessage.data["body"]
                ?: remoteMessage.data["message"]
                ?: return

            showLocalNotification(title, body)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to handle incoming FCM message: ${t.message}", t)
        }
    }

    private fun showLocalNotification(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w(TAG, "Skipping local notification because POST_NOTIFICATIONS is not granted")
                return
            }
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (manager == null) {
            Log.w(TAG, "NotificationManager unavailable")
            return
        }

        val channelId = runCatching { getString(R.string.default_notification_channel_id) }
            .getOrDefault(DEFAULT_CHANNEL_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Murshidabad Connect Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "District news, jobs, education, and emergency alerts"
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        } catch (se: SecurityException) {
            Log.w(TAG, "POST_NOTIFICATIONS permission denied while displaying notification", se)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post local notification: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "MurshidabadFCM"
        private const val DEFAULT_CHANNEL_ID = "murshidabad_alerts"
    }
}
