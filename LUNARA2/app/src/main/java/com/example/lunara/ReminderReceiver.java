package com.example.lunara;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ALARM  = "reminder_alarm_channel";
    private static final String CHANNEL_SILENT = "reminder_silent_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);

        // Check global notification setting
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);

        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        int notificationId = intent.getIntExtra("id", 0);
        boolean isSilent = intent.getBooleanExtra("isSilent", false);

        if (title == null) title = "Lunara Reminder";
        if (message == null) message = "You have a pending reminder.";

        // Show notification (respecting global toggle)
        if (notificationsEnabled) {
            if (isSilent) {
                showSilentNotification(context, title, message, notificationId);
            } else {
                showAlarmNotification(context, title, message, notificationId);
            }
        }

        // Reschedule only for quick (recurring) reminders (IDs 2, 3, 4)
        // Custom reminders (ID >= 100) are one-shot
        if (notificationId < 100) {
            rescheduleNext(context, intent, notificationId, prefs);
        }
    }

    private void rescheduleNext(Context context, Intent originalIntent, int id, SharedPreferences prefs) {
        long interval = prefs.getLong("reminder_interval_" + id, 0);
        if (interval <= 0) return;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = PendingIntent.getBroadcast(context, id, originalIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long nextTime = System.currentTimeMillis() + interval;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, nextTime, pi);
        }
    }

    // ── Alarm + Sound notification ──
    private void showAlarmNotification(Context context, String title, String message, int id) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ALARM, "Reminders (Alarm + Sound)", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("LUNARA reminders with alarm sound and vibration");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 400, 200, 400});

            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null)
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            channel.setSound(alarmSound, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());

            manager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, WomanDashboardActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, id, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null)
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ALARM)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(alarmSound)
                .setVibrate(new long[]{0, 400, 200, 400})
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS);

        manager.notify(id, builder.build());
    }

    // ── Silent notification (no sound, no vibration) ──
    private void showSilentNotification(Context context, String title, String message, int id) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_SILENT, "Reminders (Silent)", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("LUNARA reminders — notification only, no sound");
            channel.enableVibration(false);
            channel.setSound(null, null);

            manager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, WomanDashboardActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, id, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_SILENT)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(null)
                .setVibrate(null)
                .setSilent(true);

        manager.notify(id, builder.build());
    }
}
