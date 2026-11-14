package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OllamaService {

    private static final String URL = "http://127.0.0.1:11434";
    private static final String MODELO_PRANCHA =
            "hf.co/tardellirs/aac-board-generator-770m-ptbr-GGUF:Q4_K_M";
    private static final String MODELO_EMBEDDING =
            "hf.co/tardellirs/aac-pictogram-retriever-ptbr-GGUF:Q4_K_M";

    private static final List<String> PEDIDOS = List.of(
            "monta uma prancha sobre \"%s\"",
            "monta uma prancha sobre o tema %s",
            "monta uma prancha sobre %s"
    );

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, double[]> cacheDeEmbeddings = new ConcurrentHashMap<>();

    public void verificar() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(URL + "/api/tags")).GET().build();
        JsonNode instalados = mapper.readTree(enviar(request).body()).path("models");

        for (String modelo : List.of(MODELO_PRANCHA, MODELO_EMBEDDING)) {
            boolean encontrado = false;
            for (JsonNode instalado : instalados) {
                if (modelo.equals(instalado.path("name").asText())) encontrado = true;
            }
            if (!encontrado) {
                throw new IllegalStateException("Falta o modelo " + modelo
                        + " no Ollama. Rode no terminal: ollama pull " + modelo);
            }
        }
    }

    public int quantidadeDeTentativas() {
        return PEDIDOS.size();
    }

    public List<ConceitoPrancha> gerarPrancha(String tema, int tentativa) throws Exception {
        return ConceitoPrancha.daResposta(gerar(PEDIDOS.get(tentativa).formatted(tema)));
    }

    public List<double[]> gerarEmbeddings(List<String> textos) throws Exception {
        List<String> faltando = new ArrayList<>();
        for (String texto : textos) {
            if (!cacheDeEmbeddings.containsKey(texto) && !faltando.contains(texto)) faltando.add(texto);
        }

        if (!faltando.isEmpty()) {
            try {
                guardarEmbeddings(faltando);
            } catch (IllegalStateException erroNoLote) {
                for (String texto : faltando) {
                    try {
                        guardarEmbeddings(List.of(texto));
                    } catch (IllegalStateException erroNoTexto) {
                        if (texto.equals(textos.get(0))) throw erroNoTexto;
                    }
                }
            }
        }

        List<double[]> vetores = new ArrayList<>();
        for (String texto : textos) vetores.add(cacheDeEmbeddings.get(texto));
        return vetores;
    }

    private void guardarEmbeddings(List<String> textos) throws Exception {
        JsonNode resposta = post("/api/embed", Map.of("model", MODELO_EMBEDDING, "input", textos));
        OllamaEmbed embed = mapper.treeToValue(resposta, OllamaEmbed.class);
        if (embed.embeddings == null || embed.embeddings.size() != textos.size()) {
            throw new IllegalStateException("O Ollama não devolveu os embeddings esperados.");
        }
        for (int i = 0; i < textos.size(); i++) {
            double[] vetor = embed.embeddings.get(i).stream().mapToDouble(Double::doubleValue).toArray();
            cacheDeEmbeddings.put(textos.get(i), vetor);
        }
    }

    private String gerar(String pedido) throws Exception {
        String prompt = "<start_of_turn>user\n"
                + "Você monta pranchas de CAA (pictogramas, pt-BR). Para o PEDIDO, liste ~12 itens concretos e relevantes, "
                + "um por linha, no formato palavra|tipo|sinônimos (tipo: v/s/a/e/l/p). Só a lista.\n\n"
                + "PEDIDO: " + pedido
                + "<end_of_turn>\n<start_of_turn>model\n";

        JsonNode resposta = post("/api/generate", Map.of(
                "model", MODELO_PRANCHA,
                "prompt", prompt,
                "raw", true,
                "stream", false,
                "options", Map.of("temperature", 0, "num_predict", 320)
        ));
        return resposta.path("response").asText("");
    }

    private JsonNode post(String caminho, Map<String, Object> corpo) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(URL + caminho))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(corpo)))
                .build();

        JsonNode resposta = mapper.readTree(enviar(request).body());
        if (resposta.has("error")) {
            throw new IllegalStateException("O Ollama respondeu com erro: " + resposta.get("error").asText());
        }
        return resposta;
    }

    private HttpResponse<String> enviar(HttpRequest request) throws Exception {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException("O Ollama não está aberto em " + URL + ". Abra o Ollama e tente de novo.");
        }
    }
}
