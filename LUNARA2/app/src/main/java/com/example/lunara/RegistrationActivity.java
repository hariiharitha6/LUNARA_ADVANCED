package com.example.lunara;

import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegistrationActivity extends BaseActivity {

    EditText name, area, mobile, weight, password;
    DatePicker datePicker;
    Button registerBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        name        = findViewById(R.id.name);
        area        = findViewById(R.id.area);
        mobile      = findViewById(R.id.mobile);
        weight      = findViewById(R.id.weight);
        password    = findViewById(R.id.password);
        datePicker  = findViewById(R.id.datePicker);
        registerBtn = findViewById(R.id.registerBtn);

        registerBtn.setOnClickListener(v -> validateAndSave());
    }

    private void validateAndSave() {

        String nameStr     = name.getText().toString().trim();
        String areaStr     = area.getText().toString().trim();
        String mobileStr   = mobile.getText().toString().trim();
        String weightStr   = weight.getText().toString().trim();
        String passwordStr = password.getText().toString().trim();

        if (nameStr.isEmpty())          { name.setError("Enter Name");                       return; }
        if (areaStr.isEmpty())          { area.setError("Enter Area");                       return; }
        if (mobileStr.length() != 10)   { mobile.setError("Enter valid 10-digit number");   return; }
        if (passwordStr.length() < 4)   { password.setError("Password min 4 characters");   return; }
        if (weightStr.isEmpty())        { weight.setError("Enter Weight");                   return; }

        saveToFirebase(nameStr, areaStr, mobileStr, weightStr, passwordStr);
    }

    private void saveToFirebase(String nameStr, String areaStr,
                                String mobileStr, String weightStr,
                                String passwordStr) {

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Registering...");
        dialog.setCancelable(false);
        dialog.show();

        DatabaseReference usersRef =
                FirebaseDatabase.getInstance().getReference("users");

        String userId = usersRef.push().getKey();
        if (userId == null) {
            dialog.dismiss();
            Toast.makeText(this, "Error: Could not generate ID", Toast.LENGTH_SHORT).show();
            return;
        }

        UserModel user = new UserModel(
                userId, nameStr, mobileStr, areaStr, weightStr, passwordStr,
                datePicker.getYear(),
                datePicker.getMonth(),
                datePicker.getDayOfMonth()
        );

        usersRef.child(userId).setValue(user)
                .addOnSuccessListener(unused -> {
                    dialog.dismiss();

                    // Save session: store userId for WomanDashboard to read
                    SharedPreferences.Editor editor =
                            getSharedPreferences("UserData", MODE_PRIVATE).edit();
                    editor.putString("current_user_id", userId);
                    editor.apply();

                    Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Registration failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}