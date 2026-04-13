package com.example.lunara;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Re-schedules active reminders after device reboot.
 * Handles both quick (recurring) reminders and custom (one-shot) reminders.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);

        // ── Quick Reminders (recurring) ──

        // Health check (weekly)
        if (prefs.getBoolean("reminder_health_enabled", false)) {
            int h = prefs.getInt("reminder_time_health_h", 9);
            int m = prefs.getInt("reminder_time_health_m", 0);
            scheduleQuickAlarm(context, 4, "Weekly Health Check",
                    "Time for your weekly health monitoring.", h, m);
        }

        // Doctor visit (daily)
        if (prefs.getBoolean("reminder_doctor_enabled", false)) {
            int h = prefs.getInt("reminder_time_doctor_h", 10);
            int m = prefs.getInt("reminder_time_doctor_m", 0);
            scheduleQuickAlarm(context, 2, "Doctor Visit Reminder",
                    "Upcoming doctor visit reminder.", h, m);
        }

        // Medication (daily)
        if (prefs.getBoolean("reminder_med_enabled", false)) {
            int h = prefs.getInt("reminder_time_med_h", 8);
            int m = prefs.getInt("reminder_time_med_m", 0);
            scheduleQuickAlarm(context, 3, "Medication Reminder",
                    "Time to take your prescribed medication.", h, m);
        }

        // ── Custom Reminders (one-shot, future only) ──
        rescheduleCustomReminders(context, prefs);
    }

    private void scheduleQuickAlarm(Context ctx, int id, String title, String msg, int hour, int min) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        scheduleExact(ctx, id, title, msg, cal.getTimeInMillis(), false);
    }

    private void rescheduleCustomReminders(Context ctx, SharedPreferences prefs) {
        String json = prefs.getString("custom_reminders_json", "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                boolean enabled = obj.getBoolean("enabled");
                long triggerTime = obj.getLong("triggerTime");

                // Only schedule if enabled AND in the future
                if (enabled && triggerTime > System.currentTimeMillis()) {
                    int id = obj.getInt("id");
                    String title = obj.getString("title");
                    boolean isSilent = obj.getBoolean("isSilent");

                    scheduleExact(ctx, id, title, "Reminder: " + title, triggerTime, isSilent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scheduleExact(Context ctx, int id, String title, String msg, long triggerMs, boolean isSilent) {
        Intent intent = new Intent(ctx, ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", msg);
        intent.putExtra("id", id);
        intent.putExtra("isSilent", isSilent);

        PendingIntent pi = PendingIntent.getBroadcast(ctx, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pi);
        }
    }
}
