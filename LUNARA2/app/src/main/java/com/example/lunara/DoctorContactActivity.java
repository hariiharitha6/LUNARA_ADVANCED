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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DoctorContactActivity extends BaseDrawerActivity {

    SharedPreferences prefs;
    RecyclerView doctorRecyclerView;
    TextView noDoctorsText;
    DoctorAdapter adapter;
    List<DoctorModel> doctorList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_contact);

        setupDrawer();

        prefs = getSharedPreferences("app_settings", MODE_PRIVATE);

        Button call108 = findViewById(R.id.call108Btn);
        Button call181 = findViewById(R.id.call181Btn);
        call108.setOnClickListener(v -> dialNumber("108"));
        call181.setOnClickListener(v -> dialNumber("181"));

        Button addDoctorBtn = findViewById(R.id.addDoctorBtn);
        noDoctorsText       = findViewById(R.id.noDoctorsText);
        doctorRecyclerView  = findViewById(R.id.doctorRecyclerView);

        doctorList = new ArrayList<>();
        adapter = new DoctorAdapter(doctorList, position -> {
            doctorList.remove(position);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, doctorList.size());
            saveDoctorsToPrefs();
            updateEmptyState();
            Toast.makeText(this, getString(R.string.doctor_deleted), Toast.LENGTH_SHORT).show();
        });

        doctorRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        doctorRecyclerView.setAdapter(adapter);

        // Migrate legacy single-doctor data if exists
        migrateLegacyDoctor();

        loadDoctorsFromPrefs();
        addDoctorBtn.setOnClickListener(v -> showAddDoctorDialog());
    }

    private void dialNumber(String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot make call", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddDoctorDialog() {
        EditText nameInput = new EditText(this);
        nameInput.setHint(getString(R.string.doctor_name_hint));
        EditText phoneInput = new EditText(this);
        phoneInput.setHint(getString(R.string.doctor_phone_hint));
        phoneInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);
        layout.addView(nameInput);
        layout.addView(phoneInput);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.add_doctor))
                .setView(layout)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    String name  = nameInput.getText().toString().trim();
                    String phone = phoneInput.getText().toString().trim();
                    if (!name.isEmpty() && !phone.isEmpty()) {
                        doctorList.add(new DoctorModel(name, phone));
                        adapter.notifyItemInserted(doctorList.size() - 1);
                        saveDoctorsToPrefs();
                        updateEmptyState();
                        Toast.makeText(this, getString(R.string.doctor_saved), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void migrateLegacyDoctor() {
        String legacyName = prefs.getString("doctor_name", "");
        String legacyPhone = prefs.getString("doctor_phone", "");
        if (!legacyName.isEmpty() && !legacyPhone.isEmpty()) {
            // Only migrate if no doctors_json exists yet
            String existing = prefs.getString("doctors_json", "");
            if (existing.isEmpty() || existing.equals("[]")) {
                try {
                    JSONArray arr = new JSONArray();
                    JSONObject obj = new JSONObject();
                    obj.put("name", legacyName);
                    obj.put("phone", legacyPhone);
                    arr.put(obj);
                    prefs.edit()
                            .putString("doctors_json", arr.toString())
                            .remove("doctor_name")
                            .remove("doctor_phone")
                            .apply();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void loadDoctorsFromPrefs() {
        doctorList.clear();
        String json = prefs.getString("doctors_json", "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                doctorList.add(new DoctorModel(
                        obj.getString("name"),
                        obj.getString("phone")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void saveDoctorsToPrefs() {
        try {
            JSONArray arr = new JSONArray();
            for (DoctorModel doc : doctorList) {
                JSONObject obj = new JSONObject();
                obj.put("name", doc.name);
                obj.put("phone", doc.phone);
                arr.put(obj);
            }
            prefs.edit().putString("doctors_json", arr.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateEmptyState() {
        noDoctorsText.setVisibility(doctorList.isEmpty()
                ? android.view.View.VISIBLE
                : android.view.View.GONE);
    }
}
