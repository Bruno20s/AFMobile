package com.example.afmobile;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.afmobile.adapter.LugarAdapter;
import com.example.afmobile.api.OverpassHelper;
import com.example.afmobile.model.LocalSalvo;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Activity principal do aplicativo AFMobile.
 * Responsável por:
 * - Obter a localização do usuário via GPS
 * - Exibir o endereço via Nominatim (geocodificação reversa)
 * - Buscar locais próximos por categoria via Overpass API
 * - Exibir resultados em RecyclerView com opção de salvar
 * - Navegação para ListaActivity (Meus Locais) e CadastroActivity (Salvar local)
 */
public class MainActivity extends AppCompatActivity implements LugarAdapter.OnSalvarClickListener {

    // Código de requisição para permissão de localização
    private static final int REQUEST_LOCATION_PERMISSION = 100;

    // Referências das views do layout
    private Toolbar toolbar;
    private Button btnLocalizacao;
    private TextView tvEndereco;
    private Spinner spinnerCategorias;
    private Button btnBuscar;
    private ProgressBar progressBar;
    private RecyclerView recyclerViewLugares;

    // Cliente de localização do Google Play Services
    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;

    // Coordenadas do usuário (armazenadas para uso na busca)
    private double latitude = 0;
    private double longitude = 0;
    private boolean localizacaoObtida = false;

    // Adapter e lista para o RecyclerView de resultados
    private LugarAdapter lugarAdapter;
    private List<LocalSalvo> listaLugares;

    // Cliente OkHttp para chamada à Nominatim API
    private final OkHttpClient httpClient = new OkHttpClient();

    // Array de categorias exibidas no Spinner
    private final String[] categorias = {
            "Farmácia", "Hospital", "Escola", "Restaurante", "Praça", "Mercado"
    };

    // Mapeamento de categorias para chaves da Overpass API
    private final String[] overpassKeys = {
            "amenity", "amenity", "amenity", "amenity", "leisure", "shop"
    };

