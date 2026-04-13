package com.example.lunara;

import android.os.Bundle;
import android.widget.*;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RiskAlertActivity extends BaseDrawerActivity {

    CheckBox bleeding, severePain, noMovement,
             highFever, swelling, headache,
             seizure, vomiting, backPain, tiredness;

    TextView riskResult;
    Button checkRiskBtn;

    long weeks = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_risk_alert);

        setupDrawer();

        bleeding   = findViewById(R.id.bleeding);
        severePain = findViewById(R.id.severePain);
        noMovement = findViewById(R.id.noMovement);
        seizure    = findViewById(R.id.seizure);

        highFever = findViewById(R.id.highFever);
        swelling  = findViewById(R.id.swelling);
        headache  = findViewById(R.id.headache);
        vomiting  = findViewById(R.id.vomiting);

        backPain  = findViewById(R.id.backPain);
        tiredness = findViewById(R.id.tiredness);

        riskResult   = findViewById(R.id.riskResult);
        checkRiskBtn = findViewById(R.id.checkRiskBtn);

        loadWeeksFromFirebase();

        checkRiskBtn.setOnClickListener(v -> evaluateRisk());
    }

    private void loadWeeksFromFirebase() {
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

                        java.util.Calendar lmp = java.util.Calendar.getInstance();
                        lmp.set(user.lmpYear, user.lmpMonth, user.lmpDay);

                        java.util.Calendar today = java.util.Calendar.getInstance();
                        long diff = today.getTimeInMillis() - lmp.getTimeInMillis();
                        weeks = (diff / (1000 * 60 * 60 * 24)) / 7;
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void evaluateRisk() {
        int highScore     = 0;
        int moderateScore = 0;
        int mildScore     = 0;

        if (bleeding.isChecked())                      highScore += 5;
        if (severePain.isChecked())                    highScore += 5;
        if (noMovement.isChecked() && weeks > 28)      highScore += 5;
        if (seizure.isChecked())                       highScore += 6;

        if (highFever.isChecked())                     moderateScore += 3;
        if (swelling.isChecked())                      moderateScore += 3;
        if (headache.isChecked())                      moderateScore += 3;
        if (vomiting.isChecked())                      moderateScore += 2;

        if (backPain.isChecked())                      mildScore += 1;
        if (tiredness.isChecked())                     mildScore += 1;

        if (highScore >= 5) {
            showRisk(getString(R.string.risk_high_title),
                    getString(R.string.risk_high_msg),
                    android.R.color.holo_red_dark, true);
        } else if (moderateScore >= 3) {
            showRisk(getString(R.string.risk_mod_title),
                    getString(R.string.risk_mod_msg),
                    android.R.color.holo_orange_dark, true);
        } else if (mildScore >= 1) {
            showRisk(getString(R.string.risk_low_title),
                    getString(R.string.risk_low_msg),
                    android.R.color.holo_green_dark, false);
        } else {
            showRisk(getString(R.string.risk_none_title),
                    getString(R.string.risk_none_msg),
                    android.R.color.holo_green_dark, false);
        }
    }

    private void showRisk(String title, String message, int color, boolean playSound) {
        riskResult.setText(title + "\n" + message);
        riskResult.setTextColor(getResources().getColor(color));

        if (playSound) {
            showNotification(title, message);
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            RingtoneManager.getRingtone(this, notification).play();
        }
    }

    private void showNotification(String title, String message) {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "risk_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Risk Alerts", NotificationManager.IMPORTANCE_HIGH);
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
