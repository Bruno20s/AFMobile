package com.example.afmobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.afmobile.adapter.LocalSalvoAdapter;
import com.example.afmobile.model.LocalSalvo;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity responsável por exibir a lista de locais salvos pelo usuário.
 * Carrega os dados da coleção "locais_salvos" do Firestore e exibe em um RecyclerView.
 * Suporta clique curto (abrir edição) e toque longo (excluir local).
 */
public class ListaActivity extends AppCompatActivity implements LocalSalvoAdapter.OnItemInteractionListener {

    // Instância do Firestore para acesso ao banco de dados
    private FirebaseFirestore db;

    // RecyclerView para exibir os locais salvos
    private RecyclerView recyclerViewLocaisSalvos;

    // Adapter do RecyclerView
    private LocalSalvoAdapter adapter;

    // Lista de locais salvos carregados do Firestore
    private List<LocalSalvo> listaLocais;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);

        // Inicializa a instância do Firestore
        db = FirebaseFirestore.getInstance();

        // Inicializa a lista de locais
        listaLocais = new ArrayList<>();

        // Configura o RecyclerView com LinearLayoutManager
        recyclerViewLocaisSalvos = findViewById(R.id.recyclerViewLocaisSalvos);
        recyclerViewLocaisSalvos.setLayoutManager(new LinearLayoutManager(this));

        // Cria o adapter passando a lista e o listener (esta Activity)
        adapter = new LocalSalvoAdapter(listaLocais, this);
        recyclerViewLocaisSalvos.setAdapter(adapter);

        // Carrega os locais salvos do Firestore
        carregarLocaisSalvos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarrega os dados ao retornar da EdicaoActivity para refletir alterações
        carregarLocaisSalvos();
    }

    /**
     * Carrega todos os documentos da coleção "locais_salvos" do Firestore.
     * Atualiza a lista e notifica o adapter para exibir os dados.
     */
    private void carregarLocaisSalvos() {
        db.collection("locais_salvos")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Limpa a lista antes de adicionar os novos dados
                    listaLocais.clear();

                    // Itera sobre cada documento retornado
                    for (QueryDocumentSnapshot documento : queryDocumentSnapshots) {
                        // Converte o documento para o objeto LocalSalvo
                        LocalSalvo local = documento.toObject(LocalSalvo.class);

                        // Define o ID do documento no objeto
                        local.setId(documento.getId());

                        // Adiciona o local à lista
                        listaLocais.add(local);
                    }

                    // Notifica o adapter que os dados foram atualizados
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    // Exibe mensagem de erro ao usuário
                    Toast.makeText(ListaActivity.this, "Erro ao carregar locais", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Chamado quando o usuário toca brevemente em um card.
     * Abre a EdicaoActivity passando todos os campos do local + ID do documento.
     *
     * @param local O objeto LocalSalvo correspondente ao item clicado
     */
    @Override
    public void onItemClick(LocalSalvo local) {
        // Cria o Intent para abrir a tela de edição
        Intent intent = new Intent(ListaActivity.this, EdicaoActivity.class);

        // Passa o ID do documento para identificação no Firestore
        intent.putExtra("documentId", local.getId());

        // Passa os dados do local para preenchimento dos campos
        intent.putExtra("nome", local.getNome());
        intent.putExtra("tipo", local.getTipo());
        intent.putExtra("latitude", local.getLatitude());
        intent.putExtra("longitude", local.getLongitude());
        intent.putExtra("observacao", local.getObservacao());
        intent.putExtra("categoria", local.getCategoria());

        // Inicia a EdicaoActivity
        startActivity(intent);
    }

    /**
     * Chamado quando o usuário pressiona longamente em um card.
     * Exibe um AlertDialog de confirmação para exclusão do local.
     *
     * @param local    O objeto LocalSalvo correspondente ao item pressionado
     * @param position A posição do item na lista
     */
    @Override
    public void onItemLongClick(LocalSalvo local, int position) {
        // Cria e exibe o AlertDialog de confirmação de exclusão
        new AlertDialog.Builder(this)
                .setTitle("Excluir local")
                .setMessage("Deseja excluir este local?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    // Exclui o documento do Firestore pelo ID
                    db.collection("locais_salvos")
                            .document(local.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Remove o item da lista e notifica o adapter
                                adapter.removerItem(position);
                            })
                            .addOnFailureListener(e -> {
                                // Exibe mensagem de erro ao usuário
                                Toast.makeText(ListaActivity.this, "Erro ao excluir local", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Não", (dialog, which) -> {
                    // Fecha o diálogo sem realizar nenhuma ação
                    dialog.dismiss();
                })
                .show();
    }
}
