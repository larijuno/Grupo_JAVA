package org.example;

import java.util.ArrayList;
import java.util.List;

public final class VocabularioNucleo {


    private record Palavra(String rotulo, String busca, int tipo, int idPreferido) {
    }

    private static final List<Palavra> PALAVRAS = List.of(

            new Palavra("EU", "eu", 1, 0),
            new Palavra("VOCÊ", "você", 1, 0),
            new Palavra("NÓS", "nós", 1, 0),

            new Palavra("QUERO", "quero", 3, 0),
            new Palavra("NÃO QUERO", "não quero", 3, 0),
            new Palavra("GOSTO", "gostar", 3, 0),
            new Palavra("FAZER", "fazer", 3, 0),
            new Palavra("IR", "ir", 3, 0),
            new Palavra("COMER", "comer", 3, 0),
            new Palavra("BEBER", "beber", 3, 0),
            new Palavra("BRINCAR", "brincar", 3, 0),
            new Palavra("VER", "ver", 3, 0),
            new Palavra("DAR", "dar", 3, 0),
            new Palavra("ABRIR", "abrir", 3, 0),
            new Palavra("PARAR", "parar", 3, 0),
            new Palavra("AJUDA", "ajudar", 3, 0),
            new Palavra("ACABOU", "acabar", 3, 28429),

            new Palavra("MAIS", "mais", 4, 0),
            new Palavra("MUITO", "muito", 4, 0),
            new Palavra("BOM", "bom", 4, 0),
            new Palavra("RUIM", "ruim", 4, 0),
            new Palavra("GRANDE", "grande", 4, 0),
            new Palavra("PEQUENO", "pequeno", 4, 0),

            new Palavra("SIM", "sim", 5, 0),
            new Palavra("NÃO", "não", 5, 0),
            new Palavra("OLÁ", "olá", 5, 0),
            new Palavra("TCHAU", "tchau", 5, 0),
            new Palavra("POR FAVOR", "por favor", 5, 0),
            new Palavra("OBRIGADO", "obrigado", 5, 0)
    );

    private VocabularioNucleo() {
    }


    public static List<ItemPrancha> carregar(Arasaac arasaac, RepositorioDePranchas repositorio) throws Exception {
        List<ItemPrancha> salvo = repositorio.carregarNucleo();
        if (!salvo.isEmpty()) return salvo;
        return restaurarPadrao(arasaac, repositorio);
    }


    public static List<ItemPrancha> restaurarPadrao(Arasaac arasaac, RepositorioDePranchas repositorio) throws Exception {
        List<ItemPrancha> nucleo = new ArrayList<>();
        for (Palavra palavra : PALAVRAS) {
            List<ItemArasaac> encontrados = arasaac.buscarExato(palavra.busca());
            if (encontrados.isEmpty()) {
                nucleo.add(ItemPrancha.naoEncontrado(palavra.rotulo()));
                continue;
            }

            ItemArasaac escolhido = encontrados.get(0);
            for (ItemArasaac encontrado : encontrados) {
                if (encontrado.id() == palavra.idPreferido()) escolhido = encontrado;
            }
            ItemArasaac comRotulo = new ItemArasaac(escolhido.id(), palavra.rotulo(), palavra.tipo(),
                    escolhido.significado(), escolhido.palavras());
            nucleo.add(ItemPrancha.encontrado(palavra.rotulo(), comRotulo));
        }
        repositorio.salvarNucleo(nucleo);
        return nucleo;
    }
}