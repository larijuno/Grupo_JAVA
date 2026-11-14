# Manual de Uso do Sistema de AAC🧩

**💡Como rodar em outro PC e como usar?**

O programa monta **pranchas de comunicação alternativa (CAA)** com pictogramas do [ARASAAC](https://arasaac.org) a partir de um tema. Depois de montada, a prancha pode ser **editada**, **impressa** ou **usada na tela**, com o computador **falando** as palavras ao clicar nos cartões.

## 📚Índice

1. [O que instalar](#1-o-que-instalar)
2. [Instalar o Ollama](#2-instalar-o-ollama)
3. [Baixar os dois modelos de IA](#3-baixar-os-dois-modelos-de-ia)
4. [Abrir o projeto no IntelliJ](#4-abrir-o-projeto-no-intellij)
5. [Rodar](#5-rodar)
6. [Como usar](#6-como-usar)
7. [Créditos](#7-créditos)

---

## 📌1. O que instalar

| Item | Para quê | Onde baixar |
|---|---|---|
| **JDK 21 ou superior** | Rodar o programa | [adoptium.net](https://adoptium.net) |
| **IntelliJ IDEA** | Abrir e rodar o projeto  | [jetbrains.com/idea/download](https://www.jetbrains.com/idea/download) |
| **Ollama** | IA que roda no seu computador | [ollama.com/download](https://ollama.com/download) |
| **Internet** | Buscar os pictogramas no ARASAAC | — |
| **Voz em português** *(recomendado)* | O computador falar as palavras em português | Veja o [passo 6.6](#66-ouvir-a-prancha) |

---

## 📌2. Instalar o Ollama

1. Baixe e instale o Ollama.
2. Abra o Ollama. Ele fica com um ícone perto do relógio do Windows.

---

## 📌3. Baixar os dois modelos de IA

Abra um terminal (cmd ou PowerShell) e rode os **dois** comandos (baixam uns 670 MB no total):

```
ollama pull hf.co/tardellirs/aac-board-generator-770m-ptbr-GGUF:Q4_K_M
ollama pull hf.co/tardellirs/aac-pictogram-retriever-ptbr-GGUF:Q4_K_M
```

Para conferir, rode:

```
ollama list
```

Os dois modelos têm que aparecer na lista!

---

## 📌4. Abrir o projeto no IntelliJ

1. Copie a pasta **AAC_final** para o PC. Se quiser levar as pranchas já salvas, copie junto a pasta `pranchas`, que fica dentro dela.
2. No IntelliJ: **File > Open** > selecione a pasta `AAC_final` > **OK**.

---

## 📌5. Rodar

1. Confirme que o **Ollama está aberto**.
2. Abra `src/main/java/org/example/App.java`.
3. Clique na seta verde ao lado de `public class App` > **Run ▷**.

> **IMPORTANTE:** rode a classe `App`.

## 📌6. Como usar

O programa tem **dois modos**, que se alternam no botão do topo:

| Modo | Utilidade | O que aparece |
|---|---|---|
| **Modo: Editar** | Montar e arrumar a prancha | Remover (✕), trocar imagem, adicionar palavras, vocabulário básico |
| **Modo: Usar** | Comunicar | Cartões que falam ao clicar e a barra da frase |

O botão mostra o modo atual e diz qual será o próximo: **"Modo: Editar (clique para usar)"** e **"Modo: Usar (clique para editar)"**.

### 📌6.1 Gerar uma prancha

1. Digite o **tema** (ex.: praia, parquinho, supermercado) e clique em **Gerar prancha** (ou aperte Enter).
2. Na primeira vez, leva de 10 a 20 segundos. Os cartões vão aparecendo um por um, e o texto de status mostra o andamento.
3. Se o tema já tiver sido salvo, o programa abre a versão salva (com as suas edições) na hora, **sem internet e sem IA**. Para criar uma nova do zero, clique em **Gerar de novo**.

### 🎨6.2 Entender os cartões

As **cores das bordas**

| Cor | Tipo de palavra | Exemplos |
|---|---|---|
| 🟡 Amarelo | Pessoas e nomes | EU, VOCÊ, NÓS |
| 🟠 Laranja | Substantivos | BALDE, AREIA |
| 🟢 Verde | Verbos | QUERO, COMER, BRINCAR |
| 🔵 Azul | Descritivos | GRANDE, MAIS, BOM |
| 🩷 Rosa | Social | SIM, NÃO, OLÁ, TCHAU, OBRIGADO |

- No topo fica o **vocabulário básico** (cerca de 30 palavras, como EU, VOCÊ, QUERO, NÃO QUERO, MAIS, AJUDA, SIM, NÃO, ACABOU), que aparece em **todas** as pranchas.

### 🖍️6.3 Editar a prancha (Modo: Editar)

- **✕ no canto do cartão:** remove a palavra. Ela não volta quando você gerar esse tema de novo.
- **Clique no cartão:** abre as opções para trocar a imagem. A escolha vale para essa palavra **em todas as pranchas**.
- **Adicionar palavra:** inclui um item que a IA não sugeriu.
- **Vocabulário básico:** também pode ser editado, e dá para restaurar o padrão original.

Tudo é salvo automaticamente a cada mudança!

### 💬6.4 Usar a prancha para se comunicar (Modo: Usar)

1. Clique no botão do topo para entrar no **Modo: Usar**. As ferramentas de edição somem e aparece a **barra da frase**, uma faixa branca com borda verde.
2. **Clique em um cartão:** o computador **fala a palavra** na hora, e ela entra na barra da frase como um cartãozinho.
3. Continue clicando para montar a frase (por exemplo, EU + QUERO + COMER).
4. Clique em **▶ Falar** para o computador falar a **frase inteira**.

Os botões da barra da frase:

| Botão | O que faz |
|---|---|
| **▶ Falar** | Fala a frase completa |
| **⌫ Apagar última** | Tira a última palavra da frase |
| **Limpar** | Esvazia a frase |

Ao voltar para o **Modo: Editar**, a frase é limpa automaticamente.

### 💾6.5 Pranchas salvas

Toda prancha é salva automaticamente e aparece em **Pranchas salvas**, à esquerda. Clique em um tema para abrir de novo. Abrir uma prancha salva leva menos de 1 segundo e não precisa de internet.

### 🎧6.6 Ouvir a prancha

A fala **não usa áudio gravado nem internet**. O programa pede ao sistema do computador para ler o texto em voz alta:

| Sistema | Como fala |
|---|---|
| **Windows** | Voz do próprio Windows (via PowerShell) |
| **Mac** | Comando `say`, com a voz "Luciana" (português do Brasil) |
| **Linux** | Programa `espeak` em português do Brasil. Instale com `sudo apt install espeak` |

### 🖨️6.7 Imprimir

1. Abra a pasta do projeto e entre em `pranchas` e depois na pasta do tema desejado. Por exemplo: `pranchas/praia`.
2. Dê dois cliques no arquivo `prancha.html`. Ele será aberto no navegador (Chrome, Edge, etc.).
3. Pressione `Ctrl + P` para abrir a janela de impressão.
4. Escolha a impressora desejada ou selecione **Salvar como PDF** caso queira gerar um arquivo.
5. Clique em **Imprimir**.

---

## 📌7. Créditos

Pictogramas: Sergio Palao. Origem: [ARASAAC](https://arasaac.org). Licença: CC BY-NC-SA. Propriedade: Governo de Aragão (Espanha).
