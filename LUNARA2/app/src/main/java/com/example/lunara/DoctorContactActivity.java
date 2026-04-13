package com.example.lunara;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

public class DoctorContactActivity extends BaseDrawerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_contact);

        setupDrawer();

        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);

        Button call108 = findViewById(R.id.call108Btn);
        Button call181 = findViewById(R.id.call181Btn);

        call108.setOnClickListener(v -> dialNumber("108"));
        call181.setOnClickListener(v -> dialNumber("181"));

        TextView doctorName = findViewById(R.id.doctorNameText);
        TextView doctorPhone = findViewById(R.id.doctorPhoneText);
        Button callDoctorBtn = findViewById(R.id.callDoctorBtn);
        Button whatsappBtn   = findViewById(R.id.whatsappDoctorBtn);
        Button saveDoctorBtn = findViewById(R.id.saveDoctorBtn);

        String savedName  = prefs.getString("doctor_name", "");
        String savedPhone = prefs.getString("doctor_phone", "");

        if (!savedName.isEmpty()) {
            doctorName.setText(savedName);
            doctorPhone.setText(savedPhone);
        } else {
            doctorName.setText(getString(R.string.no_doctor_saved));
            doctorPhone.setText("");
        }

        callDoctorBtn.setOnClickListener(v -> {
            String phone = prefs.getString("doctor_phone", "");
            if (!phone.isEmpty()) dialNumber(phone);
            else Toast.makeText(this, getString(R.string.no_doctor_saved), Toast.LENGTH_SHORT).show();
        });

        whatsappBtn.setOnClickListener(v -> {
            String phone = prefs.getString("doctor_phone", "");
            if (!phone.isEmpty()) openWhatsApp(phone);
            else Toast.makeText(this, getString(R.string.no_doctor_saved), Toast.LENGTH_SHORT).show();
        });

        saveDoctorBtn.setOnClickListener(v -> showSaveDoctorDialog(prefs, doctorName, doctorPhone));
    }

    private void dialNumber(String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot make call", Toast.LENGTH_SHORT).show();
        }
    }

    private void openWhatsApp(String phone) {
        try {
            String url = "https://wa.me/91" + phone;
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSaveDoctorDialog(SharedPreferences prefs, TextView nameView, TextView phoneView) {
        EditText nameInput = new EditText(this);
        nameInput.setHint(getString(R.string.full_name));
        EditText phoneInput = new EditText(this);
        phoneInput.setHint(getString(R.string.mobile_number));
        phoneInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);
        layout.addView(nameInput);
        layout.addView(phoneInput);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.save_doctor))
                .setView(layout)
                .setPositiveButton(getString(R.string.yes), (d, w) -> {
                    String name  = nameInput.getText().toString().trim();
                    String phone = phoneInput.getText().toString().trim();
                    if (!name.isEmpty() && !phone.isEmpty()) {
                        prefs.edit()
                                .putString("doctor_name", name)
                                .putString("doctor_phone", phone)
                                .apply();
                        nameView.setText(name);
                        phoneView.setText(phone);
                        Toast.makeText(this, "Doctor saved", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
}
