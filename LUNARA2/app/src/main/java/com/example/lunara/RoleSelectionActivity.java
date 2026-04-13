package com.example.lunara;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.app.AlertDialog;
import android.content.Intent;
import android.widget.Toast;

public class RoleSelectionActivity extends AppCompatActivity {

    Button womanBtn, adminBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        womanBtn = findViewById(R.id.womanBtn);
        adminBtn = findViewById(R.id.adminBtn);

        // Woman Login
        womanBtn.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this,
                    LoginActivity.class);
            startActivity(intent);
        });

        // Admin Login
        adminBtn.setOnClickListener(v -> showAdminLogin());
    }

    private void showAdminLogin() {

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter Admin Password");
        passwordInput.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Admin Login")
                .setView(passwordInput)
                .setCancelable(false)
                .setPositiveButton("Login", null)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            String password = passwordInput.getText().toString().trim();

            if (password.equals("1234")) {

                dialog.dismiss();

                Intent intent = new Intent(RoleSelectionActivity.this,
                        AdminDashboardActivity.class);

                startActivity(intent);

                // 🚫 DO NOT use finish() here

            } else {

                Toast.makeText(RoleSelectionActivity.this,
                        "Incorrect Admin Password",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}