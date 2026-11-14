package org.example;

import java.util.List;

public record ItemArasaac(
        int id,
        String palavra,
        int tipo,
        String significado,
        List<String> palavras
) {

    public String urlDaImagem() {
        return Arasaac.urlDaImagem(id);
    }
}
