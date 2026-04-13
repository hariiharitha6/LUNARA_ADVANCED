package com.example.lunara;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public abstract class BaseDrawerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Toolbar toolbar;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setupDrawer() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(this, WomanDashboardActivity.class));
        } else if (id == R.id.nav_health) {
            startActivity(new Intent(this, HealthTrackingActivity.class));
        } else if (id == R.id.nav_baby) {
            startActivity(new Intent(this, BabyDevelopmentActivity.class));
        } else if (id == R.id.nav_risk) {
            startActivity(new Intent(this, RiskAlertActivity.class));
        } else if (id == R.id.nav_scheduler) {
            startActivity(new Intent(this, SmartSchedulerActivity.class));
        } else if (id == R.id.nav_reminders) {
            startActivity(new Intent(this, ReminderActivity.class));
        } else if (id == R.id.nav_doctor) {
            startActivity(new Intent(this, DoctorContactActivity.class));
        } else if (id == R.id.nav_chat) {
            startActivity(new Intent(this, ChatAssistantActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_logout) {
            showCustomLogoutDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showCustomLogoutDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.LunaraDialogTheme)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        TextView message = dialogView.findViewById(R.id.dialogMessage);
        TextView icon = dialogView.findViewById(R.id.dialogIcon);
        Button btnPos = dialogView.findViewById(R.id.btnPositive);
        Button btnNeg = dialogView.findViewById(R.id.btnNegative);

        icon.setText("🚪");
        title.setText(getString(R.string.logout));
        message.setText(getString(R.string.logout_message));

        btnPos.setOnClickListener(v -> {
            dialog.dismiss();
            performLogout();
        });

        btnNeg.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void performLogout() {
        getSharedPreferences("UserData", MODE_PRIVATE).edit()
                .remove("current_user_id")
                .remove("currentUserId")
                .remove("isLoggedIn")
                .apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
