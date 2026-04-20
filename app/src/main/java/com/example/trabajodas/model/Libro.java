package com.example.trabajodas.model;

public class Libro {

    private int id;

    private String titulo;
    private String autor;
    private String genero;
    private int valoracion;
    private boolean leido;

    public Libro() {}

    public Libro(String titulo, String autor, String genero, int valoracion, boolean leido) {
        this.titulo = titulo;
        this.autor = autor;
        this.genero = genero;
        this.valoracion = valoracion;
        this.leido = leido;
    }

    public Libro(int id, String titulo, String autor, String genero, int valoracion, boolean leido) {
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.genero = genero;
        this.valoracion = valoracion;
        this.leido = leido;
    }

    // GETTERS Y SETTERS
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public int getValoracion() { return valoracion; }
    public void setValoracion(int valoracion) { this.valoracion = valoracion; }

    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }
}