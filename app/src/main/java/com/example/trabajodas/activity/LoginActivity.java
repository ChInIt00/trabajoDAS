package com.example.trabajodas.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.example.trabajodas.R;

public class LoginActivity extends AppCompatActivity {

    EditText etUser, etPass;
    Button btnLogin, btnRegister;

    String BASE_URL = "http://35.222.163.80:81/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUser = findViewById(R.id.etUser);
        etPass = findViewById(R.id.etPass);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> login());
        btnRegister.setOnClickListener(v -> register());
    }

    private void login() {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "login.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String json = "{ \"username\":\"" + etUser.getText().toString() +
                        "\", \"password\":\"" + etPass.getText().toString() + "\" }";

                OutputStream os = conn.getOutputStream();
                os.write(json.getBytes("UTF-8"));
                os.flush();
                os.close();

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                String response = br.readLine();

                runOnUiThread(() -> {
                    if (response.contains("true")) {

                        Toast.makeText(this, R.string.login_correcto, Toast.LENGTH_SHORT).show();

                        // 👇 AÑADE ESTO
                        String username = etUser.getText().toString();

                        getSharedPreferences("user", MODE_PRIVATE)
                                .edit()
                                .putString("username", username)
                                .putBoolean("logged", true)
                                .apply();

                        Intent intent = new Intent(this, MainActivity.class);
                        intent.putExtra("desdeLogin", true);
                        startActivity(intent);

                    } else {
                        Toast.makeText(this, "Error login", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void register() {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "register.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String json = "{ \"username\":\"" + etUser.getText().toString() +
                        "\", \"password\":\"" + etPass.getText().toString() + "\" }";

                OutputStream os = conn.getOutputStream();
                os.write(json.getBytes("UTF-8"));
                os.flush();
                os.close();

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                String response = br.readLine();

                runOnUiThread(() -> {
                    Toast.makeText(this, response, Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
