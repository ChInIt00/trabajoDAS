package com.example.trabajodas.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean logged = getSharedPreferences("user", MODE_PRIVATE)
                .getBoolean("logged", false);

        String username = getSharedPreferences("user", MODE_PRIVATE)
                .getString("username", null);

        Intent intent;

        if (logged && username != null) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
