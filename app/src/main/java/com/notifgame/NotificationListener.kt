package com.notifgame

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

class NotificationListener :
    NotificationListenerService() {

    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {

        val sourcePackage =
            sbn.packageName

        if (sourcePackage == packageName) {
            return
        }

        val prefs =
            getSharedPreferences(
                "settings",
                Context.MODE_PRIVATE
            )

        val selectedPackages =
            prefs.getStringSet(
                "source_packages",
                emptySet()
            ) ?: emptySet()

        if (
            !selectedPackages.contains(
                sourcePackage
            )
        ) {
            return
        }

        try {
            cancelNotification(sbn.key)
        } catch (_: Exception) {
        }

        showReplacementNotification(this)
    }

    companion object {

        private const val CHANNEL_ID =
            "replacement_channel"

        private const val NOTIFICATION_ID =
            98765

        fun showReplacementNotification(
            context: Context
        ) {

            val prefs =
                context.getSharedPreferences(
                    "settings",
                    Context.MODE_PRIVATE
                )

            val title =
                prefs.getString(
                    "notification_title",
                    "Bildirim"
                ) ?: "Bildirim"

            val text =
                prefs.getString(
                    "notification_text",
                    "Yeni bildirim var."
                ) ?: "Yeni bildirim var."

            val targetPackage =
                prefs.getString(
                    "target_package",
                    null
                )
                    ?: return

            val manager =
                context.getSystemService(
                    NotificationManager::class.java
                )

            createChannel(manager)

            val launchIntent =
                context.packageManager
                    .getLaunchIntentForPackage(
                        targetPackage
                    )
                    ?: return

            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )

            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    1001,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )

            val notification =
                NotificationCompat.Builder(
                    context,
                    CHANNEL_ID
                )
                    .setSmallIcon(
                        android.R.drawable.ic_dialog_info
                    )
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(text)
                    )
                    .setContentIntent(
                        pendingIntent
                    )
                    .setAutoCancel(true)
                    .setPriority(
                        NotificationCompat.PRIORITY_HIGH
                    )
                    .setCategory(
                        Notification.CATEGORY_MESSAGE
                    )
                    .build()

            try {

                manager.notify(
                    NOTIFICATION_ID,
                    notification
                )

            } catch (_: SecurityException) {
            }
        }

        private fun createChannel(
            manager: NotificationManager
        ) {

            if (
                manager.getNotificationChannel(
                    CHANNEL_ID
                ) != null
            ) {
                return
            }

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "kNotification",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {

                    description =
                        "Değiştirilen uygulama bildirimleri"

                    enableVibration(true)
                }

            manager.createNotificationChannel(
                channel
            )
        }
    }
}
