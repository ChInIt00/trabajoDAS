package com.example.trabajodas;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

public class UserBooksWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        Log.d("WIDGET", "UPDATE RECIBIDO");

        SharedPreferences prefs =
                context.getSharedPreferences("widget", Context.MODE_PRIVATE);

        String username = prefs.getString("username", "Invitado");
        int libros = prefs.getInt("libros_leidos", 0);

        for (int appWidgetId : appWidgetIds) {

            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.user_books_widget);

            views.setTextViewText(R.id.txtUser, username);
            views.setTextViewText(R.id.txtLibros, R.string.libros_leidos + " " + libros);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}