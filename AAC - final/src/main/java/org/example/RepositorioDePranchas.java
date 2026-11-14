package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class RepositorioDePranchas {

    private static final String PASTA_DE_IMAGENS = "imagens";
    private static final String ARQUIVO_DE_DADOS = "prancha.json";
    private static final String ARQUIVO_HTML = "prancha.html";
    private static final String ARQUIVO_DO_NUCLEO = "nucleo.json";

    private final Path base;
    private final Arasaac arasaac;
    private final ExportadorPrancha exportador = new ExportadorPrancha();
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public RepositorioDePranchas(Path base, Arasaac arasaac) {
        this.base = base;
        this.arasaac = arasaac;
    }

    public boolean existe(String tema) {
        return Files.exists(pastaDoTema(tema).resolve(ARQUIVO_DE_DADOS));
    }

    public Optional<PranchaSalva> carregar(String tema) throws Exception {
        Path arquivo = pastaDoTema(tema).resolve(ARQUIVO_DE_DADOS);
        if (!Files.exists(arquivo)) return Optional.empty();
        return Optional.of(mapper.readValue(arquivo.toFile(), PranchaSalva.class));
    }

    public List<PranchaSalva> listar() {
        List<PranchaSalva> pranchas = new ArrayList<>();
        if (!Files.isDirectory(base)) return pranchas;

        try (DirectoryStream<Path> pastas = Files.newDirectoryStream(base, Files::isDirectory)) {
            for (Path pasta : pastas) {
                Path arquivo = pasta.resolve(ARQUIVO_DE_DADOS);
                if (!Files.exists(arquivo)) continue;
                try {
                    pranchas.add(mapper.readValue(arquivo.toFile(), PranchaSalva.class));
                } catch (Exception ignorada) {
                    continue;
                }
            }
        } catch (Exception e) {
            return pranchas;
        }

        pranchas.sort(Comparator.comparing(PranchaSalva::salvaEm, Comparator.nullsLast(Comparator.reverseOrder())));
        return pranchas;
    }

    public Path salvar(String tema, List<ItemPrancha> nucleo, List<ItemPrancha> itens) throws Exception {
        List<ItemPrancha> paraSalvar = new ArrayList<>();
        for (ItemPrancha item : itens) {
            if (item.situacao() != ItemPrancha.Situacao.REPETIDO) paraSalvar.add(item);
        }

        baixarImagens(nucleo);
        baixarImagens(paraSalvar);

        Path pasta = pastaDoTema(tema);
        Files.createDirectories(pasta);
        String agora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
        mapper.writeValue(pasta.resolve(ARQUIVO_DE_DADOS).toFile(), new PranchaSalva(tema, agora, paraSalvar));

        Path html = pasta.resolve(ARQUIVO_HTML);
        exportador.escreverHtml(html, tema, nucleo, paraSalvar, "../" + PASTA_DE_IMAGENS + "/");
        return html;
    }

    public Path arquivoHtml(String tema) {
        return pastaDoTema(tema).resolve(ARQUIVO_HTML);
    }

    public String enderecoDaImagem(ItemArasaac pictograma) {
        Path local = imagemLocal(pictograma.id());
        return Files.exists(local) ? local.toUri().toString() : pictograma.urlDaImagem();
    }

    public List<ItemPrancha> carregarNucleo() {
        Path arquivo = base.resolve(ARQUIVO_DO_NUCLEO);
        if (!Files.exists(arquivo)) return List.of();
        try {
            return mapper.readValue(arquivo.toFile(), new TypeReference<List<ItemPrancha>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    public void salvarNucleo(List<ItemPrancha> nucleo) throws Exception {
        baixarImagens(nucleo);
        Files.createDirectories(base);
        mapper.writeValue(base.resolve(ARQUIVO_DO_NUCLEO).toFile(), nucleo);
    }

    /** Regrava o HTML de todas as pranchas salvas para que mostrem o vocabulário básico atual. */
    public void reexportarTodas(List<ItemPrancha> nucleo) throws Exception {
        for (PranchaSalva prancha : listar()) {
            salvar(prancha.tema(), nucleo, prancha.itens());
        }
    }

    private void baixarImagens(List<ItemPrancha> itens) throws Exception {
        Files.createDirectories(base.resolve(PASTA_DE_IMAGENS));
        for (ItemPrancha item : itens) {
            if (!item.encontrado()) continue;
            Path destino = imagemLocal(item.pictograma().id());
            if (!Files.exists(destino)) Files.write(destino, arasaac.baixarImagem(item.pictograma().id()));
        }
    }

    private Path imagemLocal(int id) {
        return base.resolve(PASTA_DE_IMAGENS).resolve(id + ".png");
    }

    private Path pastaDoTema(String tema) {
        String nome = Normalizer.normalize(tema.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
        if (nome.isEmpty() || nome.equals(PASTA_DE_IMAGENS)) nome = "prancha_" + nome;
        return base.resolve(nome);
    }
}