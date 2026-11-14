# Amanda AAC

Gera uma prancha de CAA (pictogramas ARASAAC) a partir de um tema.

## Requisitos
- JDK 21 ou superior
- Maven
- [Ollama](https://ollama.com) rodando em `127.0.0.1:11434` com os modelos:
  - `ollama pull hf.co/tardellirs/aac-board-generator-770m-ptbr-GGUF:Q4_K_M`
  - `ollama pull hf.co/tardellirs/aac-pictogram-retriever-ptbr-GGUF:Q4_K_M`

## Como rodar
```
mvn compile exec:java
```
Ou abra a pasta no IntelliJ e rode a classe `org.example.Main`.
