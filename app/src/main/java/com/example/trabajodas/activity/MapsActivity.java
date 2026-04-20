package com.example.trabajodas.activity;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.example.trabajodas.R;
import com.example.trabajodas.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    private FusedLocationProviderClient fusedLocationClient;

    private static final int PERMISSION_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inicializar ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Cargar mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // Permiso de ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_LOCATION);
            return;
        }

        mMap.setMyLocationEnabled(true);

        // Obtener ubicación actual
        fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {
            if (location != null) {

                double lat = location.getLatitude();
                double lng = location.getLongitude();

                Log.d("MAPS", "LAT: " + lat + " LNG: " + lng);

                LatLng userLocation = new LatLng(lat, lng);

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));

                buscarLibrerias(lat, lng);
            }
        });
    }

    private void buscarLibrerias(double lat, double lng) {

        new Thread(() -> {
            try {

                URL url = new URL(
                        "https://places.googleapis.com/v1/places:searchNearby?key=AIzaSyCXUXvZ0M13HpTUG-67tNnbf0N8I7h9SWk"
                );

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                conn.setRequestProperty(
                        "X-Goog-FieldMask",
                        "places.displayName,places.location,places.id"
                );

                conn.setDoOutput(true);

                String body =
                        "{"
                                + "\"includedTypes\": [\"book_store\"],"
                                + "\"locationRestriction\": {"
                                + "\"circle\": {"
                                + "\"center\": {"
                                + "\"latitude\": " + lat + ","
                                + "\"longitude\": " + lng + "},"
                                + "\"radius\": 2000"
                                + "}"
                                + "}"
                                + "}";

                OutputStream os = conn.getOutputStream();
                os.write(body.getBytes("UTF-8"));
                os.close();

                int code = conn.getResponseCode();
                Log.d("PLACES", "HTTP CODE: " + code);

                InputStream is = (code >= 400)
                        ? conn.getErrorStream()
                        : conn.getInputStream();

                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                String response = sb.toString();
                Log.d("PLACES_RAW", response);

                JSONObject json = new JSONObject(response);

                if (!json.has("places")) {
                    Log.e("PLACES_ERROR", "No hay 'places' en respuesta");
                    return;
                }

                JSONArray places = json.getJSONArray("places");

                runOnUiThread(() -> {
                    try {
                        for (int i = 0; i < places.length(); i++) {

                            JSONObject place = places.getJSONObject(i);

                            String name = place
                                    .getJSONObject("displayName")
                                    .getString("text");

                            JSONObject location = place.getJSONObject("location");

                            double pLat = location.getDouble("latitude");
                            double pLng = location.getDouble("longitude");

                            LatLng pos = new LatLng(pLat, pLng);

                            mMap.addMarker(new MarkerOptions()
                                    .position(pos)
                                    .title(name));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("PLACES_ERROR", "Fallo API NEW", e);
            }
        }).start();
    }

    // Gestionar resultado del permiso
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onMapReady(mMap);
            }
        }
    }
}