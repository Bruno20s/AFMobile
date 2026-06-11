package com.example.afmobile.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.afmobile.R;
import com.example.afmobile.model.LocalSalvo;

import java.util.List;

/**
 * Adapter do RecyclerView para exibir os resultados da busca de lugares próximos.
 * Cada item exibe o nome, tipo, coordenadas e um botão "Salvar".
 */
public class LugarAdapter extends RecyclerView.Adapter<LugarAdapter.LugarViewHolder> {

    // Lista de lugares retornados pela busca
    private final List<LocalSalvo> listaLugares;

    // Listener para o evento de clique no botão "Salvar"
    private final OnSalvarClickListener listener;

    /**
     * Interface de callback para o clique no botão "Salvar" de cada item.
     * A Activity que utiliza este adapter deve implementar esta interface.
     */
    public interface OnSalvarClickListener {
        /**
         * Chamado quando o usuário clica no botão "Salvar" de um lugar.
         *
         * @param lugar O objeto LocalSalvo correspondente ao item clicado
         */
        void onSalvarClick(LocalSalvo lugar);
    }

    /**
     * Construtor do adapter.
     *
     * @param listaLugares Lista de lugares a serem exibidos
     * @param listener     Listener para o evento de clique no botão "Salvar"
     */
    public LugarAdapter(List<LocalSalvo> listaLugares, OnSalvarClickListener listener) {
        this.listaLugares = listaLugares;
        this.listener = listener;
    }

    /**
     * Cria um novo ViewHolder inflando o layout do item de lugar.
     *
     * @param parent   ViewGroup pai (o RecyclerView)
     * @param viewType Tipo da view (não utilizado, pois há apenas um tipo)
     * @return Nova instância de LugarViewHolder
     */
    @NonNull
    @Override
    public LugarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla o layout do card de resultado de busca
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lugar, parent, false);
        return new LugarViewHolder(view);
    }

    /**
     * Vincula os dados de um lugar ao ViewHolder na posição especificada.
     *
     * @param holder   ViewHolder a ser atualizado
     * @param position Posição do item na lista
     */
    @Override
    public void onBindViewHolder(@NonNull LugarViewHolder holder, int position) {
        // Obtém o lugar na posição atual
        LocalSalvo lugar = listaLugares.get(position);

        // Define o nome do lugar
        holder.tvNomeLugar.setText(lugar.getNome());

        // Define o tipo/categoria do lugar
        holder.tvTipoLugar.setText(lugar.getTipo());

        // Formata e define as coordenadas (latitude e longitude)
        String coordenadas = String.format("Lat: %.6f | Lon: %.6f",
                lugar.getLatitude(), lugar.getLongitude());
        holder.tvCoordenadas.setText(coordenadas);

        // Configura o listener do botão "Salvar"
        holder.btnSalvar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSalvarClick(lugar);
            }
        });
    }

    /**
     * Retorna o número total de itens na lista.
     *
     * @return Quantidade de lugares na lista
     */
    @Override
    public int getItemCount() {
        return listaLugares != null ? listaLugares.size() : 0;
    }

    /**
     * ViewHolder que mantém as referências das views de cada item do RecyclerView.
     * Evita chamadas repetidas a findViewById, melhorando a performance.
     */
    public static class LugarViewHolder extends RecyclerView.ViewHolder {

        // TextView para exibir o nome do lugar
        final TextView tvNomeLugar;

        // TextView para exibir o tipo/categoria do lugar
        final TextView tvTipoLugar;

        // TextView para exibir as coordenadas do lugar
        final TextView tvCoordenadas;

        // Botão para salvar o lugar no Firebase
        final Button btnSalvar;

        /**
         * Construtor do ViewHolder que inicializa as referências das views.
         *
         * @param itemView A view raiz do item inflado
         */
        public LugarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNomeLugar = itemView.findViewById(R.id.tvNomeLugar);
            tvTipoLugar = itemView.findViewById(R.id.tvTipoLugar);
            tvCoordenadas = itemView.findViewById(R.id.tvCoordenadas);
            btnSalvar = itemView.findViewById(R.id.btnSalvar);
        }
    }
}
