package org.example;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PranchaController {

    private static final double TAMANHO_DA_IMAGEM = 140;
    private static final double TAMANHO_DO_NUCLEO = 80;
    private static final double TAMANHO_DA_OPCAO = 90;

    @FXML
    private TextField campoTema;
    @FXML
    private Button botaoGerar;
    @FXML
    private Button botaoNovamente;
    @FXML
    private TextField campoNovaPalavra;
    @FXML
    private Button botaoAdicionar;
    @FXML
    private FlowPane nucleo;
    @FXML
    private FlowPane grade;
    @FXML
    private Label status;
    @FXML
    private ProgressIndicator progresso;
    @FXML
    private ListView<PranchaSalva> historico;

    @FXML
    private Button botaoModo;
    @FXML
    private HBox linhaEdicao;
    @FXML
    private HBox linhaNucleo;
    @FXML
    private TextField campoNovaPalavraNucleo;
    @FXML
    private Button botaoAdicionarNucleo;
    @FXML
    private Button botaoRestaurarNucleo;
    @FXML
    private HBox barraFrase;
    @FXML
    private FlowPane frase;

    private static final double TAMANHO_DA_FRASE = 60;
    private final BooleanProperty modoUsar = new SimpleBooleanProperty(false);
    private final List<ItemPrancha> fraseItens = new ArrayList<>();

    private final Arasaac arasaac = new Arasaac();
    private final MemoriaDePalavras memoria = new MemoriaDePalavras(Path.of("pranchas", "palavras.json"));
    private final GeradorDePrancha gerador = new GeradorDePrancha(new OllamaService(), arasaac, new Pictogramas(), memoria);
    private final RepositorioDePranchas repositorio = new RepositorioDePranchas(Path.of("pranchas"), arasaac);
    private final ExecutorService gravador = Executors.newSingleThreadExecutor(tarefa -> {
        Thread thread = new Thread(tarefa, "gravar-prancha");
        thread.setDaemon(true);
        return thread;
    });
    private final List<ItemPrancha> itens = new ArrayList<>();
    private volatile List<ItemPrancha> itensDoNucleo = List.of();
    private String temaAtual;
    private boolean ocupado;
    private boolean atualizandoHistorico;
    private volatile boolean ollamaVerificado;

    @FXML
    private void initialize() {
        progresso.setVisible(false);
        ocupado(false);

        linhaEdicao.visibleProperty().bind(modoUsar.not());
        linhaEdicao.managedProperty().bind(modoUsar.not());
        linhaNucleo.visibleProperty().bind(modoUsar.not());
        linhaNucleo.managedProperty().bind(modoUsar.not());
        barraFrase.visibleProperty().bind(modoUsar);
        barraFrase.managedProperty().bind(modoUsar);

        historico.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(PranchaSalva prancha, boolean vazio) {
                super.updateItem(prancha, vazio);
                setText(vazio || prancha == null ? null : prancha.tema() + "\n" + prancha.dataFormatada());
            }
        });
        historico.getSelectionModel().selectedItemProperty().addListener((observavel, antes, prancha) -> {
            if (prancha != null && !atualizandoHistorico && !ocupado) {
                campoTema.setText(prancha.tema());
                mostrarSalva(prancha);
            }
        });

        atualizarHistorico(null);
        carregarNucleo();
    }

    @FXML
    private void gerar() {
        String tema = textoDe(campoTema);
        if (tema.isEmpty()) {
            mostrarStatus("Digite um tema antes de gerar a prancha.", true);
            return;
        }

        try {
            Optional<PranchaSalva> salva = repositorio.carregar(tema);
            if (salva.isPresent()) {
                mostrarSalva(salva.get());
                selecionarNoHistorico(salva.get().tema());
                return;
            }
        } catch (Exception e) {
            mostrarStatus("Não foi possível abrir a prancha salva (" + mensagem(e) + "). Vou gerar outra.", true);
        }
        gerarNova(tema);
    }

    @FXML
    private void gerarNovamente() {
        String tema = temaAtual != null ? temaAtual : textoDe(campoTema);
        if (!tema.isEmpty()) gerarNova(tema);
    }

    @FXML
    private void adicionarPalavra(ActionEvent evento) {
        evento.consume();
        String palavra = textoDe(campoNovaPalavra);
        String tema = temaAtual;
        if (ocupado || tema == null) return;
        if (palavra.isEmpty()) {
            mostrarStatus("Digite a palavra que quer adicionar.", true);
            return;
        }
        if (itens.stream().anyMatch(item -> MemoriaDePalavras.chave(item.conceito()).equals(MemoriaDePalavras.chave(palavra)))) {
            mostrarStatus("\"" + palavra + "\" já está na prancha.", true);
            return;
        }

        Set<Integer> usados = idsEmUso();
        ocupado(true);
        Task<ItemPrancha> tarefa = new Task<>() {
            @Override
            protected ItemPrancha call() {
                updateMessage("Buscando pictograma para \"" + palavra + "\"...");
                return gerador.buscarPictograma(tema, new ConceitoPrancha(palavra, List.of()), usados);
            }
        };
        tarefa.setOnSucceeded(e -> {
            ocupado(false);
            ItemPrancha item = tarefa.getValue();
            if (item.situacao() == ItemPrancha.Situacao.REPETIDO) {
                mostrarStatus("O pictograma de \"" + palavra + "\" já está na prancha.", true);
                return;
            }
            itens.add(item);
            grade.getChildren().add(criarCartao(item, TAMANHO_DA_IMAGEM, true));
            campoNovaPalavra.clear();
            mostrarStatus(item.encontrado()
                    ? "\"" + palavra + "\" adicionada."
                    : "\"" + palavra + "\" adicionada sem pictograma. Clique no cartão para escolher um.", !item.encontrado());
            gravarEdicao(tema, () -> memoria.lembrarAdicao(tema, palavra));
        });
        tarefa.setOnFailed(e -> {
            ocupado(false);
            mostrarStatus("Erro: " + mensagem(tarefa.getException()), true);
        });
        iniciar(tarefa);
    }

    private void gerarNova(String tema) {
        limparPrancha(tema);
        ocupado(true);

        Task<Void> tarefa = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<ItemPrancha> nucleoAtual = itensDoNucleo;
                if (nucleoAtual.isEmpty()) {
                    try {
                        nucleoAtual = VocabularioNucleo.carregar(arasaac, repositorio);
                    } catch (Exception e) {
                        nucleoAtual = List.of();
                    }
                }

                if (!ollamaVerificado) {
                    updateMessage("Verificando o Ollama...");
                    gerador.verificar();
                    ollamaVerificado = true;
                }

                updateMessage("Escolhendo os itens da prancha sobre \"" + tema + "\"...");
                List<ConceitoPrancha> conceitos = gerador.conceitosDoTema(tema);
                if (conceitos.isEmpty()) {
                    throw new IllegalStateException("O modelo não sugeriu itens para esse tema. Tente escrever de outro jeito.");
                }

                Set<Integer> usados = new HashSet<>();
                for (ItemPrancha item : nucleoAtual) {
                    if (item.encontrado()) usados.add(item.pictograma().id());
                }

                List<ItemPrancha> gerados = new ArrayList<>();
                for (int i = 0; i < conceitos.size(); i++) {
                    updateMessage("Buscando pictogramas... (" + (i + 1) + " de " + conceitos.size() + ")");
                    ItemPrancha item = gerador.buscarPictograma(tema, conceitos.get(i), usados);
                    gerados.add(item);
                    Platform.runLater(() -> adicionar(item));
                }

                if (gerados.stream().anyMatch(ItemPrancha::encontrado)) {
                    updateMessage("Salvando a prancha...");
                    repositorio.salvar(tema, nucleoAtual, gerados);
                }
                return null;
            }
        };

        tarefa.setOnSucceeded(evento -> {
            ocupado(false);
            boolean salva = itens.stream().anyMatch(ItemPrancha::encontrado);
            mostrarStatus(resumo() + (salva ? " Salva em Pranchas salvas." : ""), false);
            atualizarHistorico(tema);
        });
        tarefa.setOnFailed(evento -> {
            ocupado(false);
            mostrarStatus("Erro: " + mensagem(tarefa.getException()), true);
            atualizarHistorico(tema);
        });
        iniciar(tarefa);
    }

    private void mostrarSalva(PranchaSalva prancha) {
        limparPrancha(prancha.tema());
        for (ItemPrancha item : prancha.itens()) adicionar(item);
        ocupado(false);
        mostrarStatus("Prancha \"" + prancha.tema() + "\" aberta das salvas (" + prancha.dataFormatada()
                + "). Para criar outra, clique em Gerar de novo.", false);
    }

    private void remover(ItemPrancha item) {
        String tema = temaAtual;
        int indice = indiceDe(item);
        if (ocupado || tema == null || indice < 0) return;

        itens.remove(indice);
        grade.getChildren().remove(indice);
        mostrarStatus("\"" + item.conceito() + "\" removida. Ela não volta quando você gerar este tema de novo.", false);
        gravarEdicao(tema, () -> memoria.lembrarRemocao(tema, item.conceito()));
    }

    private void trocarPictograma(ItemPrancha item) {
        String tema = temaAtual;
        if (ocupado || tema == null || indiceDe(item) < 0) return;

        abrirDialogoDePictograma(item.conceito(), escolhido -> {
            int indice = indiceDe(item);
            if (indice < 0) return;
            ItemArasaac comRotulo = new ItemArasaac(escolhido.id(), escolhido.palavra(), escolhido.tipo(),
                    escolhido.significado(), escolhido.palavras());
            ItemPrancha novo = ItemPrancha.encontrado(item.conceito(), comRotulo);
            itens.set(indice, novo);
            grade.getChildren().set(indice, criarCartao(novo, TAMANHO_DA_IMAGEM, true));
            mostrarStatus("Pictograma de \"" + item.conceito() + "\" trocado. A escolha vale para as próximas pranchas.", false);
            gravarEdicao(tema, () -> memoria.lembrarEscolha(item.conceito(), comRotulo));
        });
    }

    private void abrirDialogoDePictograma(String conceito, Consumer<ItemArasaac> aoEscolher) {
        Dialog<ItemArasaac> dialogo = new Dialog<>();
        dialogo.initOwner(grade.getScene().getWindow());
        dialogo.setTitle("Trocar pictograma");
        dialogo.setHeaderText("Escolha o pictograma para \"" + conceito + "\"");
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialogo.getDialogPane().getStylesheets().add(App.folhaDeEstilo());
        dialogo.setResultConverter(botao -> null);

        TextField busca = new TextField(conceito);
        HBox.setHgrow(busca, javafx.scene.layout.Priority.ALWAYS);
        Button buscar = new Button("Buscar");
        Label aviso = new Label();
        FlowPane opcoes = new FlowPane(10, 10);
        opcoes.setPadding(new Insets(6));
        ScrollPane rolagem = new ScrollPane(opcoes);
        rolagem.setFitToWidth(true);
        rolagem.setPrefViewportWidth(620);
        rolagem.setPrefViewportHeight(380);

        Runnable executarBusca = () -> {
            String termo = busca.getText() == null ? "" : busca.getText().trim();
            if (termo.isEmpty()) return;
            aviso.setText("Buscando...");
            opcoes.getChildren().clear();
            Task<List<ItemArasaac>> tarefa = new Task<>() {
                @Override
                protected List<ItemArasaac> call() throws Exception {
                    return gerador.opcoesDePictograma(termo);
                }
            };
            tarefa.setOnSucceeded(e -> {
                List<ItemArasaac> encontrados = tarefa.getValue();
                aviso.setText(encontrados.isEmpty()
                        ? "Nenhum pictograma encontrado. Tente outra palavra."
                        : "Clique no pictograma que você quer usar.");
                for (ItemArasaac opcao : encontrados) {
                    opcoes.getChildren().add(criarOpcao(opcao, () -> {
                        dialogo.setResult(opcao);
                        dialogo.close();
                    }));
                }
            });
            tarefa.setOnFailed(e -> aviso.setText("Erro: " + mensagem(tarefa.getException())));
            Thread thread = new Thread(tarefa, "opcoes");
            thread.setDaemon(true);
            thread.start();
        };
        buscar.setOnAction(e -> executarBusca.run());
        busca.setOnAction(e -> {
            e.consume();
            executarBusca.run();
        });

        HBox linhaDeBusca = new HBox(8, busca, buscar);
        linhaDeBusca.setAlignment(Pos.CENTER_LEFT);
        dialogo.getDialogPane().setContent(new VBox(10, linhaDeBusca, aviso, rolagem));
        executarBusca.run();

        dialogo.showAndWait().ifPresent(aoEscolher);
    }

    private VBox criarOpcao(ItemArasaac opcao, Runnable aoEscolher) {
        ImageView imagem = new ImageView(new Image(repositorio.enderecoDaImagem(opcao),
                TAMANHO_DA_OPCAO, TAMANHO_DA_OPCAO, true, true, true));
        imagem.setFitWidth(TAMANHO_DA_OPCAO);
        imagem.setFitHeight(TAMANHO_DA_OPCAO);
        imagem.setPreserveRatio(true);

        VBox caixa = new VBox(4, imagem, texto(opcao.palavra(), 12, false, "#3B2A5A", TAMANHO_DA_OPCAO + 10));
        caixa.setAlignment(Pos.TOP_CENTER);
        caixa.setPadding(new Insets(6));
        caixa.setPrefWidth(TAMANHO_DA_OPCAO + 20);
        caixa.setCursor(Cursor.HAND);
        String estilo = "-fx-background-color: " + Fitzgerald.fundo(opcao.tipo())
                + "; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-width: 3; -fx-border-color: ";
        caixa.setStyle(estilo + Fitzgerald.cor(opcao.tipo()) + ";");
        caixa.setOnMouseEntered(e -> caixa.setStyle(estilo + "#3B2A5A;"));
        caixa.setOnMouseExited(e -> caixa.setStyle(estilo + Fitzgerald.cor(opcao.tipo()) + ";"));
        caixa.setOnMouseClicked(e -> aoEscolher.run());
        if (!opcao.significado().isEmpty()) {
            Tooltip dica = new Tooltip(opcao.significado());
            dica.setWrapText(true);
            dica.setMaxWidth(300);
            Tooltip.install(caixa, dica);
        }
        return caixa;
    }

    private void gravarEdicao(String tema, AcaoDaMemoria acao) {
        List<ItemPrancha> copia = List.copyOf(itens);
        List<ItemPrancha> nucleoAtual = itensDoNucleo;
        gravador.submit(() -> {
            try {
                acao.executar();
                repositorio.salvar(tema, nucleoAtual, copia);
                Platform.runLater(() -> atualizarHistorico(tema));
            } catch (Exception e) {
                Platform.runLater(() -> mostrarStatus("Erro ao salvar a edição: " + mensagem(e), true));
            }
        });
    }

    @FunctionalInterface
    private interface AcaoDaMemoria {
        void executar() throws Exception;
    }

    private void carregarNucleo() {
        Task<List<ItemPrancha>> tarefa = new Task<>() {
            @Override
            protected List<ItemPrancha> call() throws Exception {
                return VocabularioNucleo.carregar(arasaac, repositorio);
            }
        };
        tarefa.setOnSucceeded(evento -> {
            itensDoNucleo = tarefa.getValue();
            renderizarNucleo();
        });
        tarefa.setOnFailed(evento -> nucleo.getChildren().setAll(
                texto("Não foi possível carregar o vocabulário básico: " + mensagem(tarefa.getException()),
                        13, false, "#C62828", 800)));
        Thread thread = new Thread(tarefa, "nucleo");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderizarNucleo() {
        nucleo.getChildren().clear();
        for (ItemPrancha item : itensDoNucleo) {
            nucleo.getChildren().add(criarCartao(item, TAMANHO_DO_NUCLEO,
                    () -> removerDoNucleo(item), () -> trocarPictogramaDoNucleo(item)));
        }
    }

    private int indiceNoNucleo(ItemPrancha item) {
        for (int i = 0; i < itensDoNucleo.size(); i++) {
            if (itensDoNucleo.get(i) == item) return i;
        }
        return -1;
    }

    /** Mostra o vocabulário básico novo e grava no disco (e nos HTMLs das pranchas já salvas). */
    private void aplicarNucleo(List<ItemPrancha> novo) {
        List<ItemPrancha> copia = List.copyOf(novo);
        itensDoNucleo = copia;
        renderizarNucleo();
        gravador.submit(() -> {
            try {
                repositorio.salvarNucleo(copia);
                repositorio.reexportarTodas(copia);
            } catch (Exception e) {
                Platform.runLater(() -> mostrarStatus("Erro ao salvar o vocabulário básico: " + mensagem(e), true));
            }
        });
    }

    private void removerDoNucleo(ItemPrancha item) {
        int indice = indiceNoNucleo(item);
        if (ocupado || indice < 0) return;
        List<ItemPrancha> novo = new ArrayList<>(itensDoNucleo);
        novo.remove(indice);
        aplicarNucleo(novo);
        mostrarStatus("\"" + item.conceito() + "\" removida do vocabulário básico.", false);
    }

    private void trocarPictogramaDoNucleo(ItemPrancha item) {
        if (ocupado || indiceNoNucleo(item) < 0) return;
        abrirDialogoDePictograma(item.conceito(), escolhido -> {
            int indice = indiceNoNucleo(item);
            if (indice < 0) return;
            int tipo = item.tipo() > 0 ? item.tipo() : escolhido.tipo();
            ItemArasaac comRotulo = new ItemArasaac(escolhido.id(), item.conceito(), tipo,
                    escolhido.significado(), escolhido.palavras());
            List<ItemPrancha> novo = new ArrayList<>(itensDoNucleo);
            novo.set(indice, ItemPrancha.encontrado(item.conceito(), comRotulo));
            aplicarNucleo(novo);
            mostrarStatus("Pictograma de \"" + item.conceito() + "\" trocado no vocabulário básico.", false);
        });
    }

    @FXML
    private void adicionarAoNucleo(ActionEvent evento) {
        evento.consume();
        String palavra = textoDe(campoNovaPalavraNucleo);
        if (ocupado) return;
        if (palavra.isEmpty()) {
            mostrarStatus("Digite a palavra que quer incluir no vocabulário básico.", true);
            return;
        }
        String rotulo = palavra.toUpperCase(Locale.ROOT);
        if (itensDoNucleo.stream().anyMatch(item -> MemoriaDePalavras.chave(item.conceito()).equals(MemoriaDePalavras.chave(rotulo)))) {
            mostrarStatus("\"" + rotulo + "\" já está no vocabulário básico.", true);
            return;
        }

        ocupado(true);
        Task<ItemPrancha> tarefa = new Task<>() {
            @Override
            protected ItemPrancha call() throws Exception {
                updateMessage("Buscando pictograma para \"" + palavra + "\"...");
                Optional<ItemArasaac> lembrado = memoria.escolha(palavra);
                List<ItemArasaac> achados = lembrado.isPresent() ? List.of(lembrado.get()) : arasaac.buscarExato(palavra);
                if (achados.isEmpty()) achados = arasaac.buscarAproximado(palavra);
                if (achados.isEmpty()) return ItemPrancha.naoEncontrado(rotulo);
                ItemArasaac achado = achados.get(0);
                return ItemPrancha.encontrado(rotulo, new ItemArasaac(achado.id(), rotulo, achado.tipo(),
                        achado.significado(), achado.palavras()));
            }
        };
        tarefa.setOnSucceeded(e -> {
            ocupado(false);
            ItemPrancha novoItem = tarefa.getValue();
            List<ItemPrancha> novo = new ArrayList<>(itensDoNucleo);
            novo.add(novoItem);
            aplicarNucleo(novo);
            campoNovaPalavraNucleo.clear();
            mostrarStatus(novoItem.encontrado()
                    ? "\"" + rotulo + "\" incluída no vocabulário básico."
                    : "\"" + rotulo + "\" incluída sem pictograma. Clique no cartão para escolher um.", !novoItem.encontrado());
        });
        tarefa.setOnFailed(e -> {
            ocupado(false);
            mostrarStatus("Erro: " + mensagem(tarefa.getException()), true);
        });
        iniciar(tarefa);
    }

    @FXML
    private void restaurarNucleo() {
        if (ocupado) return;
        javafx.scene.control.Alert aviso = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Isso apaga as suas mudanças e volta ao vocabulário básico original. Continuar?",
                ButtonType.OK, ButtonType.CANCEL);
        aviso.initOwner(grade.getScene().getWindow());
        aviso.setHeaderText("Restaurar vocabulário básico");
        if (aviso.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        ocupado(true);
        Task<List<ItemPrancha>> tarefa = new Task<>() {
            @Override
            protected List<ItemPrancha> call() throws Exception {
                updateMessage("Restaurando o vocabulário básico...");
                List<ItemPrancha> padrao = VocabularioNucleo.restaurarPadrao(arasaac, repositorio);
                repositorio.reexportarTodas(padrao);
                return padrao;
            }
        };
        tarefa.setOnSucceeded(e -> {
            ocupado(false);
            itensDoNucleo = List.copyOf(tarefa.getValue());
            renderizarNucleo();
            mostrarStatus("Vocabulário básico restaurado.", false);
        });
        tarefa.setOnFailed(e -> {
            ocupado(false);
            mostrarStatus("Erro: " + mensagem(tarefa.getException()), true);
        });
        iniciar(tarefa);
    }

    private void atualizarHistorico(String temaParaSelecionar) {
        atualizandoHistorico = true;
        historico.getItems().setAll(repositorio.listar());
        atualizandoHistorico = false;
        if (temaParaSelecionar != null) selecionarNoHistorico(temaParaSelecionar);
    }

    private void selecionarNoHistorico(String tema) {
        atualizandoHistorico = true;
        historico.getItems().stream()
                .filter(prancha -> prancha.tema().equalsIgnoreCase(tema))
                .findFirst()
                .ifPresentOrElse(prancha -> historico.getSelectionModel().select(prancha),
                        () -> historico.getSelectionModel().clearSelection());
        atualizandoHistorico = false;
    }

    private void limparPrancha(String tema) {
        grade.getChildren().clear();
        itens.clear();
        temaAtual = tema;
    }

    private void adicionar(ItemPrancha item) {
        if (item.situacao() == ItemPrancha.Situacao.REPETIDO) return;
        itens.add(item);
        grade.getChildren().add(criarCartao(item, TAMANHO_DA_IMAGEM, true));
    }

    private int indiceDe(ItemPrancha item) {
        for (int i = 0; i < itens.size(); i++) {
            if (itens.get(i) == item) return i;
        }
        return -1;
    }

    private Set<Integer> idsEmUso() {
        Set<Integer> usados = new HashSet<>();
        for (ItemPrancha item : itensDoNucleo) {
            if (item.encontrado()) usados.add(item.pictograma().id());
        }
        for (ItemPrancha item : itens) {
            if (item.encontrado()) usados.add(item.pictograma().id());
        }
        return usados;
    }

    private VBox criarCartao(ItemPrancha item, double tamanho, boolean editavel) {
        return criarCartao(item, tamanho,
                editavel ? () -> remover(item) : null,
                editavel ? () -> trocarPictograma(item) : null);
    }

    private VBox criarCartao(ItemPrancha item, double tamanho, Runnable aoRemover, Runnable aoTrocar) {
        boolean editavel = aoTrocar != null;
        boolean pequeno = tamanho < TAMANHO_DA_IMAGEM;
        VBox cartao = new VBox(pequeno ? 4 : 6);
        cartao.setAlignment(Pos.TOP_CENTER);
        cartao.setPadding(new Insets(pequeno ? 6 : 4, pequeno ? 6 : 10, pequeno ? 6 : 10, pequeno ? 6 : 10));
        cartao.setPrefWidth(tamanho + (pequeno ? 20 : 30));
        cartao.getStyleClass().add("cartao");
        if (pequeno) cartao.getStyleClass().add("cartao-pequeno");
        cartao.setStyle("-fx-background-color: " + Fitzgerald.fundo(item.tipo())
                + "; -fx-border-color: " + Fitzgerald.cor(item.tipo()) + ";");

        if (editavel) {
            Button botaoRemover = new Button("✕");
            botaoRemover.getStyleClass().add("botao-remover");
            botaoRemover.setCursor(Cursor.HAND);
            botaoRemover.setTooltip(new Tooltip("Remover da prancha"));
            botaoRemover.setOnAction(e -> aoRemover.run());
            botaoRemover.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
            HBox topo = new HBox(botaoRemover);
            topo.setAlignment(Pos.CENTER_RIGHT);
            topo.setMaxWidth(Double.MAX_VALUE);
            topo.visibleProperty().bind(modoUsar.not());
            topo.managedProperty().bind(modoUsar.not());
            cartao.getChildren().add(topo);
        }
        cartao.setCursor(Cursor.HAND);
        cartao.setOnMouseClicked(e -> {
            if (modoUsar.get()) tocar(item);
            else if (aoTrocar != null) aoTrocar.run();
        });

        if (item.encontrado()) {
            ItemArasaac pictograma = item.pictograma();
            ImageView imagem = new ImageView(new Image(repositorio.enderecoDaImagem(pictograma),
                    tamanho, tamanho, true, true, true));
            imagem.setFitWidth(tamanho);
            imagem.setFitHeight(tamanho);
            imagem.setPreserveRatio(true);
            cartao.getChildren().addAll(imagem,
                    texto(pictograma.palavra().toUpperCase(), pequeno ? 13 : 16, true, "#3B2A5A", tamanho + 10));
            if (!pequeno && !pictograma.palavra().equalsIgnoreCase(item.conceito())) {
                cartao.getChildren().add(texto("(" + item.conceito() + ")", 12, false, "#7B6A94", tamanho + 10));
            }

            String dica = Fitzgerald.nome(item.tipo());
            if (!pictograma.significado().isEmpty()) dica += "\n" + pictograma.significado();
            if (editavel) dica += "\n\nClique para trocar o pictograma.";
            Tooltip tooltip = new Tooltip(dica);
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(300);
            Tooltip.install(cartao, tooltip);
        } else {
            Label interrogacao = texto("?", pequeno ? 40 : 64, true, "#BBBBBB", tamanho);
            interrogacao.setMinHeight(tamanho);
            String aviso = item.situacao() == ItemPrancha.Situacao.ERRO ? "erro na busca" : "sem pictograma";
            cartao.getChildren().addAll(interrogacao,
                    texto(item.conceito().toUpperCase(), pequeno ? 13 : 16, true, "#555555", tamanho + 10),
                    texto(aviso, 12, false, "#C62828", tamanho + 10));
            String dica = item.erro() != null ? item.erro() : "Nenhum pictograma encontrado.";
            if (editavel) dica += "\nClique para escolher um pictograma.";
            Tooltip.install(cartao, new Tooltip(dica));
        }
        return cartao;
    }

    @FXML
    private void alternarModo() {
        modoUsar.set(!modoUsar.get());
        botaoModo.setText(modoUsar.get() ? "Modo: Usar (clique para editar)" : "Modo: Editar (clique para usar)");
        if (!modoUsar.get()) limparFrase();
    }

    @FXML
    private void falarFrase() {
        if (fraseItens.isEmpty()) return;
        StringBuilder texto = new StringBuilder();
        for (ItemPrancha item : fraseItens) texto.append(palavraFalada(item)).append(" ");
        falar(texto.toString().trim());
    }

    @FXML
    private void apagarUltima() {
        if (fraseItens.isEmpty()) return;
        int ultimo = fraseItens.size() - 1;
        fraseItens.remove(ultimo);
        frase.getChildren().remove(ultimo);
    }

    @FXML
    private void limparFrase() {
        fraseItens.clear();
        frase.getChildren().clear();
    }

    private void tocar(ItemPrancha item) {
        falar(palavraFalada(item));
        fraseItens.add(item);
        VBox mini = criarCartao(item, TAMANHO_DA_FRASE, false);
        mini.setOnMouseClicked(e -> {
            int posicao = frase.getChildren().indexOf(mini);
            if (posicao >= 0) {
                frase.getChildren().remove(posicao);
                fraseItens.remove(posicao);
            }
        });
        frase.getChildren().add(mini);
    }

    private static String palavraFalada(ItemPrancha item) {
        return item.encontrado() ? item.pictograma().palavra() : item.conceito();
    }

    private static void falar(String texto) {
        Thread thread = new Thread(() -> {
            try {
                String sistema = System.getProperty("os.name", "").toLowerCase();
                String seguro = texto.replace("'", "''");
                ProcessBuilder comando;
                if (sistema.contains("win")) {
                    comando = new ProcessBuilder("powershell", "-NoProfile", "-Command",
                            "Add-Type -AssemblyName System.Speech; "
                                    + "$v = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                                    + "$v.Speak('" + seguro + "')");
                } else if (sistema.contains("mac")) {
                    comando = new ProcessBuilder("say", "-v", "Luciana", texto);
                } else {
                    comando = new ProcessBuilder("espeak", "-v", "pt-br", texto);
                }
                comando.start().waitFor();
            } catch (Exception ignorado) {
                // sem voz neste computador: a prancha continua funcionando
            }
        }, "falar");
        thread.setDaemon(true);
        thread.start();
    }

    private static Label texto(String conteudo, double tamanho, boolean negrito, String cor, double larguraMaxima) {
        Label label = new Label(conteudo);
        label.setFont(Font.font("System", negrito ? FontWeight.BOLD : FontWeight.NORMAL, tamanho));
        label.setStyle("-fx-text-fill: " + cor + ";");
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setMaxWidth(larguraMaxima);
        return label;
    }

    private static String textoDe(TextField campo) {
        return campo.getText() == null ? "" : campo.getText().trim();
    }

    private String resumo() {
        long encontrados = itens.stream().filter(ItemPrancha::encontrado).count();
        long semPictograma = itens.stream().filter(item -> item.situacao() == ItemPrancha.Situacao.NAO_ENCONTRADO).count();
        long comErro = itens.stream().filter(item -> item.situacao() == ItemPrancha.Situacao.ERRO).count();

        StringBuilder texto = new StringBuilder("Prancha pronta: " + encontrados + " pictogramas.");
        if (semPictograma > 0) texto.append(" ").append(semPictograma).append(" sem pictograma.");
        if (comErro > 0) texto.append(" ").append(comErro).append(" com erro (passe o mouse para ver).");
        return texto.toString();
    }

    private void ocupado(boolean sim) {
        ocupado = sim;
        botaoGerar.setDisable(sim);
        botaoNovamente.setDisable(sim || temaAtual == null);
        botaoAdicionar.setDisable(sim || temaAtual == null);
        campoNovaPalavra.setDisable(sim || temaAtual == null);
        campoTema.setDisable(sim);
        campoNovaPalavraNucleo.setDisable(sim);
        botaoAdicionarNucleo.setDisable(sim);
        botaoRestaurarNucleo.setDisable(sim);
        historico.setDisable(sim);
        progresso.setVisible(sim);
    }

    private void mostrarStatus(String mensagem, boolean erro) {
        status.textProperty().unbind();
        status.setText(mensagem);
        status.setStyle(erro ? "-fx-text-fill: #C62828;" : "-fx-text-fill: #3B2A5A;");
    }

    private static String mensagem(Throwable erro) {
        if (erro == null) return "erro desconhecido.";
        return erro.getMessage() == null ? erro.getClass().getSimpleName() : erro.getMessage();
    }

    private void iniciar(Task<?> tarefa) {
        status.setStyle("-fx-text-fill: #3B2A5A;");
        status.textProperty().bind(tarefa.messageProperty());
        Thread thread = new Thread(tarefa, "prancha");
        thread.setDaemon(true);
        thread.start();
    }
}