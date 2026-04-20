package com.example.trabajodas.activity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.trabajodas.R;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UserActivity extends AppCompatActivity {

    private Uri photoUri;
    private ImageView imgPerfil;

    private final String BASE_URL = "http://35.222.163.80:81/";

    private ActivityResultLauncher<Intent> cameraLauncher;
    private boolean fotoSubida = false;
    private boolean nuevaFoto = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        imgPerfil = findViewById(R.id.imgPerfil);
        Button btnFoto = findViewById(R.id.btnCambiarFoto);
        Button btnPassword = findViewById(R.id.btnCambiarPassword);
        Button btnGuardar = findViewById(R.id.btnGuardar);
        EditText etPassword = findViewById(R.id.etPassword);
        cargarFotoPerfil();

        // Cámara moderna
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        subirFoto();
                        imgPerfil.setImageURI(photoUri);
                    }
                }
        );

        btnFoto.setOnClickListener(v -> {
            Log.d("USER_ACTIVITY", "CLICK FOTO");
            nuevaFoto = true;
            abrirCamara();
        });

        btnPassword.setOnClickListener(v -> {
            String nuevaPass = etPassword.getText().toString().trim();

            if (nuevaPass.isEmpty()) {
                Toast.makeText(this, R.string.introduce_contraseña, Toast.LENGTH_SHORT).show();
                return;
            }

            cambiarPassword(nuevaPass);
        });

        btnGuardar.setOnClickListener(v -> {

            if (nuevaFoto && !fotoSubida) {
                Toast.makeText(this,
                        R.string.espera_subir_foto,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            finish();

        });
    }


    // 📸 ABRIR CÁMARA
    private void abrirCamara() {
        fotoSubida = false;
        Log.d("USER_ACTIVITY", "ENTRO EN abrirCamara");

        try {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        200);
                return;
            }

            File file = new File(getExternalFilesDir(null), "perfil.jpg");

            photoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    file
            );

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            cameraLauncher.launch(intent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void subirFoto() {

        String username = getSharedPreferences("user", MODE_PRIVATE)
                .getString("username", null);

        if (username == null) return;

        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "uploads/uploadFoto.php?username=" + username);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setUseCaches(false);

                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);

                // 🔥 Escalar manteniendo proporción
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();

                int maxSize = 800;

                float ratio = Math.min(
                        (float) maxSize / width,
                        (float) maxSize / height
                );

                int newWidth = Math.round(width * ratio);
                int newHeight = Math.round(height * ratio);

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

                Bitmap circularBitmap = getCircularBitmap(scaledBitmap);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                circularBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

                byte[] imageBytes = baos.toByteArray();

                OutputStream os = conn.getOutputStream();
                os.write(imageBytes);
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                Log.d("UPLOAD", "HTTP CODE: " + code);

                InputStream responseStream =
                        (code >= 400) ? conn.getErrorStream() : conn.getInputStream();

                BufferedReader br = new BufferedReader(new InputStreamReader(responseStream));

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                Log.d("UPLOAD", "RESPUESTA: " + sb.toString());

                runOnUiThread(() -> {
                    if (code == 200) {
                        fotoSubida = true;
                        cargarFotoPerfil();
                    } else {
                        Toast.makeText(this, "Error HTTP " + code, Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("UPLOAD_ERROR", "Fallo subida", e);
            }
        }).start();
    }

    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        float radius = size / 2f;

        // Dibujar círculo
        canvas.drawCircle(radius, radius, radius, paint);

        // Aplicar modo recorte
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        // Centrar la imagen
        int left = (bitmap.getWidth() - size) / 2;
        int top = (bitmap.getHeight() - size) / 2;

        canvas.drawBitmap(bitmap,
                new Rect(left, top, left + size, top + size),
                new Rect(0, 0, size, size),
                paint);

        return output;
    }

    private void cargarFotoPerfil() {

        String username = getSharedPreferences("user", MODE_PRIVATE)
                .getString("username", "guest");

        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "getUser.php?username=" + username);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                String response = br.readLine();

                JSONObject obj = new JSONObject(response);
                String foto = obj.optString("foto_perfil", null);

                runOnUiThread(() -> {
                    if (foto != null && !foto.isEmpty()) {
                        String fullUrl = BASE_URL + "uploads/uploads/" + foto;

                        Picasso.get()
                                .load(fullUrl)
                                .into(imgPerfil);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 🔑 CAMBIAR PASSWORD (CORREGIDO)
    private void cambiarPassword(String nuevaPass) {

        String username = getSharedPreferences("user", MODE_PRIVATE)
                .getString("username", "guest");

        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "updatePassword.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String json = "{"
                        + "\"username\":\"" + username + "\","
                        + "\"password\":\"" + nuevaPass + "\""
                        + "}";

                OutputStream os = conn.getOutputStream();
                os.write(json.getBytes("UTF-8"));
                os.flush();
                os.close();

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                String response = sb.toString();

                Log.d("PASSWORD_RAW", "RESPUESTA: " + response);

                runOnUiThread(() -> {
                    try {
                        JSONObject jsonObj = new JSONObject(response);
                        boolean success = jsonObj.getBoolean("success");

                        if (success) {
                            Toast.makeText(this, R.string.cambio_contraseña, Toast.LENGTH_SHORT).show();

                            // borrar sesión
                            getSharedPreferences("user", MODE_PRIVATE)
                                    .edit()
                                    .clear()
                                    .apply();

                            // ir a login y limpiar pila
                            Intent intent = new Intent(UserActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, R.string.error_cambio_contraseña, Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}