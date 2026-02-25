package com.example.trabajodas.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.trabajodas.R;
import com.example.trabajodas.database.AppDatabase;
import com.example.trabajodas.model.Libro;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnTest = findViewById(R.id.btnTest);
        btnTest.setOnClickListener(v -> {
            AppDatabase db = AppDatabase.getInstance(this);
            Libro libroTest = new Libro("El Quijote", "Cervantes", "Novela", 5, true);
            db.libroDao().insertLibro(libroTest);
            Toast.makeText(this, "Libro insertado", Toast.LENGTH_SHORT).show();
            List<Libro> libros = db.libroDao().getAllLibros();
            for (Libro l : libros) {
                Log.d("TEST_DB", "ID: " + l.getId() + ", Titulo: " + l.getTitulo());
            }
        });
    }
}