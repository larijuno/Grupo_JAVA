package org.example;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record ConceitoPrancha(String palavra, List<String> sinonimos) {

    private static final int MAXIMO_DE_SINONIMOS = 4;

    public List<String> termos() {
        List<String> termos = new ArrayList<>();
        termos.add(palavra);
        termos.addAll(sinonimos);
        return termos;
    }

    public static List<ConceitoPrancha> daResposta(String resposta) {
        List<ConceitoPrancha> conceitos = new ArrayList<>();
        Set<String> vistos = new LinkedHashSet<>();

        for (String linhaBruta : resposta.split("\\R")) {
            ConceitoPrancha conceito = daLinha(linhaBruta);
            if (conceito != null && vistos.add(conceito.palavra().toLowerCase(Locale.ROOT))) {
                conceitos.add(conceito);
            }
        }
        return conceitos;
    }

    private static ConceitoPrancha daLinha(String linhaBruta) {
        String linha = linhaBruta.replace("*", "").replace("\"", "").trim();
        linha = linha.replaceAll("^[-•]\\s*", "");
        linha = linha.replaceAll("^\\d+[.)]\\s*", "");
        linha = linha.trim();
        if (linha.isEmpty()) return null;

        String[] partes;
        if (linha.contains("|")) {
            partes = linha.split("\\|", -1);
        } else if (linha.matches("[^:]{1,40}\\s:\\s.+")) {
            partes = linha.split("\\s:\\s", 2);
        } else {
            partes = new String[]{linha};
        }

        String[] nomes = partes[0].split("/");
        String palavra = nomes[0].trim();
        if (palavra.isEmpty() || palavra.length() > 40 || palavra.endsWith(":")) return null;

        Set<String> sinonimos = new LinkedHashSet<>();
        for (int i = 1; i < nomes.length; i++) {
            adicionarSinonimo(sinonimos, nomes[i], palavra);
        }
        if (partes.length >= 3) {
            for (String sinonimo : partes[2].split("[,;/]")) {
                adicionarSinonimo(sinonimos, sinonimo, palavra);
            }
        }

        List<String> lista = new ArrayList<>(sinonimos);
        if (lista.size() > MAXIMO_DE_SINONIMOS) lista = lista.subList(0, MAXIMO_DE_SINONIMOS);
        return new ConceitoPrancha(maiuscula(palavra), List.copyOf(lista));
    }

    private static void adicionarSinonimo(Set<String> sinonimos, String bruto, String palavra) {
        String sinonimo = bruto.trim();
        if (!sinonimo.isEmpty() && sinonimo.length() <= 40 && !sinonimo.equalsIgnoreCase(palavra)) {
            sinonimos.add(sinonimo);
        }
    }

    private static String maiuscula(String texto) {
        return texto.substring(0, 1).toUpperCase(Locale.ROOT) + texto.substring(1);
    }
}
