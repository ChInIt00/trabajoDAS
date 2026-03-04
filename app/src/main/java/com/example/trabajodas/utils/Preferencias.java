package com.example.trabajodas.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferencias {

    private static final String NOMBRE_PREF = "configuracion";
    private static final String KEY_SOLO_LEIDOS = "solo_leidos";
    private static final String KEY_IDIOMA = "idioma";

    public static void setSoloLeidos(Context context, boolean valor) {
        SharedPreferences prefs = context.getSharedPreferences(NOMBRE_PREF, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SOLO_LEIDOS, valor).apply();
    }

    public static boolean getSoloLeidos(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(NOMBRE_PREF, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SOLO_LEIDOS, false); // false por defecto
    }

    public static void setIdioma(Context context, String idioma) {
        SharedPreferences prefs = context.getSharedPreferences(NOMBRE_PREF, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_IDIOMA, idioma).apply();
    }

    public static String getIdioma(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(NOMBRE_PREF, Context.MODE_PRIVATE);
        return prefs.getString(KEY_IDIOMA, "es");
    }
}