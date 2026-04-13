package com.example.lunara;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;

public class WomanDashboardActivity extends BaseDrawerActivity {

    TextView nameText, areaText, mobileText, weightText;
    TextView weekText, trimesterText, dueDateText, tipsText;
    Button healthBtn, babyBtn, emergencyBtn, riskBtn, alertBtn;

    LinearLayout nextVisitCard;
    TextView nextVisitDateText, nextVisitCountdown, nextVisitStatus;

    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_woman_dashboard);

        // ── Status Bar Fix ──
        View mainView = findViewById(R.id.main_content);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                v.setPadding(0, top, 0, 0);
                return insets;
            });
        }

        // ── BaseDrawer Setup ──
        setupDrawer();

        // ── View Bindings ──
        nameText      = findViewById(R.id.nameText);
        areaText      = findViewById(R.id.areaText);
        mobileText    = findViewById(R.id.mobileText);
        weightText    = findViewById(R.id.weightText);
        weekText      = findViewById(R.id.weekText);
        trimesterText = findViewById(R.id.trimesterText);
        dueDateText   = findViewById(R.id.dueDateText);
        tipsText      = findViewById(R.id.tipsText);

        healthBtn    = findViewById(R.id.healthBtn);
        babyBtn      = findViewById(R.id.babyBtn);
        emergencyBtn = findViewById(R.id.emergencyBtn);
        riskBtn      = findViewById(R.id.riskBtn);
        alertBtn     = findViewById(R.id.alertBtn);

        nextVisitCard      = findViewById(R.id.nextVisitCard);
        nextVisitDateText  = findViewById(R.id.nextVisitDate);
        nextVisitCountdown = findViewById(R.id.nextVisitCountdown);
        nextVisitStatus    = findViewById(R.id.nextVisitStatus);

        currentUserId = getSharedPreferences("UserData", MODE_PRIVATE)
                .getString("current_user_id", null);

        // Fixed: Remove infinite redirect logic, just load if ID exists
        if (currentUserId != null) {
            loadUserDataFromFirebase();
        }

        // ── Dashboard Buttons ──
        if (healthBtn != null) healthBtn.setOnClickListener(v -> startActivity(new Intent(WomanDashboardActivity.this, HealthTrackingActivity.class)));
        if (babyBtn != null) babyBtn.setOnClickListener(v -> startActivity(new Intent(WomanDashboardActivity.this, BabyDevelopmentActivity.class)));
        if (emergencyBtn != null) emergencyBtn.setOnClickListener(v -> startActivity(new Intent(WomanDashboardActivity.this, EmergencyActivity.class)));
        if (riskBtn != null) riskBtn.setOnClickListener(v -> startActivity(new Intent(WomanDashboardActivity.this, RiskAlertActivity.class)));
        if (alertBtn != null) alertBtn.setOnClickListener(v -> sendEmergencyAlert());
    }

    private void loadUserDataFromFirebase() {
        if (currentUserId == null) return;
        
        FirebaseDatabase.getInstance().getReference("users")
                .child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        UserModel user = snapshot.getValue(UserModel.class);
                        if (user != null) populateUI(user);
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e("LUNARA_DASH", "Error: " + error.getMessage());
                    }
                });
    }

    private void populateUI(UserModel user) {
        if (nameText != null) nameText.setText(getString(R.string.name_prefix) + " " + user.name);
        if (areaText != null) areaText.setText(getString(R.string.area_prefix) + " " + user.area);
        if (mobileText != null) mobileText.setText(getString(R.string.mobile_prefix) + " " + user.mobile);
        if (weightText != null) weightText.setText(getString(R.string.weight_prefix) + " " + user.weight + " kg");

        Calendar lmp = Calendar.getInstance();
        lmp.set(user.lmpYear, user.lmpMonth, user.lmpDay);

        Calendar today = Calendar.getInstance();
        long diff  = today.getTimeInMillis() - lmp.getTimeInMillis();
        long weeks = (diff / (1000 * 60 * 60 * 24)) / 7;

        if (weekText != null) weekText.setText(getString(R.string.week_prefix) + " " + weeks);

        if (trimesterText != null && tipsText != null) {
            if (weeks <= 12) {
                trimesterText.setText(getString(R.string.trimester_1));
                tipsText.setText(getString(R.string.tips_1));
            } else if (weeks <= 27) {
                trimesterText.setText(getString(R.string.trimester_2));
                tipsText.setText(getString(R.string.tips_2));
            } else {
                trimesterText.setText(getString(R.string.trimester_3));
                tipsText.setText(getString(R.string.tips_3));
            }
        }

        Calendar edd = (Calendar) lmp.clone();
        edd.add(Calendar.DAY_OF_YEAR, 280);
        if (dueDateText != null) {
            dueDateText.setText(getString(R.string.due_date_prefix) + " "
                    + edd.get(Calendar.DAY_OF_MONTH) + "/"
                    + (edd.get(Calendar.MONTH) + 1) + "/"
                    + edd.get(Calendar.YEAR));
        }

        scheduleAndDisplayCheckup(user.riskScore, weeks);
    }

    private void scheduleAndDisplayCheckup(int riskScore, long weeks) {
        if (currentUserId == null) return;
        
        DatabaseReference checkupRef = FirebaseDatabase.getInstance()
                .getReference("checkups").child(currentUserId);

        checkupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                CheckupModel existing = snapshot.getValue(CheckupModel.class);
                long nowMs = System.currentTimeMillis();
                CheckupModel checkup;

                if (existing != null && existing.nextVisitDate > nowMs) {
                    checkup = existing;
                } else {
                    CheckupScheduler.ScheduleResult result = CheckupScheduler.schedule(riskScore, weeks);
                    checkup = new CheckupModel(result.nextVisitDateMs, result.riskLevel, CheckupScheduler.STATUS_PENDING, nowMs);
                    checkupRef.setValue(checkup);
                }
                displayCheckupCard(checkup);
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void displayCheckupCard(CheckupModel checkup) {
        if (nextVisitCard == null) return;
        
        nextVisitCard.setVisibility(View.VISIBLE);
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        nextVisitCard.startAnimation(fadeIn);

        long daysLeft = CheckupScheduler.daysUntilVisit(checkup.nextVisitDate);
        if (nextVisitDateText != null) nextVisitDateText.setText("📅 " + CheckupScheduler.formatDate(checkup.nextVisitDate));

        if (daysLeft < 0) {
            if (nextVisitCountdown != null) nextVisitCountdown.setText(getString(R.string.visit_overdue, Math.abs(daysLeft)));
            if (nextVisitStatus != null) {
                nextVisitStatus.setText(getString(R.string.overdue_caps));
                nextVisitStatus.setTextColor(ContextCompat.getColor(this, R.color.danger));
            }
        } else {
            if (nextVisitCountdown != null) nextVisitCountdown.setText(getString(R.string.days_remaining, daysLeft));
            boolean isHighRisk = CheckupScheduler.LEVEL_HIGH.equals(checkup.riskLevel);
            if (nextVisitStatus != null) {
                nextVisitStatus.setText(isHighRisk ? getString(R.string.high_risk_badge) : getString(R.string.low_risk_badge));
                nextVisitStatus.setTextColor(isHighRisk ? ContextCompat.getColor(this, R.color.danger) : ContextCompat.getColor(this, R.color.success));
            }
        }
    }

    private void sendEmergencyAlert() {
        if (currentUserId == null) return;

        FirebaseDatabase.getInstance().getReference("users")
                .child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        UserModel user = snapshot.getValue(UserModel.class);
                        if (user != null) {
                            AlertModel alert = new AlertModel(
                                    user.name,
                                    user.mobile,
                                    user.area,
                                    System.currentTimeMillis()
                            );

                            FirebaseDatabase.getInstance().getReference("alerts")
                                    .child(currentUserId)
                                    .setValue(alert)
                                    .addOnSuccessListener(unused -> 
                                        Toast.makeText(WomanDashboardActivity.this, 
                                            "Emergency Alert Sent!", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> 
                                        Toast.makeText(WomanDashboardActivity.this, 
                                            "Failed to send alert: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }
}
