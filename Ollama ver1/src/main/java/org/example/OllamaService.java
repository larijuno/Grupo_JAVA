package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class OllamaService {

    private static final String MODELO_EMBEDDING =
            "hf.co/tardellirs/aac-pictogram-retriever-ptbr-GGUF:Q4_K_M";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public OllamaEmbed gerarEmbedding(String texto) throws Exception {

        // Jackson cuida do escape de aspas, barras e acentos
        String json = mapper.writeValueAsString(Map.of(
                "model", MODELO_EMBEDDING,
                "input", texto
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:11434/api/embed"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        return mapper.readValue(response.body(), OllamaEmbed.class);
    }
}
