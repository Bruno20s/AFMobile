package com.example.afmobile.model;

/**
 * Classe POJO que representa um local salvo pelo usuário.
 * Utilizada para persistência no Firebase Firestore.
 * Campos: id, nome, tipo, latitude, longitude, observacao, categoria, dataSalvo.
 */
public class LocalSalvo {

    // Identificador único do documento no Firestore
    private String id;

    // Nome do local (obtido da Overpass API)
    private String nome;

    // Tipo/categoria de busca do local (ex: Farmácia, Hospital)
    private String tipo;

    // Latitude do local
    private double latitude;

    // Longitude do local
    private double longitude;

    // Observação pessoal do usuário
    private String observacao;

    // Categoria pessoal escolhida pelo usuário (ex: Estudo, Saúde, Lazer)
    private String categoria;

    // Data e hora em que o local foi salvo
    private String dataSalvo;

    /**
     * Construtor vazio exigido pelo Firebase Firestore para desserialização.
     */
    public LocalSalvo() {
    }

    /**
     * Construtor completo com todos os campos.
     *
     * @param id         Identificador do documento no Firestore
     * @param nome       Nome do local
     * @param tipo       Tipo/categoria de busca do local
     * @param latitude   Latitude do local
     * @param longitude  Longitude do local
     * @param observacao Observação pessoal do usuário
     * @param categoria  Categoria pessoal escolhida
     * @param dataSalvo  Data e hora do salvamento
     */
    public LocalSalvo(String id, String nome, String tipo, double latitude, double longitude,
                      String observacao, String categoria, String dataSalvo) {
        this.id = id;
        this.nome = nome;
        this.tipo = tipo;
        this.latitude = latitude;
        this.longitude = longitude;
        this.observacao = observacao;
        this.categoria = categoria;
        this.dataSalvo = dataSalvo;
    }

    // --- Getters e Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDataSalvo() {
        return dataSalvo;
    }

    public void setDataSalvo(String dataSalvo) {
        this.dataSalvo = dataSalvo;
    }
}
