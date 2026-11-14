package org.example;

public record ItemPrancha(String conceito, ItemArasaac pictograma, Situacao situacao, String erro) {

    public enum Situacao { ENCONTRADO, NAO_ENCONTRADO, REPETIDO, ERRO }

    public static ItemPrancha encontrado(String conceito, ItemArasaac pictograma) {
        return new ItemPrancha(conceito, pictograma, Situacao.ENCONTRADO, null);
    }

    public static ItemPrancha naoEncontrado(String conceito) {
        return new ItemPrancha(conceito, null, Situacao.NAO_ENCONTRADO, null);
    }

    public static ItemPrancha repetido(String conceito, ItemArasaac pictograma) {
        return new ItemPrancha(conceito, pictograma, Situacao.REPETIDO, null);
    }

    public static ItemPrancha comErro(String conceito, String erro) {
        return new ItemPrancha(conceito, null, Situacao.ERRO, erro);
    }

    public boolean encontrado() {
        return situacao == Situacao.ENCONTRADO;
    }

    public int tipo() {
        return pictograma == null ? 0 : pictograma.tipo();
    }
}
