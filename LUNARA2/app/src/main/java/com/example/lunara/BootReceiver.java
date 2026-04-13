package com.example.lunara;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;

/**
 * Re-schedules active reminders after device reboot.
 * Only reschedules reminders that the user has explicitly enabled.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);

        // Health check (weekly)
        if (prefs.getBoolean("reminder_health_enabled", false)) {
            int h = prefs.getInt("reminder_time_health_h", 9);
            int m = prefs.getInt("reminder_time_health_m", 0);
            scheduleAlarm(context, 4, "Weekly Health Check",
                    "Time for your weekly health monitoring.", h, m,
                    7 * 24 * 60 * 60 * 1000L);
        }

        // Doctor visit (daily)
        if (prefs.getBoolean("reminder_doctor_enabled", false)) {
            int h = prefs.getInt("reminder_time_doctor_h", 10);
            int m = prefs.getInt("reminder_time_doctor_m", 0);
            scheduleAlarm(context, 2, "Doctor Visit Reminder",
                    "Upcoming doctor visit reminder.", h, m,
                    AlarmManager.INTERVAL_DAY);
        }

        // Medication (daily)
        if (prefs.getBoolean("reminder_med_enabled", false)) {
            int h = prefs.getInt("reminder_time_med_h", 8);
            int m = prefs.getInt("reminder_time_med_m", 0);
            scheduleAlarm(context, 3, "Medication Reminder",
                    "Time to take your prescribed medication.", h, m,
                    AlarmManager.INTERVAL_DAY);
        }
    }

    private void scheduleAlarm(Context ctx, int id, String title, String msg, int hour, int min, long interval) {
        Intent intent = new Intent(ctx, ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", msg);
        intent.putExtra("id", id);

        PendingIntent pi = PendingIntent.getBroadcast(ctx, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, 0);

        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), interval, pi);
    }
}
