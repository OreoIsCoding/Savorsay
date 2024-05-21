package com.example.savorsayapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreenActivity extends AppCompatActivity {
    private static final long SPLASH_TIME_OUT = 3000; // 3 seconds in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView splashImageView= findViewById(R.id.splashScreen);

        Animation fadeIn = AnimationUtils.loadAnimation(this,R.anim.fade_in);
        splashImageView.startAnimation(fadeIn);


        new Handler().postDelayed(() -> {
            // After the splash timeout, navigate to the next activity
            Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_TIME_OUT);
    }
}
