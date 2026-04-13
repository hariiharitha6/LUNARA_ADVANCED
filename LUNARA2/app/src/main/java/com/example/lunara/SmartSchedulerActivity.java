package com.example.lunara;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SmartSchedulerActivity extends BaseDrawerActivity {

    TextView weekText, trimesterText, frequencyText, nextVisitText, urgencyBadge, overdueWarning;
    TextView riskAdviceText, laborReminderText;
    LinearLayout overdueCard;
    Button markVisitBtn;

    // Appointment views
    Button addAppointmentBtn;
    TextView noAppointmentsText;
    LinearLayout appointmentListContainer;

    SharedPreferences prefs;
    String userId;
    int pregnancyWeek = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_scheduler);

        setupDrawer();

        prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        userId = prefs.getString("current_user_id", "");

        weekText        = findViewById(R.id.weekText);
        trimesterText   = findViewById(R.id.trimesterText);
        frequencyText   = findViewById(R.id.frequencyText);
        nextVisitText   = findViewById(R.id.nextVisitText);
        urgencyBadge    = findViewById(R.id.urgencyBadge);
        overdueWarning  = findViewById(R.id.overdueWarning);
        overdueCard     = findViewById(R.id.overdueCard);
        markVisitBtn    = findViewById(R.id.markVisitBtn);
        riskAdviceText  = findViewById(R.id.riskAdviceText);
        laborReminderText = findViewById(R.id.laborReminderText);

        // Appointment views
        addAppointmentBtn       = findViewById(R.id.addAppointmentBtn);
        noAppointmentsText      = findViewById(R.id.noAppointmentsText);
        appointmentListContainer = findViewById(R.id.appointmentListContainer);

        if (userId.isEmpty()) {
            weekText.setText(getString(R.string.no_data_found));
            return;
        }

        loadLmpFromFirebase();
        loadAppointments();

        markVisitBtn.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            prefs.edit().putLong("last_visit_timestamp", now).apply();
            Toast.makeText(this, getString(R.string.visit_marked), Toast.LENGTH_SHORT).show();
            recalculateUI();
        });

        addAppointmentBtn.setOnClickListener(v -> showAddAppointmentDialog());
    }

    // ────────────────── Appointment CRUD ──────────────────

    private void showAddAppointmentDialog() {
        View dialogView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null);

        // Build custom dialog layout programmatically
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        EditText titleInput = new EditText(this);
        titleInput.setHint(getString(R.string.appointment_title));
        layout.addView(titleInput);

        EditText descInput = new EditText(this);
        descInput.setHint(getString(R.string.appointment_desc));
        layout.addView(descInput);

        // Date/Time state
        final Calendar selectedCal = Calendar.getInstance();
        final boolean[] dateSet = {false};
        final boolean[] timeSet = {false};

        TextView dateDisplay = new TextView(this);
        dateDisplay.setPadding(0, 20, 0, 10);
        dateDisplay.setText(getString(R.string.select_date));
        dateDisplay.setTextColor(ContextCompat.getColor(this, R.color.secondary));
        dateDisplay.setTextSize(15);
        layout.addView(dateDisplay);

        dateDisplay.setOnClickListener(dv -> {
            Calendar now = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                selectedCal.set(Calendar.YEAR, year);
                selectedCal.set(Calendar.MONTH, month);
                selectedCal.set(Calendar.DAY_OF_MONTH, day);
                dateSet[0] = true;
                SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
                dateDisplay.setText("📅 " + sdf.format(selectedCal.getTime()));
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
        });

        TextView timeDisplay = new TextView(this);
        timeDisplay.setPadding(0, 10, 0, 20);
        timeDisplay.setText(getString(R.string.select_time));
        timeDisplay.setTextColor(ContextCompat.getColor(this, R.color.secondary));
        timeDisplay.setTextSize(15);
        layout.addView(timeDisplay);

        timeDisplay.setOnClickListener(tv -> {
            Calendar now = Calendar.getInstance();
            new TimePickerDialog(this, (view, hour, minute) -> {
                selectedCal.set(Calendar.HOUR_OF_DAY, hour);
                selectedCal.set(Calendar.MINUTE, minute);
                timeSet[0] = true;
                String amPm = hour >= 12 ? "PM" : "AM";
                int h12 = hour % 12;
                if (h12 == 0) h12 = 12;
                timeDisplay.setText(String.format(Locale.getDefault(),
                        "⏰ %d:%02d %s", h12, minute, amPm));
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show();
        });

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.add_appointment))
                .setView(layout)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    String title = titleInput.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, getString(R.string.appointment_title), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!dateSet[0] || !timeSet[0]) {
                        Toast.makeText(this, getString(R.string.please_select_date_time), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String desc = descInput.getText().toString().trim();
                    saveAppointment(title, desc, selectedCal.getTimeInMillis());
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void saveAppointment(String title, String desc, long timeMs) {
        try {
            JSONArray arr = loadAppointmentArray();
            JSONObject obj = new JSONObject();
            obj.put("title", title);
            obj.put("desc", desc);
            obj.put("time", timeMs);
            arr.put(obj);

            prefs.edit().putString("appointments_json", arr.toString()).apply();
            Toast.makeText(this, getString(R.string.appointment_saved), Toast.LENGTH_SHORT).show();
            loadAppointments();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void deleteAppointment(int index) {
        try {
            JSONArray arr = loadAppointmentArray();
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                if (i != index) newArr.put(arr.get(i));
            }
            prefs.edit().putString("appointments_json", newArr.toString()).apply();
            Toast.makeText(this, getString(R.string.appointment_deleted), Toast.LENGTH_SHORT).show();
            loadAppointments();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONArray loadAppointmentArray() {
        String json = prefs.getString("appointments_json", "[]");
        try {
            return new JSONArray(json);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private void loadAppointments() {
        appointmentListContainer.removeAllViews();
        JSONArray arr = loadAppointmentArray();

        if (arr.length() == 0) {
            noAppointmentsText.setVisibility(View.VISIBLE);
            return;
        }

        noAppointmentsText.setVisibility(View.GONE);

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy  •  h:mm a", Locale.getDefault());

        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject obj = arr.getJSONObject(i);
                String title = obj.getString("title");
                String desc = obj.optString("desc", "");
                long time = obj.getLong("time");

                // Build card dynamically
                CardView card = new CardView(this);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.bottomMargin = 12;
                card.setLayoutParams(cardParams);
                card.setRadius(40);
                card.setCardElevation(8);
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));

                LinearLayout inner = new LinearLayout(this);
                inner.setOrientation(LinearLayout.VERTICAL);
                inner.setPadding(48, 36, 48, 36);

                TextView titleTv = new TextView(this);
                titleTv.setText(title);
                titleTv.setTextSize(16);
                titleTv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
                inner.addView(titleTv);

                TextView timeTv = new TextView(this);
                timeTv.setText("📅 " + sdf.format(new java.util.Date(time)));
                timeTv.setTextSize(13);
                timeTv.setTextColor(ContextCompat.getColor(this, R.color.secondary));
                timeTv.setPadding(0, 8, 0, 0);
                inner.addView(timeTv);

                if (!desc.isEmpty()) {
                    TextView descTv = new TextView(this);
                    descTv.setText(desc);
                    descTv.setTextSize(13);
                    descTv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    descTv.setPadding(0, 8, 0, 0);
                    inner.addView(descTv);
                }

                // Delete button
                final int idx = i;
                Button deleteBtn = new Button(this);
                deleteBtn.setText(getString(R.string.delete));
                deleteBtn.setTextSize(12);
                deleteBtn.setAllCaps(false);
                deleteBtn.setBackgroundResource(R.drawable.btn_danger_state);
                deleteBtn.setTextColor(ContextCompat.getColor(this, R.color.text_on_primary));
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, 96);
                btnParams.topMargin = 24;
                deleteBtn.setLayoutParams(btnParams);
                deleteBtn.setOnClickListener(v -> deleteAppointment(idx));
                inner.addView(deleteBtn);

                card.addView(inner);
                appointmentListContainer.addView(card);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // ────────────────── Existing Pregnancy Logic (unchanged) ──────────────────

    private void loadLmpFromFirebase() {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) {
                    weekText.setText(getString(R.string.user_not_found));
                    return;
                }

                Integer lmpYear  = snap.child("lmpYear").getValue(Integer.class);
                Integer lmpMonth = snap.child("lmpMonth").getValue(Integer.class);
                Integer lmpDay   = snap.child("lmpDay").getValue(Integer.class);

                if (lmpYear == null || lmpMonth == null || lmpDay == null) {
                    weekText.setText(getString(R.string.lmp_not_available));
                    return;
                }

                Calendar lmpCal = Calendar.getInstance();
                lmpCal.set(lmpYear, lmpMonth, lmpDay);

                long diffMs = System.currentTimeMillis() - lmpCal.getTimeInMillis();
                long diffDays = TimeUnit.MILLISECONDS.toDays(diffMs);
                pregnancyWeek = (int) (diffDays / 7);

                if (pregnancyWeek < 0) pregnancyWeek = 0;
                if (pregnancyWeek > 42) pregnancyWeek = 42;

                Integer riskScore = snap.child("riskScore").getValue(Integer.class);
                int risk = (riskScore != null) ? riskScore : 0;

                recalculateUI();
                showRiskAdvice(risk);
                showLaborReminder();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                weekText.setText(getString(R.string.error_loading_data));
            }
        });
    }

    private void recalculateUI() {
        if (pregnancyWeek < 0) return;

        weekText.setText(getString(R.string.week_prefix) + " " + pregnancyWeek);

        String trimester;
        if (pregnancyWeek <= 12) trimester = getString(R.string.trimester_1_short);
        else if (pregnancyWeek <= 27) trimester = getString(R.string.trimester_2_short);
        else trimester = getString(R.string.trimester_3_short);
        trimesterText.setText(trimester);

        String frequency;
        int intervalDays;

        if (pregnancyWeek >= 37) {
            frequency = getString(R.string.freq_high_priority);
            intervalDays = 7;
        } else if (pregnancyWeek >= 28) {
            frequency = getString(R.string.freq_weekly);
            intervalDays = 7;
        } else if (pregnancyWeek >= 13) {
            frequency = getString(R.string.freq_biweekly);
            intervalDays = 14;
        } else {
            frequency = getString(R.string.freq_monthly);
            intervalDays = 30;
        }
        frequencyText.setText(frequency);

        long lastVisit = prefs.getLong("last_visit_timestamp", 0);
        Calendar nextCal = Calendar.getInstance();

        if (lastVisit > 0) {
            nextCal.setTimeInMillis(lastVisit);
            nextCal.add(Calendar.DAY_OF_YEAR, intervalDays);
        } else {
            nextCal.setTimeInMillis(System.currentTimeMillis());
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
        nextVisitText.setText(sdf.format(nextCal.getTime()));

        long now = System.currentTimeMillis();
        if (now > nextCal.getTimeInMillis()) {
            long overdueDays = TimeUnit.MILLISECONDS.toDays(now - nextCal.getTimeInMillis());
            overdueCard.setVisibility(View.VISIBLE);
            overdueWarning.setText(getString(R.string.checkup_overdue_msg, overdueDays));
        } else {
            overdueCard.setVisibility(View.GONE);
        }

        if (pregnancyWeek >= 37) {
            urgencyBadge.setText(getString(R.string.high_caps));
            urgencyBadge.setTextColor(ContextCompat.getColor(this, R.color.danger));
        } else if (pregnancyWeek >= 28) {
            urgencyBadge.setText(getString(R.string.moderate_caps));
            urgencyBadge.setTextColor(ContextCompat.getColor(this, R.color.warning));
        } else {
            urgencyBadge.setText(getString(R.string.low_caps));
            urgencyBadge.setTextColor(ContextCompat.getColor(this, R.color.success));
        }
    }

    private void showRiskAdvice(int riskScore) {
        if (riskScore > 60) {
            riskAdviceText.setVisibility(View.VISIBLE);
            riskAdviceText.setText(getString(R.string.risk_advice_high, riskScore));
            riskAdviceText.setTextColor(ContextCompat.getColor(this, R.color.danger));
        } else if (riskScore > 30) {
            riskAdviceText.setVisibility(View.VISIBLE);
            riskAdviceText.setText(getString(R.string.risk_advice_moderate, riskScore));
            riskAdviceText.setTextColor(ContextCompat.getColor(this, R.color.warning));
        } else {
            riskAdviceText.setVisibility(View.GONE);
        }
    }

    private void showLaborReminder() {
        if (pregnancyWeek >= 36) {
            laborReminderText.setVisibility(View.VISIBLE);
            laborReminderText.setText(getString(R.string.labor_reminder_msg));
        } else {
            laborReminderText.setVisibility(View.GONE);
        }
    }
}
