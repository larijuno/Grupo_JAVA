package org.example;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class MemoriaDePalavras {

    public static class Dados {
        public Map<String, ItemArasaac> escolhas = new LinkedHashMap<>();
        public Map<String, List<String>> removidas = new LinkedHashMap<>();
        public Map<String, List<String>> adicionadas = new LinkedHashMap<>();
    }

    private final Path arquivo;
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private Dados dados;

    public MemoriaDePalavras(Path arquivo) {
        this.arquivo = arquivo;
        this.dados = ler();
    }

    public synchronized Optional<ItemArasaac> escolha(String palavra) {
        return Optional.ofNullable(dados.escolhas.get(chave(palavra)));
    }

    public synchronized void lembrarEscolha(String palavra, ItemArasaac pictograma) throws Exception {
        dados.escolhas.put(chave(palavra), pictograma);
        gravar();
    }

    public synchronized boolean removida(String tema, String palavra) {
        return dados.removidas.getOrDefault(chave(tema), List.of()).contains(chave(palavra));
    }

    public synchronized void lembrarRemocao(String tema, String palavra) throws Exception {
        List<String> removidas = dados.removidas.computeIfAbsent(chave(tema), t -> new ArrayList<>());
        if (!removidas.contains(chave(palavra))) removidas.add(chave(palavra));
        List<String> adicionadas = dados.adicionadas.get(chave(tema));
        if (adicionadas != null) adicionadas.removeIf(adicionada -> chave(adicionada).equals(chave(palavra)));
        gravar();
    }

    public synchronized List<String> adicionadas(String tema) {
        return List.copyOf(dados.adicionadas.getOrDefault(chave(tema), List.of()));
    }

    public synchronized void lembrarAdicao(String tema, String palavra) throws Exception {
        List<String> adicionadas = dados.adicionadas.computeIfAbsent(chave(tema), t -> new ArrayList<>());
        if (adicionadas.stream().noneMatch(adicionada -> chave(adicionada).equals(chave(palavra)))) adicionadas.add(palavra);
        List<String> removidas = dados.removidas.get(chave(tema));
        if (removidas != null) removidas.remove(chave(palavra));
        gravar();
    }

    public static String chave(String texto) {
        return Normalizer.normalize(texto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ");
    }

    private Dados ler() {
        if (!Files.exists(arquivo)) return new Dados();
        try {
            Dados lidos = mapper.readValue(arquivo.toFile(), Dados.class);
            if (lidos.escolhas == null) lidos.escolhas = new LinkedHashMap<>();
            if (lidos.removidas == null) lidos.removidas = new LinkedHashMap<>();
            if (lidos.adicionadas == null) lidos.adicionadas = new LinkedHashMap<>();
            return lidos;
        } catch (Exception e) {
            return new Dados();
        }
    }

    private void gravar() throws Exception {
        Path pasta = arquivo.toAbsolutePath().getParent();
        if (pasta != null) Files.createDirectories(pasta);
        mapper.writeValue(arquivo.toFile(), dados);
    }
}
