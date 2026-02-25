package com.example.trabajodas.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.trabajodas.model.Libro;

import java.util.List;

@Dao
public interface LibroDao {

    // Obtener todos los libros
    @Query("SELECT * FROM Libro")
    List<Libro> getAllLibros();

    // Insertar un libro
    @Insert
    void insertLibro(Libro libro);

    // Actualizar un libro
    @Update
    void updateLibro(Libro libro);
}