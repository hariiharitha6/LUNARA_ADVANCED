package com.example.lunara;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends BaseActivity {

    EditText mobileInput, passwordInput;
    Button loginBtn;
    TextView signupText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mobileInput   = findViewById(R.id.mobileInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginBtn      = findViewById(R.id.loginBtn);
        signupText    = findViewById(R.id.signupText);

        if (loginBtn != null) {
            loginBtn.setOnClickListener(v -> {
                String mob = mobileInput.getText().toString().trim();
                String pass = passwordInput.getText().toString().trim();
                if (mob.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                validateLoginFirebase(mob, pass);
            });
        }

        if (signupText != null) {
            signupText.setOnClickListener(v ->
                    startActivity(new Intent(this, RegistrationActivity.class)));
        }
    }

    private void validateLoginFirebase(String inputMobile, String inputPassword) {

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Logging in...");
        dialog.setCancelable(false);
        dialog.show();

        FirebaseDatabase.getInstance().getReference("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        dialog.dismiss();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            UserModel user = child.getValue(UserModel.class);
                            if (user == null) continue;

                            if (inputMobile.equals(user.mobile) &&
                                    inputPassword.equals(user.password)) {

                                SharedPreferences.Editor editor =
                                        getSharedPreferences("UserData", MODE_PRIVATE).edit();
                                editor.putString("current_user_id", child.getKey());
                                editor.putBoolean("isLoggedIn", true);
                                editor.apply();

                                Intent intent = new Intent(LoginActivity.this,
                                        WomanDashboardActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                                return;
                            }
                        }

                        Toast.makeText(LoginActivity.this,
                                "Invalid Mobile or Password",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        dialog.dismiss();
                        Toast.makeText(LoginActivity.this,
                                "Login error: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}