package com.example.trabajodas.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.example.trabajodas.model.Libro;

@Database(entities = {Libro.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;

    public abstract LibroDao libroDao();

    // Método para obtener instancia singleton
    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "libros_db"
                    ).allowMainThreadQueries() // Para simplificar el ejemplo
                    .build();
        }
        return INSTANCE;
    }
}