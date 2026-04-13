package com.example.lunara;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.content.Intent;

public class SplashActivity extends AppCompatActivity {

    Button startBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        startBtn = findViewById(R.id.startBtn);

        // Apply gentle pulse animation to CTA button
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_scale);
        startBtn.startAnimation(pulse);

        startBtn.setOnClickListener(v -> {
            startBtn.clearAnimation(); // stop pulse on click for clean feel
            startActivity(new Intent(SplashActivity.this, RoleSelectionActivity.class));
        });
    }
}