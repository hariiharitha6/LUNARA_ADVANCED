package com.example.lunara;

import android.os.Bundle;
import android.widget.*;
import android.content.SharedPreferences;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.content.Context;
import android.text.TextUtils;
import android.graphics.Color;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.*;

public class HealthTrackingActivity extends BaseDrawerActivity {

    EditText bpInput, sugarInput, weightInput, hemoInput;
    Button saveHealthBtn;
    LineChart healthChart;
    ProgressBar riskProgress;
    TextView riskStatus;

    private int lmpYear = 2024, lmpMonth = 0, lmpDay = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_tracking);

        setupDrawer();

        bpInput      = findViewById(R.id.bpInput);
        sugarInput   = findViewById(R.id.sugarInput);
        weightInput  = findViewById(R.id.weightInput);
        hemoInput    = findViewById(R.id.hemoInput);
        saveHealthBtn = findViewById(R.id.saveHealthBtn);
        healthChart  = findViewById(R.id.healthChart);
        riskProgress = findViewById(R.id.riskProgress);
        riskStatus   = findViewById(R.id.riskStatus);

        loadLmpFromFirebase();
        loadGraph();

        styleChart();

        saveHealthBtn.setOnClickListener(v -> {
            saveHealthData();
            loadGraph();
        });
    }

    private void styleChart() {
        healthChart.getDescription().setEnabled(false);
        healthChart.getLegend().setTextColor(Color.parseColor("#0F766E"));
        healthChart.setDrawGridBackground(false);
        healthChart.getAxisRight().setEnabled(false);
        healthChart.getAxisLeft().setGridColor(Color.parseColor("#E0F2F1"));
        healthChart.getAxisLeft().setTextColor(Color.parseColor("#0F766E"));
        healthChart.getXAxis().setGridColor(Color.parseColor("#E0F2F1"));
        healthChart.getXAxis().setTextColor(Color.parseColor("#0F766E"));
        healthChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        healthChart.setTouchEnabled(true);
        healthChart.setPinchZoom(true);
        healthChart.animateY(1200);
    }

    private void loadLmpFromFirebase() {
        String userId = getSharedPreferences("UserData", MODE_PRIVATE)
                .getString("current_user_id", null);
        if (userId == null) return;

        FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        UserModel user = snapshot.getValue(UserModel.class);
                        if (user == null) return;
                        lmpYear  = user.lmpYear;
                        lmpMonth = user.lmpMonth;
                        lmpDay   = user.lmpDay;
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void saveHealthData() {
        String bp     = bpInput.getText().toString().trim();
        String sugar  = sugarInput.getText().toString().trim();
        String weight = weightInput.getText().toString().trim();
        String hemo   = hemoInput.getText().toString().trim();

        if (TextUtils.isEmpty(bp) || TextUtils.isEmpty(sugar)
                || TextUtils.isEmpty(weight) || TextUtils.isEmpty(hemo)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("HealthHistory", MODE_PRIVATE);
        long timestamp = System.currentTimeMillis();
        prefs.edit()
                .putString("bp_"     + timestamp, bp)
                .putString("sugar_"  + timestamp, sugar)
                .putString("weight_" + timestamp, weight)
                .putString("hemo_"   + timestamp, hemo)
                .apply();

        analyzeRisk(bp, sugar, hemo);
        Toast.makeText(this, "Health Data Saved", Toast.LENGTH_SHORT).show();
    }

    private void analyzeRisk(String bp, String sugar, String hemo) {
        try {
            int riskScore = 0;
            int systolic;

            if (bp.contains("/")) {
                systolic = Integer.parseInt(bp.split("/")[0]);
            } else {
                systolic = Integer.parseInt(bp);
            }

            int sugarValue = Integer.parseInt(sugar);
            double hemoValue = Double.parseDouble(hemo);

            if (systolic >= 140)  riskScore += 30;
            if (sugarValue >= 140) riskScore += 25;
            if (hemoValue < 10)   riskScore += 20;

            Calendar lmp = Calendar.getInstance();
            lmp.set(lmpYear, lmpMonth, lmpDay);
            Calendar today = Calendar.getInstance();
            long diff  = today.getTimeInMillis() - lmp.getTimeInMillis();
            long weeks = (diff / (1000 * 60 * 60 * 24)) / 7;
            if (weeks > 36) riskScore += 15;

            riskProgress.setProgress(riskScore);

            if (riskScore <= 30) {
                riskStatus.setText("🟢 Low Risk (" + riskScore + "%)");
                riskStatus.setTextColor(Color.parseColor("#16A34A"));
            } else if (riskScore <= 60) {
                riskStatus.setText("🟡 Moderate Risk (" + riskScore + "%)");
                riskStatus.setTextColor(Color.parseColor("#F59E0B"));
            } else {
                riskStatus.setText("🔴 High Risk (" + riskScore + "%)");
                riskStatus.setTextColor(Color.parseColor("#DC2626"));
            }

            if (systolic >= 140)
                showNotification("⚠ High Blood Pressure",
                        "Your BP is high. Please consult doctor immediately.");
            if (sugarValue >= 140)
                showNotification("⚠ High Sugar Level", "Monitor sugar levels carefully.");
            if (hemoValue < 10)
                showNotification("⚠ Low Hemoglobin", "Iron deficiency detected.");

        } catch (Exception e) {
            Toast.makeText(this, "Invalid Input Format (BP example: 120/80)",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void loadGraph() {
        SharedPreferences prefs = getSharedPreferences("HealthHistory", MODE_PRIVATE);
        Map<String, ?> allData = prefs.getAll();

        List<Long> timestamps = new ArrayList<>();
        for (String key : allData.keySet()) {
            if (key.startsWith("sugar_")) {
                timestamps.add(Long.parseLong(key.replace("sugar_", "")));
            }
        }
        Collections.sort(timestamps);

        List<Entry> sugarEntries = new ArrayList<>();
        List<Entry> bpEntries    = new ArrayList<>();
        int index = 0;

        for (Long time : timestamps) {
            String sugarVal = prefs.getString("sugar_" + time, "0");
            String bpVal    = prefs.getString("bp_"    + time, "0");

            sugarEntries.add(new Entry(index, Float.parseFloat(sugarVal)));
            if (bpVal.contains("/")) {
                bpEntries.add(new Entry(index, Float.parseFloat(bpVal.split("/")[0])));
            }
            index++;
        }

        LineDataSet sugarSet = new LineDataSet(sugarEntries, "Sugar Level");
        sugarSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        sugarSet.setColor(Color.parseColor("#F472B6"));
        sugarSet.setCircleColor(Color.parseColor("#F472B6"));
        sugarSet.setValueTextColor(Color.parseColor("#374151"));
        sugarSet.setLineWidth(2.5f);
        sugarSet.setCircleRadius(4f);
        sugarSet.setDrawFilled(true);
        sugarSet.setFillColor(Color.parseColor("#FCE7F3"));

        LineDataSet bpSet = new LineDataSet(bpEntries, "BP (Systolic)");
        bpSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        bpSet.setColor(Color.parseColor("#0F766E"));
        bpSet.setCircleColor(Color.parseColor("#0F766E"));
        bpSet.setValueTextColor(Color.parseColor("#374151"));
        bpSet.setLineWidth(2.5f);
        bpSet.setCircleRadius(4f);
        bpSet.setDrawFilled(true);
        bpSet.setFillColor(Color.parseColor("#CCFBF1"));

        healthChart.setData(new LineData(sugarSet, bpSet));
        healthChart.invalidate();
    }

    private void showNotification(String title, String message) {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "health_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Health Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
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
}
