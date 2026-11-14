package org.example;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record PranchaSalva(String tema, String salvaEm, List<ItemPrancha> itens) {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String dataFormatada() {
        try {
            return LocalDateTime.parse(salvaEm).format(FORMATO);
        } catch (Exception e) {
            return salvaEm == null ? "" : salvaEm;
        }
    }
}
