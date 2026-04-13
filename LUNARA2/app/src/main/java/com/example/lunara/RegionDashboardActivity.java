package com.example.lunara;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.content.Intent;

public class RegionDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_region_dashboard);

        Button region1 = findViewById(R.id.region1);
        Button region2 = findViewById(R.id.region2);
        Button region3 = findViewById(R.id.region3);

        region1.setOnClickListener(v -> openRegion("Region A"));
        region2.setOnClickListener(v -> openRegion("Region B"));
        region3.setOnClickListener(v -> openRegion("Region C"));
    }

    private void openRegion(String regionName) {
        Intent intent = new Intent(this, MotherListActivity.class);
        intent.putExtra("region", regionName);
        startActivity(intent);
    }
}