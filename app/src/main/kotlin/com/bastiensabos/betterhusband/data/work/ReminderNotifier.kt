package com.bastiensabos.betterhusband.data.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bastiensabos.betterhusband.MainActivity
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.domain.model.DateLabel
import com.bastiensabos.betterhusband.domain.model.UpcomingDate
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Affiche un rappel discret. Sur l'écran verrouillé, seule la version publique, neutre, est visible ; le détail
 * (qui, quand) n'apparaît qu'une fois le téléphone déverrouillé.
 */
@Singleton
class ReminderNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        // La permission est vérifiée par canPostNotifications() avant l'envoi.
        @SuppressLint("MissingPermission")
        fun notify(
            item: UpcomingDate,
            offsetDays: Int,
        ) {
            if (!canPostNotifications()) return
            ensureChannel()
            val publicVersion =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_reminder)
                    .setContentTitle(context.getString(R.string.notif_public_title))
                    .setContentText(context.getString(R.string.notif_public_text))
                    .build()
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_reminder)
                    .setContentTitle(item.label.text())
                    .setContentText(whenText(offsetDays))
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPublicVersion(publicVersion)
                    .setContentIntent(openAppIntent())
                    .setAutoCancel(true)
                    .build()
            NotificationManagerCompat.from(context).notify("${item.key}-j$offsetDays".hashCode(), notification)
        }

        private fun canPostNotifications(): Boolean {
            val granted =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        private fun ensureChannel() {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = context.getString(R.string.notif_channel_description) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        private fun openAppIntent(): PendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun whenText(offsetDays: Int): String =
            if (offsetDays == 0) {
                context.getString(R.string.notif_today)
            } else {
                context.resources.getQuantityString(R.plurals.dates_in_days, offsetDays, offsetDays)
            }

        private fun DateLabel.text(): String =
            when (this) {
                is DateLabel.PartnerBirthday -> context.getString(R.string.dates_label_birthday_of, name)
                DateLabel.RelationshipStart -> context.getString(R.string.dates_label_relationship_start)
                is DateLabel.RelativeBirthday -> context.getString(R.string.dates_label_birthday_of, name)
                is DateLabel.Custom -> title
            }

        private companion object {
            const val CHANNEL_ID = "important_dates"
        }
    }
