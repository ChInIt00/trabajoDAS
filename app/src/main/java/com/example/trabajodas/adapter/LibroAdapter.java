package com.example.trabajodas.adapter;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trabajodas.R;
import com.example.trabajodas.model.Libro;

import java.util.List;

public class LibroAdapter extends RecyclerView.Adapter<LibroAdapter.LibroViewHolder> {

    private List<Libro> listaLibros;
    private OnItemClickListener listener;
    private Activity activity;

    // Listener para clicks
    public interface OnItemClickListener {
        void onItemClick(Libro libro);
        void onItemLongClick(Libro libro); //borrar al tener click largo
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public LibroAdapter(Activity activity, List<Libro> listaLibros) {
        this.activity = activity;
        this.listaLibros = listaLibros;
    }

    // Crea la vista (CardView)
    @NonNull
    @Override
    public LibroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_libro, parent, false);

        return new LibroViewHolder(view);
    }

    // Asigna los datos a la vista
    @Override
    public void onBindViewHolder(@NonNull LibroViewHolder holder, int position) {

        Libro libro = listaLibros.get(position);

        holder.tvTitulo.setText(libro.getTitulo());

        holder.tvAutor.setText(activity.getString(R.string.autor, libro.getAutor()));
        holder.tvGenero.setText(activity.getString(R.string.genero, libro.getGenero()));
        holder.tvValoracion.setText(activity.getString(R.string.valoracion, libro.getValoracion()));

        holder.tvEstado.setText(
                libro.isLeido() ? activity.getString(R.string.leido_si)
                        : activity.getString(R.string.leido_no)
        );

        // Click normal = editar
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && position != RecyclerView.NO_POSITION) {
                listener.onItemClick(libro);
            }
        });

        // Click largo = borrar
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null && position != RecyclerView.NO_POSITION) {
                listener.onItemLongClick(libro);
                return true; // importante devolver true para indicar que el evento se consumió
            }
            return false;
        });

        //botón buscar en Google
        holder.btnBuscarGoogle.setOnClickListener(v -> {
            String query = libro.getTitulo() + " " + libro.getAutor();
            Uri uri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(query));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);

            // Usamos activity en lugar de v.getContext()
            try {
                activity.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(activity, R.string.no_aplicacion_abrir, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaLibros.size();
    }

    // Clase ViewHolder
    public class LibroViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitulo, tvAutor, tvGenero, tvValoracion, tvEstado;
        Button btnBuscarGoogle;

        public LibroViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvAutor = itemView.findViewById(R.id.tvAutor);
            tvGenero = itemView.findViewById(R.id.tvGenero);
            tvValoracion = itemView.findViewById(R.id.tvValoracion);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            btnBuscarGoogle = itemView.findViewById(R.id.btnBuscarGoogle);
        }
    }
}