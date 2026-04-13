package com.example.lunara;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.util.Calendar;
import java.util.Locale;

public class ReminderActivity extends AppCompatActivity {

    // Reminder IDs (matching ReminderUtils)
    private static final int ID_HEALTH  = 4;
    private static final int ID_DOCTOR  = 2;
    private static final int ID_MED     = 3;

    SharedPreferences prefs;
    DrawerLayout drawerLayout;

    Switch healthSwitch, doctorSwitch, medSwitch;
    TextView healthTime, doctorTime, medTime;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);

        prefs = getSharedPreferences("app_settings", MODE_PRIVATE);

        // Drawer
        drawerLayout = findViewById(R.id.drawerLayout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("Reminders");

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView nav = findViewById(R.id.navigationView);
        nav.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawers();
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) { startActivity(new Intent(this, WomanDashboardActivity.class)); finish(); }
            else if (id == R.id.nav_health)    startActivity(new Intent(this, HealthTrackingActivity.class));
            else if (id == R.id.nav_baby)      startActivity(new Intent(this, BabyDevelopmentActivity.class));
            else if (id == R.id.nav_risk)      startActivity(new Intent(this, RiskAlertActivity.class));
            else if (id == R.id.nav_scheduler) startActivity(new Intent(this, SmartSchedulerActivity.class));
            else if (id == R.id.nav_reminders) { /* already here */ }
            else if (id == R.id.nav_doctor)    startActivity(new Intent(this, DoctorContactActivity.class));
            else if (id == R.id.nav_chat)      startActivity(new Intent(this, ChatAssistantActivity.class));
            else if (id == R.id.nav_settings)  startActivity(new Intent(this, SettingsActivity.class));
            else if (id == R.id.nav_logout) {
                getSharedPreferences("UserData", MODE_PRIVATE).edit()
                        .remove("currentUserId").remove("isLoggedIn").apply();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            return true;
        });

        // Bind views
        healthSwitch = findViewById(R.id.healthSwitch);
        doctorSwitch = findViewById(R.id.doctorSwitch);
        medSwitch    = findViewById(R.id.medSwitch);
        healthTime   = findViewById(R.id.healthTimeText);
        doctorTime   = findViewById(R.id.doctorTimeText);
        medTime      = findViewById(R.id.medTimeText);

        // Load saved state
        healthSwitch.setChecked(prefs.getBoolean("reminder_health_enabled", false));
        doctorSwitch.setChecked(prefs.getBoolean("reminder_doctor_enabled", false));
        medSwitch.setChecked(prefs.getBoolean("reminder_med_enabled", false));

        updateTimeDisplay(healthTime, prefs.getInt("reminder_time_health_h", 9), prefs.getInt("reminder_time_health_m", 0));
        updateTimeDisplay(doctorTime, prefs.getInt("reminder_time_doctor_h", 10), prefs.getInt("reminder_time_doctor_m", 0));
        updateTimeDisplay(medTime, prefs.getInt("reminder_time_med_h", 8), prefs.getInt("reminder_time_med_m", 0));

        // Toggle listeners
        healthSwitch.setOnCheckedChangeListener((btn, on) -> {
            prefs.edit().putBoolean("reminder_health_enabled", on).apply();
            if (on) scheduleReminder(ID_HEALTH, "Weekly Health Check",
                    "Time for your weekly health monitoring.",
                    prefs.getInt("reminder_time_health_h", 9),
                    prefs.getInt("reminder_time_health_m", 0),
                    7 * 24 * 60 * 60 * 1000L);
            else cancelReminder(ID_HEALTH);
            Toast.makeText(this, on ? "Health reminder enabled" : "Health reminder disabled", Toast.LENGTH_SHORT).show();
        });

        doctorSwitch.setOnCheckedChangeListener((btn, on) -> {
            prefs.edit().putBoolean("reminder_doctor_enabled", on).apply();
            if (on) scheduleReminder(ID_DOCTOR, "Doctor Visit Reminder",
                    "Upcoming doctor visit reminder.",
                    prefs.getInt("reminder_time_doctor_h", 10),
                    prefs.getInt("reminder_time_doctor_m", 0),
                    AlarmManager.INTERVAL_DAY);
            else cancelReminder(ID_DOCTOR);
            Toast.makeText(this, on ? "Doctor reminder enabled" : "Doctor reminder disabled", Toast.LENGTH_SHORT).show();
        });

        medSwitch.setOnCheckedChangeListener((btn, on) -> {
            prefs.edit().putBoolean("reminder_med_enabled", on).apply();
            if (on) scheduleReminder(ID_MED, "Medication Reminder",
                    "Time to take your prescribed medication.",
                    prefs.getInt("reminder_time_med_h", 8),
                    prefs.getInt("reminder_time_med_m", 0),
                    AlarmManager.INTERVAL_DAY);
            else cancelReminder(ID_MED);
            Toast.makeText(this, on ? "Medication reminder enabled" : "Medication reminder disabled", Toast.LENGTH_SHORT).show();
        });

        // Time picker click listeners
        healthTime.setOnClickListener(v -> showTimePicker("health", ID_HEALTH,
                "Weekly Health Check", "Time for your weekly health monitoring.",
                7 * 24 * 60 * 60 * 1000L, healthTime, healthSwitch));

        doctorTime.setOnClickListener(v -> showTimePicker("doctor", ID_DOCTOR,
                "Doctor Visit Reminder", "Upcoming doctor visit reminder.",
                AlarmManager.INTERVAL_DAY, doctorTime, doctorSwitch));

        medTime.setOnClickListener(v -> showTimePicker("med", ID_MED,
                "Medication Reminder", "Time to take your prescribed medication.",
                AlarmManager.INTERVAL_DAY, medTime, medSwitch));
    }

    private void showTimePicker(String key, int reminderId, String title, String message,
                                long interval, TextView display, Switch toggle) {
        int curH = prefs.getInt("reminder_time_" + key + "_h", 9);
        int curM = prefs.getInt("reminder_time_" + key + "_m", 0);

        new TimePickerDialog(this, (view, hour, minute) -> {
            prefs.edit()
                    .putInt("reminder_time_" + key + "_h", hour)
                    .putInt("reminder_time_" + key + "_m", minute)
                    .apply();
            updateTimeDisplay(display, hour, minute);

            // Reschedule if enabled
            if (toggle.isChecked()) {
                scheduleReminder(reminderId, title, message, hour, minute, interval);
            }
        }, curH, curM, false).show();
    }

    private void updateTimeDisplay(TextView tv, int hour, int minute) {
        String amPm = hour >= 12 ? "PM" : "AM";
        int h12 = hour % 12;
        if (h12 == 0) h12 = 12;
        tv.setText(String.format(Locale.getDefault(), "%d:%02d %s", h12, minute, amPm));
    }

    private void scheduleReminder(int id, String title, String message, int hour, int minute, long interval) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        intent.putExtra("id", id);

        PendingIntent pi = PendingIntent.getBroadcast(this, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);

        // If time already passed today, start tomorrow
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), interval, pi);
    }

    private void cancelReminder(int id) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null) am.cancel(pi);
    }
}
