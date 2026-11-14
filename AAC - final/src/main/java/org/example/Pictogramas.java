package org.example;

import java.util.List;

public class Pictogramas {

    public record Achado(ItemCatalogo item, double nota) {
    }

    public Achado buscarMaisParecido(
            double[] vetorConsulta,
            List<ItemCatalogo> catalogo) {

        Achado melhor = null;

        for (ItemCatalogo item : catalogo) {

            double similaridade =
                    similaridadeCosseno(vetorConsulta, item.vetor());

            if (!Double.isNaN(similaridade) && (melhor == null || similaridade > melhor.nota())) {
                melhor = new Achado(item, similaridade);
            }
        }

        return melhor;
    }

    public double similaridadeCosseno(double[] a, double[] b) {

        double produtoEscalar = 0;
        double tamanhoA = 0;
        double tamanhoB = 0;

        for (int i = 0; i < a.length; i++) {
            produtoEscalar += a[i] * b[i];
            tamanhoA += a[i] * a[i];
            tamanhoB += b[i] * b[i];
        }

        return produtoEscalar /
                (Math.sqrt(tamanhoA) * Math.sqrt(tamanhoB));
    }
}
