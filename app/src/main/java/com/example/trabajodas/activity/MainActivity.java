package com.example.trabajodas.activity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trabajodas.R;
import com.example.trabajodas.adapter.LibroAdapter;
import com.example.trabajodas.database.AppDatabase;
import com.example.trabajodas.model.Libro;
import com.example.trabajodas.utils.Preferencias;
import com.google.android.material.appbar.MaterialToolbar;

import android.view.View;
import android.widget.EditText;
import android.widget.CheckBox;
import androidx.appcompat.app.AlertDialog;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LibroAdapter adapter;
    private List<Libro> listaLibros;

    private static final String CANAL_ID = "libros_canal";

    private static final int PERMISO_NOTIFICACIONES = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String idioma = Preferencias.getIdioma(this);

        Locale locale = new Locale(idioma);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        super.onCreate(savedInstanceState);
        // Pedir permiso para notificaciones si es Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISO_NOTIFICACIONES);
            }
        }
        EdgeToEdge.enable(this);
        crearCanalNotificacion();
        int modo = getSharedPreferences("ajustes", MODE_PRIVATE)
                .getInt("modo_tema", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        AppCompatDelegate.setDefaultNightMode(modo);
        setContentView(R.layout.activity_main);

        // Inicializar RecyclerView
        recyclerView = findViewById(R.id.recyclerViewLibros);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Cargar libros de la base de datos
        AppDatabase db = AppDatabase.getInstance(this);
        listaLibros = new ArrayList<>();

        // Crear adapter
        adapter = new LibroAdapter(this, listaLibros);
        recyclerView.setAdapter(adapter);
        cargarLibros();

        adapter.setOnItemClickListener(new LibroAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Libro libro) {
                // Click normal → editar libro
                mostrarDialogoEditarLibro(libro);
            }

            @Override
            public void onItemLongClick(Libro libro) {
                // Click largo → eliminar libro
                new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.eliminar_libro)
                        .setMessage(getString(R.string.deseas_eliminar_libro) + " " + libro.getTitulo())
                        .setPositiveButton(R.string.si, (dialog, which) -> {
                            // Borrar libro de la base de datos
                            AppDatabase db = AppDatabase.getInstance(MainActivity.this);
                            db.libroDao().deleteLibro(libro);

                            // Actualizar lista y RecyclerView respetando el filtro
                            cargarLibros();

                            Toast.makeText(MainActivity.this, R.string.libro_eliminado, Toast.LENGTH_SHORT).show();
                            mostrarNotificacion(getString(R.string.libro_eliminado), getString(R.string.se_ha_eliminado) + " " + libro.getTitulo());
                        })
                        .setNegativeButton(R.string.cancelar, (dialog, which) -> dialog.dismiss())
                        .show();
            }
        });

        // Botón insertar libro
        Button btnTest = findViewById(R.id.btnTest);
        btnTest.setText(R.string.agregar_libro);
        btnTest.setOnClickListener(v -> mostrarDialogoAgregarLibro());

        //Botón cambiar mostrar/ocultar leídos (preferencia)
        Button btnToggleFiltro = findViewById(R.id.btnToggleFiltro);

        btnToggleFiltro.setOnClickListener(v -> {

            boolean actual = Preferencias.getSoloLeidos(this);

            Preferencias.setSoloLeidos(this, !actual);

            cargarLibros();

            Toast.makeText(this,
                    !actual ? R.string.mostrar_solo_leidos : R.string.mostrar_todos,
                    Toast.LENGTH_SHORT).show();
        });

        Button btnAjustes = findViewById(R.id.btnAjustes);
        btnAjustes.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void mostrarDialogoAgregarLibro() {
        // Crear builder del diálogo
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.agregar_libro);

        // Inflar layout personalizado
        View view = getLayoutInflater().inflate(R.layout.dialog_agregar_libro, null);
        builder.setView(view);

        // Referencias a los campos
        EditText etTitulo = view.findViewById(R.id.etTitulo);
        EditText etAutor = view.findViewById(R.id.etAutor);
        EditText etGenero = view.findViewById(R.id.etGenero);
        EditText etValoracion = view.findViewById(R.id.etValoracion);
        CheckBox cbLeido = view.findViewById(R.id.cbLeido);

        // Botón Aceptar
        builder.setPositiveButton(R.string.agregar, (dialog, which) -> {
            String titulo = etTitulo.getText().toString().trim();
            String autor = etAutor.getText().toString().trim();
            String genero = etGenero.getText().toString().trim();
            int valoracion = 0;
            try {
                valoracion = Integer.parseInt(etValoracion.getText().toString().trim());
            } catch (NumberFormatException e) {
                valoracion = 0;
            }
            boolean leido = cbLeido.isChecked();

            // Crear libro
            Libro libro = new Libro(titulo, autor, genero, valoracion, leido);

            // Insertar en base de datos
            AppDatabase db = AppDatabase.getInstance(this);
            db.libroDao().insertLibro(libro);

            // Actualizar lista y RecyclerView respetando el filtro
            cargarLibros();

            Toast.makeText(this, R.string.libro_agregado, Toast.LENGTH_SHORT).show();
            mostrarNotificacion(getString(R.string.libro_agregado), getString(R.string.se_ha_agregado) + " " + libro.getTitulo());
        });

        // Botón Cancelar
        builder.setNegativeButton(R.string.cancelar, (dialog, which) -> dialog.dismiss());

        // Mostrar diálogo
        builder.show();
    }

    private void mostrarDialogoEditarLibro(Libro libro) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.editar_libro);

        // Inflar layout personalizado
        View view = getLayoutInflater().inflate(R.layout.dialog_agregar_libro, null);
        builder.setView(view);

        // Referencias a los campos
        EditText etTitulo = view.findViewById(R.id.etTitulo);
        EditText etAutor = view.findViewById(R.id.etAutor);
        EditText etGenero = view.findViewById(R.id.etGenero);
        EditText etValoracion = view.findViewById(R.id.etValoracion);
        CheckBox cbLeido = view.findViewById(R.id.cbLeido);

        // Rellenar con los datos actuales
        etTitulo.setText(libro.getTitulo());
        etAutor.setText(libro.getAutor());
        etGenero.setText(libro.getGenero());
        etValoracion.setText(String.valueOf(libro.getValoracion()));
        cbLeido.setChecked(libro.isLeido());

        // Botón Guardar
        builder.setPositiveButton(R.string.guardar, (dialog, which) -> {
            libro.setTitulo(etTitulo.getText().toString().trim());
            libro.setAutor(etAutor.getText().toString().trim());
            libro.setGenero(etGenero.getText().toString().trim());
            try {
                libro.setValoracion(Integer.parseInt(etValoracion.getText().toString().trim()));
            } catch (NumberFormatException e) {
                libro.setValoracion(0);
            }
            libro.setLeido(cbLeido.isChecked());

            // Actualizar en base de datos
            AppDatabase db = AppDatabase.getInstance(this);
            db.libroDao().updateLibro(libro);

            // Actualizar lista y RecyclerView respetando el filtro
            cargarLibros();

            Toast.makeText(this, R.string.libro_actualizado, Toast.LENGTH_SHORT).show();

            mostrarNotificacion(getString(R.string.libro_agregado), getString(R.string.se_ha_agregado) + " " + libro.getTitulo());

            if (libro.isLeido()) {
                mostrarNotificacion(getString(R.string.libro_leido), getString(R.string.marcado_leido) + " " + libro.getTitulo());
            }
        });

        // Botón Cancelar
        builder.setNegativeButton(R.string.cancelar, (dialog, which) -> dialog.dismiss());

        // Mostrar diálogo
        builder.show();
    }

    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence nombre = getString(R.string.libros);
            String descripcion = getString(R.string.noti_libro);
            int importancia = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel canal = new NotificationChannel(CANAL_ID, nombre, importancia);
            canal.setDescription(descripcion);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(canal);
            }
        }
    }

    private void mostrarNotificacion(String titulo, String mensaje) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CANAL_ID)
                .setSmallIcon(R.drawable.ic_libro) // icono en res/drawable
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISO_NOTIFICACIONES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permiso_noti_si, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.permiso_noti_no, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void cargarLibros() {

        AppDatabase db = AppDatabase.getInstance(this);

        listaLibros.clear();

        if (Preferencias.getSoloLeidos(this)) {

            for (Libro libro : db.libroDao().getAllLibros()) {
                if (libro.isLeido()) {
                    listaLibros.add(libro);
                }
            }

        } else {

            listaLibros.addAll(db.libroDao().getAllLibros());

        }

        adapter.notifyDataSetChanged();
    }
}