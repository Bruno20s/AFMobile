package com.example.afmobile.api;

import android.os.Handler;
import android.os.Looper;

import com.example.afmobile.model.LocalSalvo;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Classe auxiliar para consultas à Overpass API do OpenStreetMap.
 * Constrói a query Overpass, executa a requisição via OkHttp de forma assíncrona (POST),
 * faz parsing da resposta JSON com Gson e retorna os resultados via callback na thread principal.
 */
public class OverpassHelper {

    // URL base da Overpass API para consultas
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";

    // Raio de busca em metros ao redor da localização do usuário
    private static final int RAIO_BUSCA = 1500;

    // Cliente OkHttp reutilizável com timeout aumentado para evitar erro 504
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    // Instância do Gson para parsing de JSON
    private static final Gson gson = new Gson();

    // Handler para postar resultados na thread principal (UI)
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Interface de callback para comunicar resultados da busca.
     * Permite receber a lista de locais encontrados ou uma mensagem de erro.
     */
    public interface OverpassCallback {

        /**
         * Chamado quando a busca retorna resultados com sucesso.
         *
         * @param lugares Lista de objetos LocalSalvo com os locais encontrados.
         */
        void onSucesso(List<LocalSalvo> lugares);

        /**
         * Chamado quando ocorre um erro durante a busca.
         *
         * @param mensagem Mensagem descritiva do erro ocorrido.
         */
        void onErro(String mensagem);
    }

    /**
     * Busca locais próximos à coordenada fornecida utilizando a Overpass API.
     * A requisição é executada de forma assíncrona via OkHttp enqueue (não bloqueia a thread principal).
     * Os resultados são entregues via callback na thread principal usando Handler.
     *
     * @param lat      Latitude da posição do usuário
     * @param lon      Longitude da posição do usuário
     * @param key      Chave da tag OpenStreetMap (ex: "amenity", "shop", "leisure")
     * @param value    Valor da tag OpenStreetMap (ex: "pharmacy", "hospital", "park")
     * @param callback Interface de callback para receber resultados ou erros
     */
    public static void buscarLugares(double lat, double lon, String key, String value,
                                     OverpassCallback callback) {
        // Constrói a query Overpass com timeout de 25 segundos:
        // [out:json][timeout:25];node["KEY"="VALUE"](around:1500,LAT,LON);out body;
        String query = "[out:json][timeout:25];node[\"" + key + "\"=\"" + value + "\"](around:"
                + RAIO_BUSCA + "," + lat + "," + lon + ");out body;";

        // Cria o corpo da requisição POST com a query como parâmetro "data"
        RequestBody body = new FormBody.Builder()
                .add("data", query)
                .build();

        // Cria a requisição HTTP POST para a Overpass API com User-Agent obrigatório
        Request request = new Request.Builder()
                .url(OVERPASS_URL)
                .post(body)
                .addHeader("User-Agent", "AFMobile/1.0")
                .build();

        // Executa a requisição de forma assíncrona usando OkHttp enqueue (thread separada)
        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                // Em caso de falha na conexão, notifica o callback na thread principal
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onErro("Erro de conexão: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    // Verifica se a resposta HTTP foi bem-sucedida
                    if (!response.isSuccessful()) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onErro("Erro na resposta: código " + response.code());
                            }
                        });
                        return;
                    }

                    // Obtém o corpo da resposta como String JSON
                    String respostaJson = response.body().string();

                    // Faz parsing do JSON e extrai a lista de locais
                    List<LocalSalvo> lugares = parsearResposta(respostaJson, value);

                    // Entrega os resultados na thread principal via Handler
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSucesso(lugares);
                        }
                    });

                } catch (Exception e) {
                    // Em caso de erro no parsing, notifica o callback na thread principal
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onErro("Erro ao processar resposta: " + e.getMessage());
                        }
                    });
                } finally {
                    // Fecha o corpo da resposta para liberar recursos
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    /**
     * Faz parsing da resposta JSON da Overpass API e extrai os dados dos locais.
     * Cada elemento retornado é convertido em um objeto LocalSalvo com nome, tipo, latitude e longitude.
     * Se o elemento não possuir a tag "name", utiliza "Sem nome" como valor padrão.
     *
     * @param json  String JSON retornada pela Overpass API
     * @param tipo  Tipo/categoria do local buscado (ex: "pharmacy", "hospital")
     * @return Lista de objetos LocalSalvo com os dados dos locais encontrados
     */
    private static List<LocalSalvo> parsearResposta(String json, String tipo) {
        List<LocalSalvo> lugares = new ArrayList<>();

        // Converte a string JSON em objeto JsonObject
        JsonObject resposta = gson.fromJson(json, JsonObject.class);

        // Verifica se a resposta contém o array "elements"
        if (resposta == null || !resposta.has("elements")) {
            return lugares;
        }

        // Obtém o array de elementos (nós encontrados pela query)
        JsonArray elementos = resposta.getAsJsonArray("elements");

        // Itera sobre cada elemento do array de resultados
        for (JsonElement elemento : elementos) {
            JsonObject obj = elemento.getAsJsonObject();

            // Extrai latitude e longitude do elemento
            double latitude = obj.get("lat").getAsDouble();
            double longitude = obj.get("lon").getAsDouble();

            // Extrai o nome do local a partir das tags (usa "Sem nome" se ausente)
            String nome = "Sem nome";
            if (obj.has("tags")) {
                JsonObject tags = obj.getAsJsonObject("tags");
                if (tags.has("name")) {
                    nome = tags.get("name").getAsString();
                }
            }

            // Cria objeto LocalSalvo com os dados extraídos
            LocalSalvo local = new LocalSalvo();
            local.setNome(nome);
            local.setTipo(tipo);
            local.setLatitude(latitude);
            local.setLongitude(longitude);

            // Adiciona o local à lista de resultados
            lugares.add(local);
        }

        return lugares;
    }
}