    // Mapeamento de categorias para valores da Overpass API
    private final String[] overpassValues = {
            "pharmacy", "hospital", "school", "restaurant", "park", "supermarket"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Task 7.1: Inicialização das views e componentes ---

        // Inicializa as referências das views do layout
        toolbar = findViewById(R.id.toolbar);
        btnLocalizacao = findViewById(R.id.btnLocalizacao);
        tvEndereco = findViewById(R.id.tvEndereco);
        spinnerCategorias = findViewById(R.id.spinnerCategorias);
        btnBuscar = findViewById(R.id.btnBuscar);
        progressBar = findViewById(R.id.progressBar);
        recyclerViewLugares = findViewById(R.id.recyclerViewLugares);

        // --- Task 7.5: Configura a Toolbar como ActionBar ---
        setSupportActionBar(toolbar);

        // Inicializa o cliente de localização do Google Play Services
        fusedLocationClient = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(this);

        // --- Task 7.4: Configura o Spinner com as 6 categorias ---
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categorias
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategorias.setAdapter(spinnerAdapter);

        // Configura o RecyclerView com LinearLayoutManager e adapter vazio
        listaLugares = new ArrayList<>();
        lugarAdapter = new LugarAdapter(listaLugares, this);
        recyclerViewLugares.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewLugares.setAdapter(lugarAdapter);

        // --- Task 7.1: Listener do botão "Obter Minha Localização" ---
        // Solicita permissão de localização ao clicar
        btnLocalizacao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                solicitarPermissaoLocalizacao();
            }
        });

        // --- Task 7.4: Listener do botão "Buscar Lugares Próximos" ---
        btnBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buscarLugaresProximos();
            }
        });
    }

    // =====================================================================
    // TASK 7.1 / 7.2: Permissão de localização
    // =====================================================================

    /**
     * Solicita a permissão de localização em tempo de execução.
     * Se já concedida, obtém a localização diretamente.
     */
    private void solicitarPermissaoLocalizacao() {
        // Verifica se a permissão já foi concedida
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permissão já concedida, obtém a localização
            obterLocalizacao();
        } else {
            // Solicita a permissão ao usuário
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
        }
    }

    /**
     * Task 7.2: Callback de resultado da solicitação de permissão.
     * Se concedida, obtém a localização.
     * Se negada, exibe AlertDialog explicando a necessidade com opção de ir para Configurações.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, obtém a localização
                obterLocalizacao();
            } else {
                // Permissão negada: exibe AlertDialog com explicação e link para Settings
                mostrarDialogoPermissaoNegada();
            }
        }
    }

    /**
     * Task 7.2: Exibe um AlertDialog informando que a permissão é necessária.
     * Oferece um botão para abrir as configurações do app no dispositivo.
     */
    private void mostrarDialogoPermissaoNegada() {
        new AlertDialog.Builder(this)
                .setTitle("Permissão Necessária")
                .setMessage("A permissão de localização é necessária para obter sua posição " +
                        "e buscar locais próximos. Por favor, habilite a permissão nas " +
                        "configurações do aplicativo.")
                .setPositiveButton("Configurações", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Abre as configurações do aplicativo no dispositivo
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // =====================================================================
    // TASK 7.3: Obtenção de localização + Nominatim (geocodificação reversa)
    // =====================================================================

    /**
     * Obtém a última localização conhecida do dispositivo via FusedLocationProviderClient.
     * Após obter lat/lon, chama a Nominatim API para geocodificação reversa.
     */
    @SuppressWarnings("MissingPermission")
    private void obterLocalizacao() {
        // Usa getLastLocation() para obter a última posição conhecida
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Armazena as coordenadas como campos da classe
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        localizacaoObtida = true;

                        // Chama a Nominatim API em thread separada para obter o endereço
                        buscarEnderecoNominatim(latitude, longitude);
                    } else {
                        // Localização nula (GPS pode estar desativado)
                        Toast.makeText(MainActivity.this,
                                "Erro ao obter localização. Verifique se o GPS está ativado.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    // Falha ao obter localização
                    Toast.makeText(MainActivity.this,
                            "Erro ao obter localização: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Task 7.3: Realiza geocodificação reversa usando a Nominatim API do OpenStreetMap.
     * A chamada é feita em uma thread separada para não bloquear a main thread.
     * Usa OkHttp com User-Agent obrigatório conforme política da Nominatim.
     *
     * @param lat Latitude da posição do usuário
     * @param lon Longitude da posição do usuário
     */
    private void buscarEnderecoNominatim(double lat, double lon) {
        // Executa a chamada de rede em thread separada (obrigatório no Android)
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Monta a URL da Nominatim API para geocodificação reversa
                    String url = "https://nominatim.openstreetmap.org/reverse?format=json&lat="
                            + lat + "&lon=" + lon;

                    // Cria a requisição com User-Agent obrigatório (política da Nominatim)
                    Request request = new Request.Builder()
                            .url(url)
                            .addHeader("User-Agent", "AFMobile/1.0")
                            .build();

                    // Executa a requisição de forma síncrona (já estamos em thread separada)
                    Response response = httpClient.newCall(request).execute();

                    if (response.isSuccessful() && response.body() != null) {
                        // Obtém o corpo da resposta como String JSON
                        String respostaJson = response.body().string();

                        // Faz parsing do JSON para extrair o campo "display_name"
                        JSONObject jsonObject = new JSONObject(respostaJson);
                        String endereco = jsonObject.getString("display_name");

                        // Atualiza a UI na thread principal com o endereço obtido
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvEndereco.setText(endereco);
                            }
                        });
                    } else {
                        // Resposta não foi bem-sucedida
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,
                                        "Erro ao obter endereço",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    // Fecha o corpo da resposta para liberar recursos
                    if (response.body() != null) {
                        response.close();
                    }

                } catch (IOException e) {
                    // Erro de conexão/rede
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "Erro de conexão ao obter endereço: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    // Erro genérico (parsing, etc.)
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "Erro ao processar endereço: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    // =====================================================================
    // TASK 7.4: Busca de locais por categoria via Overpass API
    // =====================================================================

    /**
     * Realiza a busca de locais próximos com base na categoria selecionada no Spinner.
     * Valida se a localização já foi obtida antes de iniciar a busca.
     * Exibe ProgressBar durante a operação e trata todos os cenários de erro.
     */
    private void buscarLugaresProximos() {
        // Verifica se a localização já foi obtida
        if (!localizacaoObtida) {
            Toast.makeText(this, "Obtenha sua localização primeiro", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtém o índice da categoria selecionada no Spinner
        int indiceSelecionado = spinnerCategorias.getSelectedItemPosition();

        // Obtém a chave e valor correspondentes para a Overpass API
        String key = overpassKeys[indiceSelecionado];
        String value = overpassValues[indiceSelecionado];

        // Exibe o ProgressBar durante a busca
        progressBar.setVisibility(View.VISIBLE);

        // Chama o OverpassHelper para buscar locais (execução assíncrona interna)
        OverpassHelper.buscarLugares(latitude, longitude, key, value,
                new OverpassHelper.OverpassCallback() {
                    @Override
                    public void onSucesso(List<LocalSalvo> lugares) {
                        // Esconde o ProgressBar após receber a resposta
                        progressBar.setVisibility(View.GONE);

                        if (lugares.isEmpty()) {
                            // Nenhum resultado encontrado
                            Toast.makeText(MainActivity.this,
                                    "Nenhum lugar encontrado",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // Atualiza a lista do adapter com os novos resultados
                        listaLugares.clear();
                        listaLugares.addAll(lugares);
                        lugarAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onErro(String mensagem) {
                        // Esconde o ProgressBar em caso de erro
                        progressBar.setVisibility(View.GONE);

                        // Exibe mensagem de erro ao usuário
                        Toast.makeText(MainActivity.this,
                                mensagem,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // =====================================================================
    // TASK 7.5: Toolbar, Menu e Navegação
    // =====================================================================

    /**
     * Infla o menu principal na Toolbar (menu_main.xml).
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Trata o clique nos itens do menu da Toolbar.
     * "Meus Locais" abre a ListaActivity.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_meus_locais) {
            // Abre a ListaActivity para visualizar locais salvos
            Intent intent = new Intent(this, ListaActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Task 7.5: Callback do LugarAdapter quando o usuário clica em "Salvar".
     * Abre a CadastroActivity passando os dados do local via Intent extras.
     *
     * @param lugar O objeto LocalSalvo do item clicado
     */
    @Override
    public void onSalvarClick(LocalSalvo lugar) {
        // Cria Intent para abrir a CadastroActivity com os dados do local
        Intent intent = new Intent(this, CadastroActivity.class);
        intent.putExtra("nome", lugar.getNome());
        intent.putExtra("tipo", lugar.getTipo());
        intent.putExtra("latitude", lugar.getLatitude());
        intent.putExtra("longitude", lugar.getLongitude());
        startActivity(intent);
    }
}
