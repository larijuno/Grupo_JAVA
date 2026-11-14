package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Arasaac {

    private static final String API = "https://api.arasaac.org/v1/pictograms/br/";
    private static final String IMAGENS = "https://static.arasaac.org/pictograms/";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, List<ItemArasaac>> cache = new ConcurrentHashMap<>();

    public static String urlDaImagem(int id) {
        return IMAGENS + id + "/" + id + "_500.png";
    }

    public List<ItemArasaac> buscarExato(String termo) throws Exception {
        return consultar("bestsearch/", termo);
    }

    public List<ItemArasaac> buscarAproximado(String termo) throws Exception {
        return consultar("search/", termo);
    }

    public byte[] baixarImagem(int id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(urlDaImagem(id)))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<byte[]> resposta = enviar(request, HttpResponse.BodyHandlers.ofByteArray());
        if (resposta.statusCode() != 200) {
            throw new IllegalStateException("Não foi possível baixar o pictograma " + id + ".");
        }
        return resposta.body();
    }

    private List<ItemArasaac> consultar(String tipoDeBusca, String termo) throws Exception {
        String texto = termo.trim().toLowerCase(Locale.ROOT);
        if (texto.isEmpty()) return List.of();

        String chave = tipoDeBusca + texto;
        List<ItemArasaac> guardado = cache.get(chave);
        if (guardado != null) return guardado;

        String codificado = URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
        HttpRequest request = HttpRequest.newBuilder(URI.create(API + tipoDeBusca + codificado))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<String> resposta = enviar(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (resposta.statusCode() >= 500) {
            throw new IllegalStateException("O ARASAAC está com problemas agora (erro " + resposta.statusCode() + "). Tente de novo mais tarde.");
        }

        List<ItemArasaac> resultados = new ArrayList<>();
        if (resposta.statusCode() == 200) {
            JsonNode raiz = mapper.readTree(resposta.body());
            if (raiz.isArray()) {
                for (JsonNode pictograma : raiz) {
                    ItemArasaac item = converter(pictograma, texto);
                    if (item != null) resultados.add(item);
                }
            }
        }

        List<ItemArasaac> imutavel = List.copyOf(resultados);
        cache.put(chave, imutavel);
        return imutavel;
    }

    private ItemArasaac converter(JsonNode pictograma, String termo) {
        if (!pictograma.has("_id")) return null;

        List<String> palavras = new ArrayList<>();
        JsonNode escolhida = null;
        for (JsonNode keyword : pictograma.path("keywords")) {
            String palavra = corrigirAcentos(keyword.path("keyword").asText("")).trim();
            if (palavra.isEmpty()) continue;
            palavras.add(palavra);
            if (escolhida == null || (palavra.equalsIgnoreCase(termo) && !mesmaPalavra(escolhida, termo))) {
                escolhida = keyword;
            }
        }
        if (escolhida == null) return null;

        String significado = corrigirAcentos(escolhida.path("meaning").asText("")).trim();
        if (significado.isEmpty()) {
            for (JsonNode keyword : pictograma.path("keywords")) {
                String outro = corrigirAcentos(keyword.path("meaning").asText("")).trim();
                if (!outro.isEmpty()) {
                    significado = outro;
                    break;
                }
            }
        }

        return new ItemArasaac(
                pictograma.get("_id").asInt(),
                corrigirAcentos(escolhida.path("keyword").asText("")).trim(),
                escolhida.path("type").asInt(0),
                significado,
                List.copyOf(palavras)
        );
    }

    private static boolean mesmaPalavra(JsonNode keyword, String termo) {
        return corrigirAcentos(keyword.path("keyword").asText("")).trim().equalsIgnoreCase(termo);
    }

    private static String corrigirAcentos(String texto) {
        if (texto.indexOf('Ã') < 0 && texto.indexOf('Â') < 0) return texto;
        for (char c : texto.toCharArray()) {
            if (c > 0xFF) return texto;
        }
        String corrigido = new String(texto.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        return corrigido.indexOf('�') >= 0 ? texto : corrigido;
    }

    private <T> HttpResponse<T> enviar(HttpRequest request, HttpResponse.BodyHandler<T> leitor) throws Exception {
        try {
            return client.send(request, leitor);
        } catch (IOException e) {
            throw new IllegalStateException("Sem conexão com o ARASAAC (arasaac.org). Verifique a internet e tente de novo.");
        }
    }
}
