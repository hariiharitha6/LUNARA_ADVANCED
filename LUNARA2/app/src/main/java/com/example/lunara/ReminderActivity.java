package com.example.lunara;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderActivity extends BaseDrawerActivity {

    // ── Quick Reminder IDs (unchanged) ──
    private static final int ID_HEALTH = 4;
    private static final int ID_DOCTOR = 2;
    private static final int ID_MED    = 3;

    // Custom reminders start from ID 100 to avoid collision
    private static final int CUSTOM_ID_START = 100;

    SharedPreferences prefs;

    // Quick reminder views
    Switch healthSwitch, doctorSwitch, medSwitch;
    TextView healthTime, doctorTime, medTime;

    // Custom reminder views
    Button addReminderBtn;
    TextView noCustomRemindersText;
    RecyclerView customReminderRecyclerView;
    ReminderAdapter adapter;
    List<ReminderModel> customReminders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);

        setupDrawer();

        prefs = getSharedPreferences("app_settings", MODE_PRIVATE);

        // ═══════════════════════════════════
        // SECTION 1: Quick Reminders (kept)
        // ═══════════════════════════════════
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
            if (on) scheduleQuickReminder(ID_HEALTH, "Weekly Health Check",
                    "Time for your weekly health monitoring.",
                    prefs.getInt("reminder_time_health_h", 9),
                    prefs.getInt("reminder_time_health_m", 0));
            else cancelAlarm(ID_HEALTH);
            Toast.makeText(this, on ? "Health reminder enabled" : "Health reminder disabled", Toast.LENGTH_SHORT).show();
        });

        doctorSwitch.setOnCheckedChangeListener((btn, on) -> {
            prefs.edit().putBoolean("reminder_doctor_enabled", on).apply();
            if (on) scheduleQuickReminder(ID_DOCTOR, "Doctor Visit Reminder",
                    "Upcoming doctor visit reminder.",
                    prefs.getInt("reminder_time_doctor_h", 10),
                    prefs.getInt("reminder_time_doctor_m", 0));
            else cancelAlarm(ID_DOCTOR);
            Toast.makeText(this, on ? "Doctor reminder enabled" : "Doctor reminder disabled", Toast.LENGTH_SHORT).show();
        });

        medSwitch.setOnCheckedChangeListener((btn, on) -> {
            prefs.edit().putBoolean("reminder_med_enabled", on).apply();
            if (on) scheduleQuickReminder(ID_MED, "Medication Reminder",
                    "Time to take your prescribed medication.",
                    prefs.getInt("reminder_time_med_h", 8),
                    prefs.getInt("reminder_time_med_m", 0));
            else cancelAlarm(ID_MED);
            Toast.makeText(this, on ? "Medication reminder enabled" : "Medication reminder disabled", Toast.LENGTH_SHORT).show();
        });

        // Time picker click listeners
        healthTime.setOnClickListener(v -> showQuickTimePicker("health", ID_HEALTH,
                "Weekly Health Check", "Time for your weekly health monitoring.",
                healthTime, healthSwitch));

        doctorTime.setOnClickListener(v -> showQuickTimePicker("doctor", ID_DOCTOR,
                "Doctor Visit Reminder", "Upcoming doctor visit reminder.",
                doctorTime, doctorSwitch));

        medTime.setOnClickListener(v -> showQuickTimePicker("med", ID_MED,
                "Medication Reminder", "Time to take your prescribed medication.",
                medTime, medSwitch));

        // ═══════════════════════════════════
        // SECTION 2: Custom Reminders (new)
        // ═══════════════════════════════════
        addReminderBtn           = findViewById(R.id.addReminderBtn);
        noCustomRemindersText    = findViewById(R.id.noCustomRemindersText);
        customReminderRecyclerView = findViewById(R.id.customReminderRecyclerView);

        customReminders = new ArrayList<>();
        adapter = new ReminderAdapter(customReminders, new ReminderAdapter.OnReminderActionListener() {
            @Override
            public void onDelete(int position) {
                deleteCustomReminder(position);
            }

            @Override
            public void onToggle(int position, boolean enabled) {
                toggleCustomReminder(position, enabled);
            }
        });

        customReminderRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        customReminderRecyclerView.setAdapter(adapter);

        loadCustomReminders();

        addReminderBtn.setOnClickListener(v -> showAddReminderDialog());
    }

    // ═══════════════════════════════════════
    // Quick Reminder Methods (existing logic)
    // ═══════════════════════════════════════

    private void showQuickTimePicker(String key, int reminderId, String title, String message,
                                     TextView display, Switch toggle) {
        int curH = prefs.getInt("reminder_time_" + key + "_h", 9);
        int curM = prefs.getInt("reminder_time_" + key + "_m", 0);

        new TimePickerDialog(this, (view, hour, minute) -> {
            prefs.edit()
                    .putInt("reminder_time_" + key + "_h", hour)
                    .putInt("reminder_time_" + key + "_m", minute)
                    .apply();
            updateTimeDisplay(display, hour, minute);

            if (toggle.isChecked()) {
                scheduleQuickReminder(reminderId, title, message, hour, minute);
            }
        }, curH, curM, false).show();
    }

    private void updateTimeDisplay(TextView tv, int hour, int minute) {
        String amPm = hour >= 12 ? "PM" : "AM";
        int h12 = hour % 12;
        if (h12 == 0) h12 = 12;
        tv.setText(String.format(Locale.getDefault(), "%d:%02d %s", h12, minute, amPm));
    }

    private void scheduleQuickReminder(int id, String title, String message, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        scheduleExactAlarm(id, title, message, cal.getTimeInMillis(), false);

        // Store interval for rescheduling (BootReceiver / ReminderReceiver)
        long interval = (id == ID_HEALTH) ? 7 * 24 * 60 * 60 * 1000L : AlarmManager.INTERVAL_DAY;
        prefs.edit().putLong("reminder_interval_" + id, interval).apply();
    }

    // ═══════════════════════════════════════
    // Custom Reminder — Add Dialog
    // ═══════════════════════════════════════

    private void showAddReminderDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        // Title
        EditText titleInput = new EditText(this);
        titleInput.setHint(getString(R.string.reminder_title_hint));
        titleInput.setTextSize(15);
        layout.addView(titleInput);

        // Date picker button
        final Calendar selectedCal = Calendar.getInstance();
        final boolean[] dateSet = {false};
        final boolean[] timeSet = {false};

        TextView dateBtn = new TextView(this);
        dateBtn.setPadding(0, 30, 0, 10);
        dateBtn.setText("📅 " + getString(R.string.select_date));
        dateBtn.setTextColor(ContextCompat.getColor(this, R.color.secondary));
        dateBtn.setTextSize(16);
        layout.addView(dateBtn);

        dateBtn.setOnClickListener(dv -> {
            Calendar now = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                selectedCal.set(Calendar.YEAR, year);
                selectedCal.set(Calendar.MONTH, month);
                selectedCal.set(Calendar.DAY_OF_MONTH, day);
                dateSet[0] = true;
                SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
                dateBtn.setText("📅 " + sdf.format(selectedCal.getTime()));
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Time picker button
        TextView timeBtn = new TextView(this);
        timeBtn.setPadding(0, 10, 0, 20);
        timeBtn.setText("⏰ " + getString(R.string.select_time));
        timeBtn.setTextColor(ContextCompat.getColor(this, R.color.secondary));
        timeBtn.setTextSize(16);
        layout.addView(timeBtn);

        timeBtn.setOnClickListener(tv -> {
            Calendar now = Calendar.getInstance();
            new TimePickerDialog(this, (view, hour, minute) -> {
                selectedCal.set(Calendar.HOUR_OF_DAY, hour);
                selectedCal.set(Calendar.MINUTE, minute);
                selectedCal.set(Calendar.SECOND, 0);
                selectedCal.set(Calendar.MILLISECOND, 0);
                timeSet[0] = true;
                String amPm = hour >= 12 ? "PM" : "AM";
                int h12 = hour % 12;
                if (h12 == 0) h12 = 12;
                timeBtn.setText(String.format(Locale.getDefault(),
                        "⏰ %d:%02d %s", h12, minute, amPm));
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show();
        });

        // Alert type radio buttons
        TextView alertLabel = new TextView(this);
        alertLabel.setText(getString(R.string.alert_type));
        alertLabel.setTextSize(14);
        alertLabel.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        alertLabel.setPadding(0, 10, 0, 6);
        layout.addView(alertLabel);

        RadioGroup alertGroup = new RadioGroup(this);
        alertGroup.setOrientation(RadioGroup.VERTICAL);

        RadioButton alarmRadio = new RadioButton(this);
        alarmRadio.setId(View.generateViewId());
        alarmRadio.setText(getString(R.string.alarm_sound));
        alarmRadio.setChecked(true);
        alertGroup.addView(alarmRadio);

        RadioButton silentRadio = new RadioButton(this);
        silentRadio.setId(View.generateViewId());
        silentRadio.setText(getString(R.string.silent_notification));
        alertGroup.addView(silentRadio);

        layout.addView(alertGroup);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.add_reminder))
                .setView(layout)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    String title = titleInput.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, getString(R.string.reminder_title_hint), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!dateSet[0] || !timeSet[0]) {
                        Toast.makeText(this, getString(R.string.please_select_date_time), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean isSilent = silentRadio.isChecked();
                    long triggerTime = selectedCal.getTimeInMillis();

                    addCustomReminder(title, triggerTime, isSilent);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    // ═══════════════════════════════════════
    // Custom Reminder — CRUD & AlarmManager
    // ═══════════════════════════════════════

    private int nextCustomId() {
        int maxId = prefs.getInt("custom_reminder_next_id", CUSTOM_ID_START);
        prefs.edit().putInt("custom_reminder_next_id", maxId + 1).apply();
        return maxId;
    }

    private void addCustomReminder(String title, long triggerTime, boolean isSilent) {
        int id = nextCustomId();
        ReminderModel rem = new ReminderModel(id, title, triggerTime, isSilent, true);

        customReminders.add(rem);
        adapter.notifyItemInserted(customReminders.size() - 1);
        saveCustomReminders();
        updateCustomEmptyState();

        // Schedule the alarm
        scheduleExactAlarm(id, title, "Reminder: " + title, triggerTime, isSilent);

        Toast.makeText(this, getString(R.string.reminder_saved), Toast.LENGTH_SHORT).show();
    }

    private void deleteCustomReminder(int position) {
        if (position < 0 || position >= customReminders.size()) return;

        ReminderModel rem = customReminders.get(position);
        cancelAlarm(rem.id);

        customReminders.remove(position);
        adapter.notifyItemRemoved(position);
        adapter.notifyItemRangeChanged(position, customReminders.size());
        saveCustomReminders();
        updateCustomEmptyState();

        Toast.makeText(this, getString(R.string.reminder_deleted), Toast.LENGTH_SHORT).show();
    }

    private void toggleCustomReminder(int position, boolean enabled) {
        if (position < 0 || position >= customReminders.size()) return;

        ReminderModel rem = customReminders.get(position);
        rem.enabled = enabled;
        saveCustomReminders();

        if (enabled) {
            // Only schedule if trigger time is in the future
            if (rem.triggerTime > System.currentTimeMillis()) {
                scheduleExactAlarm(rem.id, rem.title, "Reminder: " + rem.title, rem.triggerTime, rem.isSilent);
            } else {
                Toast.makeText(this, "Reminder time has passed. Please delete and create a new one.", Toast.LENGTH_LONG).show();
                rem.enabled = false;
                saveCustomReminders();
            }
        } else {
            cancelAlarm(rem.id);
        }

        adapter.notifyItemChanged(position);
    }

    // ═══════════════════════════════════════
    // Custom Reminder — Persistence (JSON)
    // ═══════════════════════════════════════

    private void saveCustomReminders() {
        try {
            JSONArray arr = new JSONArray();
            for (ReminderModel r : customReminders) {
                JSONObject obj = new JSONObject();
                obj.put("id", r.id);
                obj.put("title", r.title);
                obj.put("triggerTime", r.triggerTime);
                obj.put("isSilent", r.isSilent);
                obj.put("enabled", r.enabled);
                arr.put(obj);
            }
            prefs.edit().putString("custom_reminders_json", arr.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadCustomReminders() {
        customReminders.clear();
        String json = prefs.getString("custom_reminders_json", "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                customReminders.add(new ReminderModel(
                        obj.getInt("id"),
                        obj.getString("title"),
                        obj.getLong("triggerTime"),
                        obj.getBoolean("isSilent"),
                        obj.getBoolean("enabled")
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        adapter.notifyDataSetChanged();
        updateCustomEmptyState();
    }

    private void updateCustomEmptyState() {
        noCustomRemindersText.setVisibility(
                customReminders.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ═══════════════════════════════════════
    // AlarmManager — Shared by both sections
    // ═══════════════════════════════════════

    private void scheduleExactAlarm(int id, String title, String message, long triggerTimeMs, boolean isSilent) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        intent.putExtra("id", id);
        intent.putExtra("isSilent", isSilent);

        PendingIntent pi = PendingIntent.getBroadcast(this, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs, pi);
        }
    }

    private void cancelAlarm(int id) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null) am.cancel(pi);
    }
}
