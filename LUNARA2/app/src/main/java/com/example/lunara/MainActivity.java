package com.example.lunara;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    DatePicker datePicker;
    Button calcBtn, checkRiskBtn, emergencyBtn;
    TextView resultText, babyInfo, riskResult, reminderText;
    CheckBox bleeding, fever, swelling, noMovement;

    long currentWeeks = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        datePicker = findViewById(R.id.datePicker);
        calcBtn = findViewById(R.id.calcBtn);
        resultText = findViewById(R.id.resultText);
        babyInfo = findViewById(R.id.babyInfo);

        bleeding = findViewById(R.id.bleeding);
        fever = findViewById(R.id.fever);
        swelling = findViewById(R.id.swelling);
        noMovement = findViewById(R.id.noMovement);

        checkRiskBtn = findViewById(R.id.checkRiskBtn);
        riskResult = findViewById(R.id.riskResult);
        emergencyBtn = findViewById(R.id.emergencyBtn);

        reminderText = findViewById(R.id.reminderText);

        calcBtn.setOnClickListener(v -> calculatePregnancy());
        checkRiskBtn.setOnClickListener(v -> checkRisk());

        emergencyBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(android.net.Uri.parse("tel:108"));
            startActivity(intent);
        });
    }

    private void calculatePregnancy() {

        Calendar lmp = Calendar.getInstance();
        lmp.set(datePicker.getYear(),
                datePicker.getMonth(),
                datePicker.getDayOfMonth());

        Calendar today = Calendar.getInstance();

        long diff = today.getTimeInMillis() - lmp.getTimeInMillis();
        long days = diff / (1000 * 60 * 60 * 24);
        currentWeeks = days / 7;

        String trimester;
        if (currentWeeks <= 12)
            trimester = "First Trimester";
        else if (currentWeeks <= 27)
            trimester = "Second Trimester";
        else
            trimester = "Third Trimester";

        Calendar dueDate = (Calendar) lmp.clone();
        dueDate.add(Calendar.DAY_OF_YEAR, 280);

        resultText.setText(
                "Pregnancy Week: " + currentWeeks +
                        "\nTrimester: " + trimester +
                        "\nDue Date: " + dueDate.getTime().toString()
        );

        babyInfo.setText(getBabyDevelopmentInfo(currentWeeks));
        reminderText.setText(getReminderMessage(currentWeeks));
    }

    private void checkRisk() {

        if (bleeding.isChecked()) {
            showHighRisk("Bleeding detected! Immediate hospital visit required.");
        }
        else if (noMovement.isChecked() && currentWeeks >= 20) {
            showHighRisk("No baby movement after 20 weeks! Emergency.");
        }
        else if (fever.isChecked() || swelling.isChecked()) {
            riskResult.setText("Moderate Risk. Consult health worker soon.");
            riskResult.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        }
        else {
            riskResult.setText("Low Risk. Continue regular prenatal care.");
            riskResult.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
    }

    private void showHighRisk(String message) {
        riskResult.setText(message);
        riskResult.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
    }

    private String getBabyDevelopmentInfo(long week) {

        if (week <= 12)
            return "Baby's major organs are forming. Take folic acid daily.";
        else if (week <= 27)
            return "Baby can hear sounds. Regular ultrasound checkups important.";
        else
            return "Baby is gaining weight. Prepare hospital bag and birth plan.";
    }

    private String getReminderMessage(long week) {

        if (week <= 12)
            return "Reminder: Take Iron & Folic Acid tablets daily.";
        else if (week <= 20)
            return "Reminder: Schedule anomaly scan between 18-20 weeks.";
        else if (week <= 32)
            return "Reminder: Monitor baby movements daily.";
        else
            return "Reminder: Weekly checkups recommended until delivery.";
    }
}