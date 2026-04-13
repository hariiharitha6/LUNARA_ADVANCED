package com.example.lunara;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MotherListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mother_list);

        String region = getIntent().getStringExtra("region");

        TextView regionTitle = findViewById(R.id.regionTitle);
        regionTitle.setText(region + " Mothers List");
    }
}