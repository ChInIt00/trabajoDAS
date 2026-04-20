package com.example.trabajodas.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trabajodas.R;
import com.example.trabajodas.UserBooksWidget;
import com.example.trabajodas.adapter.LibroAdapter;
import com.example.trabajodas.model.Libro;
import com.example.trabajodas.receiver.AlarmReceiver;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LibroAdapter adapter;
    private List<Libro> listaLibros = new ArrayList<>();

    private static final String CANAL_ID = "libros_canal";

    private static final int PERMISO_NOTIFICACIONES = 1001;
    private String BASE_URL = "http://35.222.163.80:81/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        String idioma = Preferencias.getIdioma(this);
        Locale locale = new Locale(idioma);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        super.onCreate(savedInstanceState);

        boolean logged = getSharedPreferences("user", MODE_PRIVATE)
                .getBoolean("logged", false);

        if (!logged) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

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
        cargarLibros();

        String username = getSharedPreferences("user", MODE_PRIVATE)
                .getString("username", "guest");

        boolean desdeLogin = getIntent().getBooleanExtra("desdeLogin", false);

        if (desdeLogin) {
            Toast.makeText(this, R.string.bienvenido + username, Toast.LENGTH_SHORT).show();
        }

        // Inicializar RecyclerView
        recyclerView = findViewById(R.id.recyclerViewLibros);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Crear adapter
        adapter = new LibroAdapter(this, listaLibros);
        recyclerView.setAdapter(adapter);
        iniciarAlarma();

        adapter.setOnItemClickListener(new LibroAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Libro libro) {
                // Click normal → editar libro
                mostrarDialogoEditarLibro(libro);
            }

            @Override
            public void onItemLongClick(Libro libro) {

                Log.d("DELETE_ID", String.valueOf(libro.getId()));

                new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.eliminar_libro)
                        .setMessage(getString(R.string.deseas_eliminar_libro) + " " + libro.getTitulo())
                        .setPositiveButton(R.string.si, (dialog, which) -> {

                            new Thread(() -> {
                                try {
                                    URL url = new URL(BASE_URL + "delete.php");
                                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                                    conn.setRequestMethod("POST");
                                    conn.setRequestProperty("Content-Type", "application/json");
                                    conn.setDoOutput(true);

                                    String json = "{"
                                            + "\"id\":\"" + libro.getId() + "\""
                                            + "}";

                                    OutputStream os = conn.getOutputStream();
                                    os.write(json.getBytes("UTF-8"));
                                    os.close();

                                    int responseCode = conn.getResponseCode();
                                    Log.d("DELETE_CODE", "HTTP: " + responseCode);

                                    InputStream is;

                                    if (responseCode >= 200 && responseCode < 400) {
                                        is = conn.getInputStream();
                                    } else {
                                        is = conn.getErrorStream();
                                    }

                                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                                    StringBuilder sb = new StringBuilder();
                                    String line;

                                    while ((line = br.readLine()) != null) {
                                        sb.append(line);
                                    }

                                    Log.d("DELETE_RESPONSE", sb.toString());

                                    runOnUiThread(() -> {
                                        Toast.makeText(MainActivity.this,
                                                R.string.libro_eliminado,
                                                Toast.LENGTH_SHORT).show();

                                        mostrarNotificacion(
                                                getString(R.string.libro_eliminado),
                                                getString(R.string.se_ha_eliminado) + " " + libro.getTitulo()
                                        );

                                        cargarLibros();
                                        actualizarWidget();
                                    });

                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Log.e("DELETE_ERROR", e.toString());
                                }
                            }).start();

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
            int valoracion;
            try {
                valoracion = Integer.parseInt(etValoracion.getText().toString().trim());
            } catch (Exception e) {
                valoracion = 0;
            }
            final int valoracionFinal = valoracion;
            boolean leido = cbLeido.isChecked();

            String username = getSharedPreferences("user", MODE_PRIVATE)
                    .getString("username", null);

            if (username == null) {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }

            // Insertar en base de datos
            new Thread(() -> {
                try {
                    URL url = new URL(BASE_URL + "insertLibro.php");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    String json = "{"
                            + "\"titulo\":\"" + titulo + "\","
                            + "\"autor\":\"" + autor + "\","
                            + "\"genero\":\"" + genero + "\","
                            + "\"valoracion\":" + valoracionFinal + ","
                            + "\"leido\":" + (leido ? 1 : 0) + ","
                            + "\"username\":\"" + username + "\""
                            + "}";

                    OutputStream os = conn.getOutputStream();
                    os.write(json.getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );

                    String response = br.readLine();

                    runOnUiThread(() -> {

                        if (response != null && response.contains("true")) {
                            Toast.makeText(this, R.string.libro_agregado, Toast.LENGTH_SHORT).show();

                            mostrarNotificacion(
                                    getString(R.string.libro_agregado),
                                    getString(R.string.se_ha_agregado) + " " + titulo
                            );

                            cargarLibros();
                            actualizarWidget();
                        } else {
                            Toast.makeText(this, R.string.error_al_guardar, Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
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

            String titulo = etTitulo.getText().toString().trim();
            String autor = etAutor.getText().toString().trim();
            String genero = etGenero.getText().toString().trim();
            boolean leido = cbLeido.isChecked();

            //FIX lambda issue
            int valoracion;
            try {
                valoracion = Integer.parseInt(etValoracion.getText().toString().trim());
            } catch (NumberFormatException e) {
                valoracion = 0;
            }
            final int valoracionFinal = valoracion;

            Log.d("UPDATE_ID", String.valueOf(libro.getId()));
            new Thread(() -> {
                try {
                    URL url = new URL(BASE_URL + "updateLibro.php");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    String username = getSharedPreferences("user", MODE_PRIVATE)
                            .getString("username", "guest");

                    String json = "{"
                            + "\"id\":" + libro.getId() + ","
                            + "\"titulo\":\"" + titulo + "\","
                            + "\"autor\":\"" + autor + "\","
                            + "\"genero\":\"" + genero + "\","
                            + "\"valoracion\":" + valoracionFinal + ","
                            + "\"leido\":" + (leido ? 1 : 0) + ","
                            + "\"username\":\"" + username + "\""
                            + "}";

                    OutputStream os = conn.getOutputStream();
                    os.write(json.getBytes("UTF-8"));
                    os.close();

                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );

                    String response = br.readLine();

                    Log.d("UPDATE_RESPONSE", response);

                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.libro_actualizado, Toast.LENGTH_SHORT).show();

                        mostrarNotificacion(
                                getString(R.string.libro_actualizado),
                                getString(R.string.se_ha_agregado) + " " + titulo
                        );

                        cargarLibros();
                        actualizarWidget();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        });

        builder.setNegativeButton(R.string.cancelar, (dialog, which) -> dialog.dismiss());

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

        String username = getSharedPreferences("user", MODE_PRIVATE)
                .getString("username", "guest");

        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "getLibros.php?username=" + username);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                JSONArray array = new JSONArray(sb.toString());

                listaLibros.clear();

                boolean soloLeidos = Preferencias.getSoloLeidos(this);

                int leidos = 0;

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);

                    Libro libro = new Libro();

                    libro.setId(obj.getInt("id"));
                    libro.setTitulo(obj.getString("titulo"));
                    libro.setAutor(obj.getString("autor"));
                    libro.setGenero(obj.getString("genero"));
                    libro.setValoracion(obj.getInt("valoracion"));
                    libro.setLeido(obj.getInt("leido") == 1);

                    if (libro.isLeido()) {
                        leidos++;
                    }

                    if (!soloLeidos || libro.isLeido()) {
                        listaLibros.add(libro);
                    }
                }


                actualizarWidget();

                runOnUiThread(() -> adapter.notifyDataSetChanged());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void iniciarAlarma() {

        Intent intent = new Intent(this, AlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        long intervalo = 60000; // 1 minuto

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                intervalo,
                pendingIntent
        );
    }

    private void actualizarWidget() {

        String username = getSharedPreferences("user", MODE_PRIVATE)
                .getString("username", "guest");

        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "getLibros.php?username=" + username);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                JSONArray array = new JSONArray(sb.toString());

                final int[] leidos = {0};

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    if (obj.getInt("leido") == 1) {
                        leidos[0]++;
                    }
                }

                SharedPreferences prefs = getSharedPreferences("widget", MODE_PRIVATE);
                prefs.edit().putInt("libros_leidos", leidos[0]).apply();

                runOnUiThread(() -> {

                    AppWidgetManager manager = AppWidgetManager.getInstance(this);
                    ComponentName widget = new ComponentName(this, UserBooksWidget.class);

                    int[] ids = manager.getAppWidgetIds(widget);

                    for (int id : ids) {

                        RemoteViews views = new RemoteViews(getPackageName(),
                                R.layout.user_books_widget);

                        views.setTextViewText(R.id.txtUser,
                                getSharedPreferences("user", MODE_PRIVATE)
                                        .getString("username", "Invitado"));

                        views.setTextViewText(R.id.txtLibros,
                                R.string.libros_leidos + " " + leidos[0]);

                        manager.updateAppWidget(id, views);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}