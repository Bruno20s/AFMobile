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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Activity responsável pelo cadastro de um local no Firebase Firestore.
 * Recebe os dados do local selecionado via Intent e permite ao usuário
 * adicionar uma observação pessoal e uma categoria antes de salvar.
 */
public class CadastroActivity extends AppCompatActivity {

    // TextViews para exibir dados do local (não editáveis)
    private TextView tvNome, tvTipo, tvLatitude, tvLongitude;

    // Campo de texto para observação pessoal
    private EditText etObservacao;

    // Spinner para seleção de categoria pessoal
    private Spinner spinnerCategoriaPessoal;

    // Botão para salvar no Firebase
    private Button btnSalvarFirebase;

    // Instância do Firebase Firestore
    private FirebaseFirestore db;

    // Dados recebidos via Intent
    private String nome, tipo;
    private double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        // Inicializar instância do Firestore
        db = FirebaseFirestore.getInstance();

        // Vincular componentes do layout aos campos da Activity
        tvNome = findViewById(R.id.tvNome);
        tvTipo = findViewById(R.id.tvTipo);
        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        etObservacao = findViewById(R.id.etObservacao);
        spinnerCategoriaPessoal = findViewById(R.id.spinnerCategoriaPessoal);
        btnSalvarFirebase = findViewById(R.id.btnSalvarFirebase);

        // Receber dados enviados pela Activity anterior via Intent
        nome = getIntent().getStringExtra("nome");
        tipo = getIntent().getStringExtra("tipo");
        latitude = getIntent().getDoubleExtra("latitude", 0.0);
        longitude = getIntent().getDoubleExtra("longitude", 0.0);

        // Exibir dados recebidos nos TextViews
        tvNome.setText("Nome: " + nome);
        tvTipo.setText("Tipo: " + tipo);
        tvLatitude.setText("Latitude: " + latitude);
        tvLongitude.setText("Longitude: " + longitude);

        // Configurar Spinner com categorias pessoais
        configurarSpinnerCategorias();

        // Configurar ação do botão salvar
        btnSalvarFirebase.setOnClickListener(v -> salvarNoFirebase());
    }

    /**
     * Configura o Spinner com as categorias pessoais disponíveis.
     * Categorias: Estudo, Saúde, Lazer, Alimentação, Compras, Outros.
     */
    private void configurarSpinnerCategorias() {
        // Array de categorias pessoais
        String[] categorias = {"Estudo", "Saúde", "Lazer", "Alimentação", "Compras", "Outros"};

        // Criar adapter para o Spinner com layout padrão do Android
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categorias
        );

        // Definir layout do dropdown
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Associar adapter ao Spinner
        spinnerCategoriaPessoal.setAdapter(adapter);
    }

    /**
     * Valida os campos e salva o local no Firebase Firestore.
     * Coleção utilizada: "locais_salvos".
     * Campos salvos: nome, tipo, latitude, longitude, observacao, categoria, dataSalvo.
     */
    private void salvarNoFirebase() {
        // Obter texto da observação
        String observacao = etObservacao.getText().toString().trim();

        // Validação: observação não pode estar vazia
        if (observacao.isEmpty()) {
            Toast.makeText(this, "Preencha a observação", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obter categoria selecionada no Spinner
        String categoria = spinnerCategoriaPessoal.getSelectedItem().toString();

        // Obter data e hora atual formatada
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String dataSalvo = sdf.format(new Date());

        // Montar mapa com os dados a serem salvos no Firestore
        Map<String, Object> local = new HashMap<>();
        local.put("nome", nome);
        local.put("tipo", tipo);
        local.put("latitude", latitude);
        local.put("longitude", longitude);
        local.put("observacao", observacao);
        local.put("categoria", categoria);
        local.put("dataSalvo", dataSalvo);

        // Salvar na coleção "locais_salvos" do Firestore
        db.collection("locais_salvos")
                .add(local)
                .addOnSuccessListener(documentReference -> {
                    // Sucesso ao salvar: exibir mensagem e fechar Activity
                    Toast.makeText(CadastroActivity.this, "Local salvo com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Falha ao salvar: exibir mensagem de erro
                    Toast.makeText(CadastroActivity.this, "Erro ao salvar. Verifique sua conexão.", Toast.LENGTH_SHORT).show();
                });
    }
}
