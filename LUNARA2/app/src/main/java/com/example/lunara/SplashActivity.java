package com.example.lunara;

import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.content.Intent;

public class SplashActivity extends BaseActivity {

    Button startBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        startBtn = findViewById(R.id.startBtn);

        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_scale);
        if (startBtn != null) startBtn.startAnimation(pulse);

        if (startBtn != null) {
            startBtn.setOnClickListener(v -> {
                startBtn.clearAnimation();
                startActivity(new Intent(SplashActivity.this, RoleSelectionActivity.class));
            });
        }
    }
}