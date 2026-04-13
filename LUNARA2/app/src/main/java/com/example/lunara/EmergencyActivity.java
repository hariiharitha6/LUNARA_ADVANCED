package com.example.lunara;

import android.os.Bundle;
import android.widget.Button;
import android.content.Intent;
import android.net.Uri;

public class EmergencyActivity extends BaseDrawerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        setupDrawer();

        Button callBtn = findViewById(R.id.callBtn);

        callBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:108"));
            startActivity(intent);
        });
    }
}
