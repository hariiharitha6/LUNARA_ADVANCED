package com.example.lunara;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

public class SettingsActivity extends BaseDrawerActivity {

    Spinner languageSpinner, reminderSpinner;
    Switch notificationSwitch;
    Button editProfileBtn, logoutBtn;

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupDrawer();

        prefs = getSharedPreferences("app_settings", MODE_PRIVATE);

        languageSpinner   = findViewById(R.id.languageSpinner);
        reminderSpinner   = findViewById(R.id.reminderSpinner);
        notificationSwitch = findViewById(R.id.notificationSwitch);
        editProfileBtn    = findViewById(R.id.editProfileBtn);
        logoutBtn         = findViewById(R.id.logoutBtn);

        setupLanguageSpinner();
        setupReminderSpinner();
        setupNotificationSwitch();

        editProfileBtn.setOnClickListener(v ->
                startActivity(new Intent(this, RegistrationActivity.class)));

        logoutBtn.setOnClickListener(v -> showLogoutDialog());
    }

    private void setupLanguageSpinner() {
        String[] langs = {"English", "മലയാളം"};
        String[] codes = {"en", "ml"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, langs);
        languageSpinner.setAdapter(adapter);

        String current = LocaleHelper.getPersistedLanguage(this);
        languageSpinner.setSelection(current.equals("ml") ? 1 : 0);

        languageSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            boolean init = true;
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int pos, long id) {
                if (init) { init = false; return; }
                String selectedLang = codes[pos];
                String currentLang = LocaleHelper.getPersistedLanguage(SettingsActivity.this);
                
                if (!selectedLang.equals(currentLang)) {
                    LocaleHelper.setLocale(SettingsActivity.this, selectedLang);
                    // Restart entire task so all activities get the new locale
                    Intent restart = new Intent(SettingsActivity.this, WomanDashboardActivity.class);
                    restart.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(restart);
                    finish();
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupReminderSpinner() {
        String[] freqs = {getString(R.string.daily), getString(R.string.weekly)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, freqs);
        reminderSpinner.setAdapter(adapter);

        String saved = prefs.getString("reminder_freq", "daily");
        reminderSpinner.setSelection(saved.equals("weekly") ? 1 : 0);

        reminderSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            boolean init = true;
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int pos, long id) {
                if (init) { init = false; return; }
                prefs.edit().putString("reminder_freq", pos == 1 ? "weekly" : "daily").apply();
                Toast.makeText(SettingsActivity.this, getString(R.string.reminder_set_to) + " " + freqs[pos], Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupNotificationSwitch() {
        boolean enabled = prefs.getBoolean("notifications_enabled", true);
        notificationSwitch.setChecked(enabled);
        notificationSwitch.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("notifications_enabled", isChecked).apply());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.logout))
                .setMessage(getString(R.string.logout_confirm))
                .setPositiveButton(getString(R.string.yes), (d, w) -> {
                    getSharedPreferences("UserData", MODE_PRIVATE)
                            .edit()
                            .remove("current_user_id")   // matches key written by LoginActivity
                            .remove("isLoggedIn")
                            .apply();

                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }
}
