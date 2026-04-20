package com.example.trabajodas.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.trabajodas.R;
import com.example.trabajodas.activity.MainActivity;

import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import java.util.Locale;
import android.content.res.Configuration;
import com.example.trabajodas.utils.Preferencias;


public class SettingsActivity extends AppCompatActivity {

    private RadioGroup radioGroup;
    private Spinner spinnerIdioma;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        radioGroup = findViewById(R.id.radioGroupTheme);

        // Marcar opción guardada
        int modo = getSharedPreferences("ajustes", MODE_PRIVATE)
                .getInt("modo_tema", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        if (modo == AppCompatDelegate.MODE_NIGHT_NO) {
            radioGroup.check(R.id.radioClaro);
        } else if (modo == AppCompatDelegate.MODE_NIGHT_YES) {
            radioGroup.check(R.id.radioOscuro);
        } else {
            radioGroup.check(R.id.radioAuto);
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {

            int nuevoModo;

            if (checkedId == R.id.radioClaro) {
                nuevoModo = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.radioOscuro) {
                nuevoModo = AppCompatDelegate.MODE_NIGHT_YES;
            } else {
                nuevoModo = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }

            // Guardar preferencia
            getSharedPreferences("ajustes", MODE_PRIVATE)
                    .edit()
                    .putInt("modo_tema", nuevoModo)
                    .apply();

            // Aplicar modo
            AppCompatDelegate.setDefaultNightMode(nuevoModo);
        });

        spinnerIdioma = findViewById(R.id.spinnerIdioma);

// Adaptador
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.idiomas_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIdioma.setAdapter(adapter);

// Seleccionar idioma guardado
        String idiomaGuardado = Preferencias.getIdioma(this);

        if (idiomaGuardado.equals("es")) spinnerIdioma.setSelection(0);
        else if (idiomaGuardado.equals("en")) spinnerIdioma.setSelection(1);
        else spinnerIdioma.setSelection(2);

// Listener
        spinnerIdioma.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {

                String nuevoIdioma;

                if (position == 0) nuevoIdioma = "es";
                else if (position == 1) nuevoIdioma = "en";
                else nuevoIdioma = "eu";

                if (!nuevoIdioma.equals(Preferencias.getIdioma(SettingsActivity.this))) {

                    Preferencias.setIdioma(SettingsActivity.this, nuevoIdioma);

                    Locale locale = new Locale(nuevoIdioma);
                    Locale.setDefault(locale);

                    Configuration config = new Configuration();
                    config.setLocale(locale);

                    getResources().updateConfiguration(config, getResources().getDisplayMetrics());

                    Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Button btnMapa = findViewById(R.id.btnMapa);

        btnMapa.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        });

        Button btnUsuario = findViewById(R.id.btnUsuario);

        btnUsuario.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, UserActivity.class);
            startActivity(intent);
        });

        Button btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> {

            // 1. Borrar sesión
            getSharedPreferences("user", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            // 2. Ir a login y limpiar pila
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}