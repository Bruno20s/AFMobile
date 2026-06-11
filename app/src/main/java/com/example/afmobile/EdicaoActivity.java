package com.example.afmobile;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity responsável pela edição de um local salvo.
 * Permite ao usuário alterar a observação e a categoria pessoal do local.
 * Atualiza os dados no Firebase Firestore pelo documentId.
 */
public class EdicaoActivity extends AppCompatActivity {

    // Componentes de interface
    private TextView tvNome, tvTipo;
    private EditText etObservacao;
    private Spinner spinnerCategoriaPessoal;
    private Button btnSalvarAlteracoes;

    // Identificador do documento no Firestore
    private String documentId;

    // Lista de categorias pessoais disponíveis
    private final String[] categorias = {"Estudo", "Saúde", "Lazer", "Alimentação", "Compras", "Outros"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edicao);

        // Inicializar componentes da interface
        tvNome = findViewById(R.id.tvNome);
        tvTipo = findViewById(R.id.tvTipo);
        etObservacao = findViewById(R.id.etObservacao);
        spinnerCategoriaPessoal = findViewById(R.id.spinnerCategoriaPessoal);
        btnSalvarAlteracoes = findViewById(R.id.btnSalvarAlteracoes);

        // Recuperar dados enviados pela Intent
        documentId = getIntent().getStringExtra("documentId");
        String nome = getIntent().getStringExtra("nome");
        String tipo = getIntent().getStringExtra("tipo");
        String observacao = getIntent().getStringExtra("observacao");
        String categoria = getIntent().getStringExtra("categoria");

        // Exibir nome e tipo nos TextViews (não editáveis)
        tvNome.setText(nome);
        tvTipo.setText(tipo);

        // Preencher campo de observação com o valor atual
        etObservacao.setText(observacao);

        // Configurar Spinner de categorias pessoais com ArrayAdapter
        ArrayAdapter<String> adapterCategorias = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categorias
        );
        adapterCategorias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoriaPessoal.setAdapter(adapterCategorias);

        // Pré-selecionar a categoria salva no Spinner
        if (categoria != null) {
            for (int i = 0; i < categorias.length; i++) {
                if (categorias[i].equals(categoria)) {
                    spinnerCategoriaPessoal.setSelection(i);
                    break;
                }
            }
        }

        // Configurar ação do botão "Salvar Alterações"
        btnSalvarAlteracoes.setOnClickListener(v -> salvarAlteracoes());
    }

    /**
     * Valida os campos e atualiza o documento no Firestore.
     * Campos atualizados: observacao e categoria.
     */
    private void salvarAlteracoes() {
        // Obter texto da observação
        String novaObservacao = etObservacao.getText().toString().trim();

        // Validação: observação não pode estar vazia
        if (novaObservacao.isEmpty()) {
            Toast.makeText(this, "Preencha a observação", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obter categoria selecionada no Spinner
        String novaCategoria = spinnerCategoriaPessoal.getSelectedItem().toString();

        // Montar mapa com os campos a serem atualizados
        Map<String, Object> atualizacoes = new HashMap<>();
        atualizacoes.put("observacao", novaObservacao);
        atualizacoes.put("categoria", novaCategoria);

        // Atualizar documento no Firestore pela coleção "locais_salvos"
        FirebaseFirestore.getInstance()
                .collection("locais_salvos")
                .document(documentId)
                .update(atualizacoes)
                .addOnSuccessListener(aVoid -> {
                    // Sucesso: exibir mensagem e fechar a Activity
                    Toast.makeText(EdicaoActivity.this, "Local atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Falha: exibir mensagem de erro ao usuário
                    Toast.makeText(EdicaoActivity.this, "Erro ao atualizar. Verifique sua conexão.", Toast.LENGTH_SHORT).show();
                });
    }
}
