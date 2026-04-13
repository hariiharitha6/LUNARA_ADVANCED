package com.example.lunara;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Calendar;

public class BabyDevelopmentActivity extends BaseDrawerActivity {

    TextView weekText, developmentText, quoteText;
    long weeks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_baby_development);

        setupDrawer();

        weekText = findViewById(R.id.weekText);
        developmentText = findViewById(R.id.developmentText);
        quoteText = findViewById(R.id.quoteText);

        calculateWeek();
        quoteText.setText(getDailyQuote(weeks));
    }

    private String getDailyQuote(long week) {
        Calendar calendar = Calendar.getInstance();
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        if (week <= 12) {
            String[] firstTrimesterQuotes = {
                    getString(R.string.quote_1_1),
                    getString(R.string.quote_1_2),
                    getString(R.string.quote_1_3),
                    getString(R.string.quote_1_4)
            };
            return firstTrimesterQuotes[dayOfMonth % firstTrimesterQuotes.length];
        } else if (week <= 27) {
            String[] secondTrimesterQuotes = {
                    getString(R.string.quote_2_1),
                    getString(R.string.quote_2_2),
                    getString(R.string.quote_2_3),
                    getString(R.string.quote_2_4)
            };
            return secondTrimesterQuotes[dayOfMonth % secondTrimesterQuotes.length];
        } else {
            String[] thirdTrimesterQuotes = {
                    getString(R.string.quote_3_1),
                    getString(R.string.quote_3_2),
                    getString(R.string.quote_3_3),
                    getString(R.string.quote_3_4)
            };
            return thirdTrimesterQuotes[dayOfMonth % thirdTrimesterQuotes.length];
        }
    }

    private void calculateWeek() {
        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);

        int year = prefs.getInt("year", 2024);
        int month = prefs.getInt("month", 0);
        int day = prefs.getInt("day", 1);

        Calendar lmp = Calendar.getInstance();
        lmp.set(year, month, day);

        Calendar today = Calendar.getInstance();

        weeks = ((today.getTimeInMillis() - lmp.getTimeInMillis())
                / (1000 * 60 * 60 * 24)) / 7;

        TextView weekTitle = findViewById(R.id.weekTitle);
        if (weekTitle != null) {
            weekTitle.setText(getString(R.string.week_prefix) + " " + weeks);
        } else if (weekText != null) {
            weekText.setText(getString(R.string.week_prefix) + " " + weeks);
        }

        if (weeks <= 12) {
            developmentText.setText(getString(R.string.baby_dev_1));
        } else if (weeks <= 27) {
            developmentText.setText(getString(R.string.baby_dev_2));
        } else {
            developmentText.setText(getString(R.string.baby_dev_3));
        }
    }
}
