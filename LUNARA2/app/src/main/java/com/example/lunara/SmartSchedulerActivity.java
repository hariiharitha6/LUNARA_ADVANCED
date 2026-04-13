package com.example.lunara;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SmartSchedulerActivity extends BaseDrawerActivity {

    TextView weekText, trimesterText, frequencyText, nextVisitText, urgencyBadge, overdueWarning;
    TextView riskAdviceText, laborReminderText;
    LinearLayout overdueCard;
    Button markVisitBtn;

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

        if (userId.isEmpty()) {
            weekText.setText(getString(R.string.no_data_found));
            return;
        }

        loadLmpFromFirebase();

        markVisitBtn.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            prefs.edit().putLong("last_visit_timestamp", now).apply();
            Toast.makeText(this, getString(R.string.visit_marked), Toast.LENGTH_SHORT).show();
            recalculateUI();
        });
    }

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
