package org.example;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class ExportadorPrancha {

    private static final String CREDITO = "Pictogramas: Sergio Palao. Origem: ARASAAC (https://arasaac.org). "
            + "Licença: CC BY-NC-SA. Propriedade: Governo de Aragão (Espanha).";

    public void escreverHtml(Path arquivo, String tema, List<ItemPrancha> nucleo, List<ItemPrancha> itens,
                             String pastaDasImagens) throws Exception {
        String pagina = """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Prancha: %s</title>
                  <style>
                    * { box-sizing: border-box; }
                    body {
                      font-family: "Trebuchet MS", "Segoe UI", Arial, sans-serif; color: #3B2A5A; margin: 0; padding: 28px;
                      background:
                        radial-gradient(circle at 0%% 0%%, rgba(242,140,166,.55), rgba(242,140,166,0) 38%%),
                        radial-gradient(circle at 55%% 0%%, rgba(185,139,224,.40), rgba(185,139,224,0) 30%%),
                        radial-gradient(circle at 100%% 0%%, rgba(245,200,92,.55), rgba(245,200,92,0) 36%%),
                        radial-gradient(circle at 100%% 100%%, rgba(124,184,240,.55), rgba(124,184,240,0) 40%%),
                        radial-gradient(circle at 0%% 100%%, rgba(127,211,107,.45), rgba(127,211,107,0) 34%%),
                        #FFF8EE;
                      background-attachment: fixed;
                      -webkit-print-color-adjust: exact; print-color-adjust: exact;
                    }
                    h1 { margin: 0 0 18px; font-size: 32px; display: flex; align-items: center; gap: 14px; }
                    h1 svg { flex: none; }
                    h2 { margin: 0 0 14px; font-size: 19px; color: #7A56B0; }
                    .painel { background: rgba(255,255,255,.82); border: 4px solid #7FD36B; border-radius: 26px; padding: 18px 20px 22px; margin-bottom: 22px; box-shadow: 0 4px 14px rgba(59,42,90,.14); }
                    .painel-nucleo { border-color: #B98BE0; }
                    .grade { display: grid; grid-template-columns: repeat(auto-fill, minmax(170px, 1fr)); gap: 16px; }
                    .grade.nucleo { grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 10px; }
                    .cartao { background: #fff; border: 6px solid #B7AFC4; border-radius: 20px; padding: 10px; text-align: center; box-shadow: 0 3px 8px rgba(59,42,90,.16); break-inside: avoid; }
                    .cartao img { width: 100%%; max-width: 160px; aspect-ratio: 1; object-fit: contain; }
                    .palavra { font-weight: bold; font-size: 18px; margin-top: 6px; color: #3B2A5A; }
                    .nucleo .palavra { font-size: 15px; }
                    footer { margin-top: 8px; font-size: 12px; color: #7B6A94; }
                    @media print {
                      body { background: #fff; padding: 0; }
                      .painel { background: #fff; box-shadow: none; }
                      .cartao { box-shadow: none; }
                    }
                  </style>
                </head>
                <body>
                  <h1><svg width="58" height="36" viewBox="0 0 58 36" aria-hidden="true"><rect x="2" y="6" width="24" height="24" rx="5" fill="#F59E7E"/><circle cx="26" cy="18" r="6" fill="#F59E7E"/><rect x="28" y="6" width="24" height="24" rx="5" fill="#7CB8F0"/><circle cx="52" cy="18" r="6" fill="#7CB8F0"/></svg>Prancha: %s</h1>
                %s%s  <footer>%s</footer>
                </body>
                </html>
                """.formatted(html(tema), html(tema),
                secao("Vocabulário básico", "grade nucleo", nucleo, pastaDasImagens),
                secao(tema, "grade", itens, pastaDasImagens),
                html(CREDITO));

        Files.createDirectories(arquivo.getParent());
        Files.writeString(arquivo, pagina, StandardCharsets.UTF_8);
    }

    private static String secao(String titulo, String classe, List<ItemPrancha> itens, String pastaDasImagens) {
        StringBuilder cartoes = new StringBuilder();
        for (ItemPrancha item : itens) {
            if (!item.encontrado()) continue;
            ItemArasaac pictograma = item.pictograma();
            cartoes.append("    <div class=\"cartao\" style=\"border-color: ").append(Fitzgerald.cor(pictograma.tipo())).append("; background: ").append(Fitzgerald.fundo(pictograma.tipo())).append("\">\n")
                    .append("      <img src=\"").append(pastaDasImagens).append(pictograma.id()).append(".png\" alt=\"")
                    .append(html(pictograma.palavra())).append("\">\n")
                    .append("      <div class=\"palavra\">").append(html(pictograma.palavra().toUpperCase(Locale.ROOT))).append("</div>\n")
                    .append("    </div>\n");
        }
        if (cartoes.isEmpty()) return "";
        String painel = classe.contains("nucleo") ? "painel painel-nucleo" : "painel";
        return "  <section class=\"" + painel + "\">\n  <h2>" + html(titulo) + "</h2>\n  <div class=\"" + classe + "\">\n"
                + cartoes + "  </div>\n  </section>\n";
    }

    private static String html(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
