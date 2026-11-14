package org.example;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GeradorDePrancha {

    private static final double NOTA_MINIMA = 0.55;
    private static final int MAXIMO_DE_CANDIDATOS = 20;
    private static final int MINIMO_DE_ITENS = 3;
    private static final double RELEVANCIA_MINIMA = 0.08;
    private static final Set<String> PALAVRAS_VAZIAS = Set.of(
            "a", "o", "as", "os", "de", "da", "do", "das", "dos", "e", "em", "na", "no", "nas", "nos",
            "um", "uma", "uns", "umas", "para", "pra", "com", "por", "ao", "aos", "à", "às", "se");

    private final OllamaService ollama;
    private final Arasaac arasaac;
    private final Pictogramas buscador;
    private final MemoriaDePalavras memoria;

    public GeradorDePrancha() {
        this(new OllamaService(), new Arasaac(), new Pictogramas());
    }

    public GeradorDePrancha(OllamaService ollama, Arasaac arasaac, Pictogramas buscador) {
        this(ollama, arasaac, buscador, null);
    }

    public GeradorDePrancha(OllamaService ollama, Arasaac arasaac, Pictogramas buscador, MemoriaDePalavras memoria) {
        this.ollama = ollama;
        this.arasaac = arasaac;
        this.buscador = buscador;
        this.memoria = memoria;
    }

    public List<ConceitoPrancha> conceitosDoTema(String tema) throws Exception {
        List<ConceitoPrancha> conceitos = new ArrayList<>();
        Set<String> vistos = new HashSet<>();

        for (ConceitoPrancha conceito : listarConceitos(tema)) {
            if (memoria != null && memoria.removida(tema, conceito.palavra())) continue;
            if (vistos.add(MemoriaDePalavras.chave(conceito.palavra()))) conceitos.add(conceito);
        }
        if (memoria != null) {
            for (String adicionada : memoria.adicionadas(tema)) {
                if (vistos.add(MemoriaDePalavras.chave(adicionada))) conceitos.add(new ConceitoPrancha(adicionada, List.of()));
            }
        }
        return conceitos;
    }

    public void verificar() throws Exception {
        ollama.verificar();
    }

    public List<ConceitoPrancha> listarConceitos(String tema) throws Exception {
        List<ConceitoPrancha> melhor = List.of();
        double melhorRelevancia = Double.NEGATIVE_INFINITY;

        for (int tentativa = 0; tentativa < ollama.quantidadeDeTentativas(); tentativa++) {
            List<ConceitoPrancha> conceitos = ollama.gerarPrancha(tema, tentativa);
            if (conceitos.size() < MINIMO_DE_ITENS) {
                if (melhor.size() < MINIMO_DE_ITENS && conceitos.size() > melhor.size()) melhor = conceitos;
                continue;
            }

            double relevancia = relevanciaMedia(tema, conceitos);
            if (relevancia >= RELEVANCIA_MINIMA) return conceitos;
            if (melhor.size() < MINIMO_DE_ITENS || relevancia > melhorRelevancia) {
                melhor = conceitos;
                melhorRelevancia = relevancia;
            }
        }
        return melhor;
    }

    private double relevanciaMedia(String tema, List<ConceitoPrancha> conceitos) throws Exception {
        List<String> textos = new ArrayList<>();
        textos.add("task: search result | query: " + tema);
        for (ConceitoPrancha conceito : conceitos) textos.add("title: none | text: " + conceito.palavra());

        List<double[]> vetores = ollama.gerarEmbeddings(textos);
        double soma = 0;
        int contados = 0;
        for (int i = 1; i < vetores.size(); i++) {
            if (vetores.get(i) == null) continue;
            double nota = buscador.similaridadeCosseno(vetores.get(0), vetores.get(i));
            if (Double.isNaN(nota)) continue;
            soma += nota;
            contados++;
        }
        return contados == 0 ? 0 : soma / contados;
    }

    public List<ItemArasaac> opcoesDePictograma(String termo) throws Exception {
        Map<Integer, ItemArasaac> opcoes = new LinkedHashMap<>();
        for (String variacao : variacoes(termo)) {
            for (ItemArasaac item : arasaac.buscarExato(variacao)) opcoes.putIfAbsent(item.id(), item);
        }
        for (ItemArasaac item : arasaac.buscarAproximado(termo)) {
            if (opcoes.size() >= 30) break;
            opcoes.putIfAbsent(item.id(), item);
        }
        return new ArrayList<>(opcoes.values());
    }

    public ItemPrancha buscarPictograma(String tema, ConceitoPrancha conceito, Set<Integer> jaUsados) {
        try {
            ItemArasaac escolhido = memoria == null ? null : memoria.escolha(conceito.palavra()).orElse(null);
            if (escolhido == null) escolhido = buscarExato(tema, conceito);
            if (escolhido == null) {
                escolhido = buscarAproximado(tema, conceito, jaUsados);
            }

            if (escolhido == null) return ItemPrancha.naoEncontrado(conceito.palavra());
            if (!jaUsados.add(escolhido.id())) return ItemPrancha.repetido(conceito.palavra(), escolhido);
            return ItemPrancha.encontrado(conceito.palavra(), escolhido);
        } catch (Exception e) {
            return ItemPrancha.comErro(conceito.palavra(), e.getMessage());
        }
    }

    private ItemArasaac buscarExato(String tema, ConceitoPrancha conceito) throws Exception {
        for (String termo : conceito.termos()) {
            for (String variacao : variacoes(termo)) {
                List<ItemArasaac> exatos = arasaac.buscarExato(variacao);
                if (exatos.size() == 1) return exatos.get(0);
                if (exatos.size() > 1) return maisParecido(tema, conceito, exatos, -1);
            }
        }
        return null;
    }

    private static List<String> variacoes(String termo) {
        String base = termo.trim().toLowerCase(Locale.ROOT);
        List<String> variacoes = new ArrayList<>();
        variacoes.add(base);
        if (base.contains("-")) variacoes.add(base.replace("-", " "));
        if (base.endsWith("s") && base.length() > 3) {
            variacoes.add(base.substring(0, base.length() - 1));
        } else if (!base.contains(" ") && base.matches(".*[aeiou]")) {
            variacoes.add(base + "s");
        }
        return variacoes;
    }

    private ItemArasaac buscarAproximado(String tema, ConceitoPrancha conceito, Set<Integer> jaUsados) throws Exception {
        Map<Integer, ItemArasaac> candidatos = new LinkedHashMap<>();

        for (String termo : conceito.termos()) {
            adicionar(candidatos, arasaac.buscarAproximado(termo), jaUsados);
        }
        for (String parte : partesImportantes(conceito.palavra())) {
            adicionar(candidatos, arasaac.buscarExato(parte), jaUsados);
        }

        if (candidatos.isEmpty()) return null;
        return maisParecido(tema, conceito, new ArrayList<>(candidatos.values()), NOTA_MINIMA);
    }

    private static void adicionar(Map<Integer, ItemArasaac> candidatos, List<ItemArasaac> itens, Set<Integer> jaUsados) {
        for (ItemArasaac item : itens) {
            if (candidatos.size() >= MAXIMO_DE_CANDIDATOS) return;
            if (!jaUsados.contains(item.id())) candidatos.putIfAbsent(item.id(), item);
        }
    }

    private static List<String> partesImportantes(String palavra) {
        String[] partes = palavra.trim().split("\\s+");
        List<String> importantes = new ArrayList<>();
        if (partes.length < 2) return importantes;
        for (String parte : partes) {
            String limpa = parte.toLowerCase(Locale.ROOT);
            if (limpa.length() >= 3 && !PALAVRAS_VAZIAS.contains(limpa)) importantes.add(limpa);
        }
        return importantes;
    }

    private ItemArasaac maisParecido(String tema, ConceitoPrancha conceito, List<ItemArasaac> candidatos,
                                     double notaMinima) throws Exception {
        List<String> textos = new ArrayList<>();
        textos.add("task: search result | query: " + conceito.palavra() + " (tema: " + tema + ")");
        for (ItemArasaac candidato : candidatos) {
            String texto = candidato.significado().isEmpty()
                    ? candidato.palavra()
                    : candidato.palavra() + ": " + candidato.significado();
            textos.add("title: none | text: " + texto);
        }

        List<double[]> vetores = ollama.gerarEmbeddings(textos);
        List<ItemCatalogo> catalogo = new ArrayList<>();
        for (int i = 0; i < candidatos.size(); i++) {
            ItemArasaac candidato = candidatos.get(i);
            double[] vetor = vetores.get(i + 1);
            if (vetor != null) catalogo.add(new ItemCatalogo(candidato.id(), candidato.palavra(), vetor));
        }

        Pictogramas.Achado achado = buscador.buscarMaisParecido(vetores.get(0), catalogo);
        if (achado == null || achado.nota() < notaMinima) return null;

        for (ItemArasaac candidato : candidatos) {
            if (candidato.id() == achado.item().id()) return candidato;
        }
        return null;
    }
}
