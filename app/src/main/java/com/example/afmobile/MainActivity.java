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
 * Responsável por obter a localização do usuário, exibir o endereço via Nominatim,
 * buscar locais próximos via Overpass API e navegar para as demais telas.
 */
public class MainActivity extends AppCompatActivity implements LugarAdapter.OnSalvarClickListener {

    private static final int REQUEST_LOCATION_PERMISSION = 100;

    // Views do layout
    private Toolbar toolbar;
    private Button btnLocalizacao;
    private TextView tvEndereco;
    private Spinner spinnerCategorias;
    private Button btnBuscar;
    private ProgressBar progressBar;
    private RecyclerView recyclerViewLugares;

    // Cliente de localização GPS
    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;

    // Coordenadas do usuário
    private double latitude = 0;
    private double longitude = 0;
    private boolean localizacaoObtida = false;

    // RecyclerView
    private LugarAdapter lugarAdapter;
    private List<LocalSalvo> listaLugares;

    // Cliente HTTP para Nominatim
    private final OkHttpClient httpClient = new OkHttpClient();

    // Categorias de busca e mapeamento para Overpass
    private final String[] categorias = {
            "Farmácia", "Hospital", "Escola", "Restaurante", "Praça", "Mercado"
    };
    private final String[] overpassKeys = {
            "amenity", "amenity", "amenity", "amenity", "leisure", "shop"
    };
    private final String[] overpassValues = {
            "pharmacy", "hospital", "school", "restaurant", "park", "supermarket"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialização das views
        toolbar = findViewById(R.id.toolbar);
        btnLocalizacao = findViewById(R.id.btnLocalizacao);
        tvEndereco = findViewById(R.id.tvEndereco);
        spinnerCategorias = findViewById(R.id.spinnerCategorias);
        btnBuscar = findViewById(R.id.btnBuscar);
        progressBar = findViewById(R.id.progressBar);
        recyclerViewLugares = findViewById(R.id.recyclerViewLugares);

        // Configura a Toolbar como ActionBar
        setSupportActionBar(toolbar);

        // Inicializa o cliente de localização
        fusedLocationClient = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(this);

        // Configura o Spinner com as categorias de busca
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categorias
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategorias.setAdapter(spinnerAdapter);

        // Configura o RecyclerView
        listaLugares = new ArrayList<>();
        lugarAdapter = new LugarAdapter(listaLugares, this);
        recyclerViewLugares.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewLugares.setAdapter(lugarAdapter);

        // Botão para obter localização
        btnLocalizacao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                solicitarPermissaoLocalizacao();
            }
        });

        // Botão para buscar lugares próximos
        btnBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buscarLugaresProximos();
            }
        });
    }

    /**
     * Solicita permissão de localização. Se já concedida, obtém a localização.
     */
    private void solicitarPermissaoLocalizacao() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            obterLocalizacao();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
        }
    }

    /**
     * Callback da solicitação de permissão.
     * Se negada, exibe diálogo com opção de ir às configurações.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obterLocalizacao();
            } else {
                mostrarDialogoPermissaoNegada();
            }
        }
    }

    /**
     * Exibe AlertDialog explicando a necessidade da permissão
     * e oferece botão para abrir as configurações do app.
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
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Obtém a última localização conhecida via FusedLocationProviderClient.
     * Em seguida, busca o endereço aproximado pela Nominatim API.
     */
    @SuppressWarnings("MissingPermission")
    private void obterLocalizacao() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        localizacaoObtida = true;
                        buscarEnderecoNominatim(latitude, longitude);
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Erro ao obter localização. Verifique se o GPS está ativado.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Toast.makeText(MainActivity.this,
                            "Erro ao obter localização: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Realiza geocodificação reversa usando a Nominatim API em thread separada.
     * Obtém o endereço a partir das coordenadas e exibe na tela.
     */
    private void buscarEnderecoNominatim(double lat, double lon) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = "https://nominatim.openstreetmap.org/reverse?format=json&lat="
                            + lat + "&lon=" + lon;

                    Request request = new Request.Builder()
                            .url(url)
                            .addHeader("User-Agent", "AFMobile/1.0")
                            .build();

                    Response response = httpClient.newCall(request).execute();

                    if (response.isSuccessful() && response.body() != null) {
                        String respostaJson = response.body().string();
                        JSONObject jsonObject = new JSONObject(respostaJson);
                        String endereco = jsonObject.getString("display_name");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvEndereco.setText(endereco);
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,
                                        "Erro ao obter endereço",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    if (response.body() != null) {
                        response.close();
                    }

                } catch (IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "Erro de conexão ao obter endereço: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
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

    /**
     * Busca locais próximos com base na categoria selecionada no Spinner.
     * Usa a Overpass API via OverpassHelper.
     */
    private void buscarLugaresProximos() {
        if (!localizacaoObtida) {
            Toast.makeText(this, "Obtenha sua localização primeiro", Toast.LENGTH_SHORT).show();
            return;
        }

        int indiceSelecionado = spinnerCategorias.getSelectedItemPosition();
        String key = overpassKeys[indiceSelecionado];
        String value = overpassValues[indiceSelecionado];

        progressBar.setVisibility(View.VISIBLE);

        OverpassHelper.buscarLugares(latitude, longitude, key, value,
                new OverpassHelper.OverpassCallback() {
                    @Override
                    public void onSucesso(List<LocalSalvo> lugares) {
                        progressBar.setVisibility(View.GONE);

                        if (lugares.isEmpty()) {
                            Toast.makeText(MainActivity.this,
                                    "Nenhum lugar encontrado",
                                    Toast.LENGTH_SHORT).show();
                        }

                        listaLugares.clear();
                        listaLugares.addAll(lugares);
                        lugarAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onErro(String mensagem) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, mensagem, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Infla o menu da Toolbar (botão "Meus Locais").
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Trata clique no menu. "Meus Locais" abre a ListaActivity.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_meus_locais) {
            Intent intent = new Intent(this, ListaActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback do adapter: abre CadastroActivity com os dados do local selecionado.
     */
    @Override
    public void onSalvarClick(LocalSalvo lugar) {
        Intent intent = new Intent(this, CadastroActivity.class);
        intent.putExtra("nome", lugar.getNome());
        intent.putExtra("tipo", lugar.getTipo());
        intent.putExtra("latitude", lugar.getLatitude());
        intent.putExtra("longitude", lugar.getLongitude());
        startActivity(intent);
    }
}
