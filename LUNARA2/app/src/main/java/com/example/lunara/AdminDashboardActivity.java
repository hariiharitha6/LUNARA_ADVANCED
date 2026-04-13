package com.example.lunara;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Context;
import android.os.Vibrator;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.util.Log;

public class AdminDashboardActivity extends BaseDrawerActivity {

    TextView areaSummary, trimesterSummary, riskSummary, alertStatusText;
    TextView totalMothersText, highRiskText;
    Button mapBtn, clearAlertBtn;
    RecyclerView motherRecyclerView;
    MotherAdapter motherAdapter;
    List<MotherModel> motherList;

    TextView checkupUpcomingCount, checkupOverdueCount, checkupHighRiskCount;
    RecyclerView checkupRecyclerView;
    CheckupAdapter checkupAdapter;
    List<CheckupEntry> checkupEntryList;

    DatabaseReference alertRef;
    ValueEventListener alertListener;
    Ringtone ringtone;
    long lastAlertTimestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        setupDrawer();

        areaSummary      = findViewById(R.id.areaSummary);
        trimesterSummary = findViewById(R.id.trimesterSummary);
        riskSummary      = findViewById(R.id.riskSummary);
        alertStatusText  = findViewById(R.id.alertStatusText);
        mapBtn           = findViewById(R.id.mapBtn);
        clearAlertBtn    = findViewById(R.id.clearAlertBtn);
        totalMothersText = findViewById(R.id.totalMothersText);
        highRiskText     = findViewById(R.id.highRiskText);

        motherRecyclerView = findViewById(R.id.motherRecyclerView);
        motherList = new ArrayList<>();
        motherAdapter = new MotherAdapter(motherList);
        motherRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        motherRecyclerView.setAdapter(motherAdapter);
        motherRecyclerView.setNestedScrollingEnabled(false);

        checkupUpcomingCount = findViewById(R.id.checkupUpcomingCount);
        checkupOverdueCount  = findViewById(R.id.checkupOverdueCount);
        checkupHighRiskCount = findViewById(R.id.checkupHighRiskCount);

        checkupRecyclerView = findViewById(R.id.checkupRecyclerView);
        checkupEntryList    = new ArrayList<>();
        checkupAdapter      = new CheckupAdapter(checkupEntryList);
        checkupRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        checkupRecyclerView.setAdapter(checkupAdapter);
        checkupRecyclerView.setNestedScrollingEnabled(false);

        loadSummary();
        loadAllMothersFromFirebase();
        loadCheckupData();

        mapBtn.setOnClickListener(v ->
                startActivity(new Intent(this, RegionDashboardActivity.class)));

        alertRef = FirebaseDatabase.getInstance().getReference("alerts");

        alertListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    if (ringtone != null && ringtone.isPlaying()) {
                        ringtone.stop();
                        ringtone = null;
                    }
                    alertStatusText.setText(getString(R.string.no_alerts));
                    alertStatusText.setTextColor(getResources().getColor(android.R.color.black));
                    return;
                }

                DataSnapshot latest = null;
                for (DataSnapshot data : snapshot.getChildren()) { latest = data; }

                if (latest != null) {
                    AlertModel alert = latest.getValue(AlertModel.class);
                    if (alert != null) {
                        alertStatusText.setText(
                                "🚨 EMERGENCY ALERT\n\n"
                                        + getString(R.string.name_prefix) + " "   + alert.name   + "\n"
                                        + getString(R.string.mobile_prefix) + " " + alert.mobile + "\n"
                                        + getString(R.string.area_prefix) + " "   + alert.area);
                        alertStatusText.setTextColor(
                                getResources().getColor(android.R.color.holo_red_dark));

                        if (alert.timestamp > lastAlertTimestamp) {
                            lastAlertTimestamp = alert.timestamp;
                            showNotification("🚨 Emergency Alert",
                                    "Patient: " + alert.name
                                            + " | Mobile: " + alert.mobile
                                            + " | Area: " + alert.area);
                            playAlertSound();
                            vibrateDevice();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        };

        alertRef.addValueEventListener(alertListener);
        clearAlertBtn.setOnClickListener(v -> clearEmergencyAlert());
    }

    private void loadAllMothersFromFirebase() {
        FirebaseDatabase.getInstance().getReference("users")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        motherList.clear();
                        int highRisk = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            UserModel user = child.getValue(UserModel.class);
                            if (user == null) continue;
                            Calendar lmp = Calendar.getInstance();
                            lmp.set(user.lmpYear, user.lmpMonth, user.lmpDay);
                            long weeks = (Calendar.getInstance().getTimeInMillis()
                                    - lmp.getTimeInMillis()) / (1000L * 60 * 60 * 24 * 7);
                            String trimester = weeks <= 12 ? getString(R.string.trimester_1_short)
                                    : weeks <= 27 ? getString(R.string.trimester_2_short) : getString(R.string.trimester_3_short);
                            motherList.add(new MotherModel(
                                    user.name, user.area, maskMobile(user.mobile),
                                    user.riskScore, trimester));
                            if (user.riskScore >= 60) highRisk++;
                        }
                        motherAdapter.notifyDataSetChanged();
                        totalMothersText.setText(getString(R.string.total_mothers, motherList.size()));
                        highRiskText.setText(getString(R.string.high_risk_count, highRisk));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void loadCheckupData() {
        FirebaseDatabase.getInstance().getReference("users")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot usersSnap) {
                        Map<String, UserModel> usersMap = new HashMap<>();
                        for (DataSnapshot u : usersSnap.getChildren()) {
                            UserModel user = u.getValue(UserModel.class);
                            if (user != null) usersMap.put(u.getKey(), user);
                        }

                        FirebaseDatabase.getInstance().getReference("checkups")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot checkupsSnap) {
                                        checkupEntryList.clear();
                                        int upcoming = 0, overdue = 0, highRiskWeek = 0;

                                        for (DataSnapshot c : checkupsSnap.getChildren()) {
                                            String userId = c.getKey();
                                            CheckupModel checkup = c.getValue(CheckupModel.class);
                                            UserModel user = usersMap.get(userId);

                                            if (checkup == null || user == null) continue;

                                            long daysLeft = CheckupScheduler.daysUntilVisit(checkup.nextVisitDate);
                                            String status = CheckupScheduler.computeStatus(checkup.nextVisitDate);

                                            checkupEntryList.add(new CheckupEntry(
                                                    user.name, user.area, user.riskScore,
                                                    checkup.riskLevel, checkup.nextVisitDate,
                                                    status, daysLeft));

                                            if (daysLeft < 0) overdue++;
                                            else upcoming++;
                                            if (CheckupScheduler.LEVEL_HIGH.equals(checkup.riskLevel)
                                                    && daysLeft >= 0 && daysLeft <= 7) highRiskWeek++;
                                        }

                                        checkupAdapter.notifyDataSetChanged();
                                        checkupUpcomingCount.setText(getString(R.string.upcoming_count, upcoming));
                                        checkupOverdueCount.setText(getString(R.string.overdue_count, overdue));
                                        checkupHighRiskCount.setText(getString(R.string.high_risk_week_count, highRiskWeek));
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {}
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    /**
     * Masks mobile number for privacy in normal views.
     * Shows only last 2 digits (e.g., 9876543210 → ********10).
     * For Emergency Alerts, use full number from AlertModel instead.
     *
     * @param number Mobile number string, may be null
     * @return Masked string, or "N/A" if null/invalid
     */
    private static String maskMobile(String number) {
        if (number == null || number.trim().isEmpty()) return "N/A";
        String n = number.trim();
        if (n.length() >= 4) {
            int keep = 2;
            int maskCount = n.length() - keep;
            StringBuilder sb = new StringBuilder(maskCount + keep);
            for (int i = 0; i < maskCount; i++) sb.append('*');
            sb.append(n.substring(n.length() - keep));
            return sb.toString();
        }
        return "N/A";
    }

    private void loadSummary() {
        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        String area  = prefs.getString("area", "No Area");
        int year     = prefs.getInt("year", 2024);
        int month    = prefs.getInt("month", 0);
        int day      = prefs.getInt("day", 1);
        Calendar lmp = Calendar.getInstance();
        lmp.set(year, month, day);
        long weeks = (Calendar.getInstance().getTimeInMillis()
                - lmp.getTimeInMillis()) / (1000L * 60 * 60 * 24 * 7);
        String trimester = weeks <= 12 ? getString(R.string.trimester_1)
                : weeks <= 27 ? getString(R.string.trimester_2) : getString(R.string.trimester_3);
        areaSummary.setText(getString(R.string.area_prefix) + " " + area);
        trimesterSummary.setText(trimester);
        riskSummary.setText(weeks > 36 ? getString(R.string.high_monitoring) : getString(R.string.stable_status));
    }

    private void clearEmergencyAlert() {
        alertRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (ringtone != null && ringtone.isPlaying()) { ringtone.stop(); ringtone = null; }
                alertStatusText.setText(getString(R.string.no_alerts));
                alertStatusText.setTextColor(getResources().getColor(android.R.color.black));
                Toast.makeText(this, getString(R.string.alert_cleared), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void playAlertSound() {
        try {
            if (ringtone != null && ringtone.isPlaying()) return;
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null)
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), alarmSound);
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ringtone.setLooping(true);
                ringtone.play();
            }
        } catch (Exception e) {}
    }

    private void vibrateDevice() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(new long[]{0, 500, 200, 500, 200, 500}, -1);
            }
        } catch (Exception e) {}
    }

    private void showNotification(String title, String message) {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "admin_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(new NotificationChannel(
                    channelId, "Admin Alerts", NotificationManager.IMPORTANCE_HIGH));
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (alertRef != null && alertListener != null)
            alertRef.removeEventListener(alertListener);
        if (ringtone != null && ringtone.isPlaying()) ringtone.stop();
    }
}
