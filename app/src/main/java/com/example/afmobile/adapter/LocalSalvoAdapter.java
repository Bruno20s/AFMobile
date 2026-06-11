package com.example.afmobile.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.afmobile.R;
import com.example.afmobile.model.LocalSalvo;

import java.util.List;

/**
 * Adapter do RecyclerView para exibir os locais salvos pelo usuário na ListaActivity.
 * Utiliza MaterialCardView com nome, categoria, observação e coordenadas.
 * Suporta clique curto (edição) e toque longo (exclusão).
 */
public class LocalSalvoAdapter extends RecyclerView.Adapter<LocalSalvoAdapter.LocalSalvoViewHolder> {

    // Lista de locais salvos exibidos no RecyclerView
    private final List<LocalSalvo> listaLocais;

    // Listener para interações do usuário com os itens
    private final OnItemInteractionListener listener;

    /**
     * Interface de callback para interações com os itens da lista.
     * Implementada pela Activity que utiliza este adapter.
     */
    public interface OnItemInteractionListener {

        /**
         * Chamado quando o usuário toca brevemente em um card.
         * Deve abrir a EdicaoActivity com os dados do local.
         *
         * @param local O objeto LocalSalvo correspondente ao item clicado
         */
        void onItemClick(LocalSalvo local);

        /**
         * Chamado quando o usuário pressiona longamente em um card.
         * Deve exibir o AlertDialog de confirmação de exclusão.
         *
         * @param local    O objeto LocalSalvo correspondente ao item pressionado
         * @param position A posição do item na lista
         */
        void onItemLongClick(LocalSalvo local, int position);
    }

    /**
     * Construtor do adapter.
     *
     * @param listaLocais Lista de locais salvos a serem exibidos
     * @param listener    Listener para tratar cliques curtos e toques longos
     */
    public LocalSalvoAdapter(List<LocalSalvo> listaLocais, OnItemInteractionListener listener) {
        this.listaLocais = listaLocais;
        this.listener = listener;
    }

    /**
     * Cria o ViewHolder inflando o layout item_local_salvo.xml.
     */
    @NonNull
    @Override
    public LocalSalvoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla o layout do card de local salvo
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_local_salvo, parent, false);
        return new LocalSalvoViewHolder(view);
    }

    /**
     * Vincula os dados do local salvo ao ViewHolder na posição especificada.
     * Configura os listeners de clique curto e toque longo.
     */
    @Override
    public void onBindViewHolder(@NonNull LocalSalvoViewHolder holder, int position) {
        // Obtém o local salvo na posição atual
        LocalSalvo local = listaLocais.get(position);

        // Preenche os TextViews com os dados do local
        holder.tvNomeLocal.setText(local.getNome());
        holder.tvCategoriaLocal.setText(local.getCategoria());
        holder.tvObservacaoLocal.setText(local.getObservacao());
        holder.tvCoordenadasLocal.setText("Lat: " + local.getLatitude() + " | Lon: " + local.getLongitude());

        // Configura o clique curto para abrir a tela de edição
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(local);
            }
        });

        // Configura o toque longo para exibir o diálogo de exclusão
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemLongClick(local, holder.getAdapterPosition());
            }
            return true; // Retorna true para indicar que o evento foi consumido
        });
    }

    /**
     * Retorna a quantidade total de itens na lista.
     */
    @Override
    public int getItemCount() {
        return listaLocais.size();
    }

    /**
     * Remove um item da lista na posição especificada e notifica o adapter.
     * Utilizado após a confirmação de exclusão no AlertDialog.
     *
     * @param position Posição do item a ser removido
     */
    public void removerItem(int position) {
        listaLocais.remove(position);
        notifyItemRemoved(position);
    }

    /**
     * ViewHolder que mantém referências aos TextViews do layout item_local_salvo.xml.
     * Evita chamadas repetidas a findViewById durante a rolagem.
     */
    public static class LocalSalvoViewHolder extends RecyclerView.ViewHolder {

        // TextView para o nome do local
        TextView tvNomeLocal;

        // TextView para a categoria pessoal
        TextView tvCategoriaLocal;

        // TextView para a observação do usuário
        TextView tvObservacaoLocal;

        // TextView para as coordenadas (latitude e longitude)
        TextView tvCoordenadasLocal;

        /**
         * Construtor do ViewHolder. Inicializa as referências aos TextViews.
         *
         * @param itemView A view raiz do item (MaterialCardView)
         */
        public LocalSalvoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNomeLocal = itemView.findViewById(R.id.tvNomeLocal);
            tvCategoriaLocal = itemView.findViewById(R.id.tvCategoriaLocal);
            tvObservacaoLocal = itemView.findViewById(R.id.tvObservacaoLocal);
            tvCoordenadasLocal = itemView.findViewById(R.id.tvCoordenadasLocal);
        }
    }
}
