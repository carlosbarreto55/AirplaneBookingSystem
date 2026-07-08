# SistemaPassagensAereas

Trabalho pratico de Sistemas Operacionais sobre programacao concorrente e paralela.
O projeto implementa uma simulacao de sistema de venda de passagens aereas em duas versoes:

- `SistemaPassagensSequencial`: atendimento tradicional, com um unico fluxo de execucao.
- `SistemaPassagensThreads`: atendimento paralelo, com uma fila compartilhada e varias threads atendentes.

Nao ha frameworks, bibliotecas externas, Maven ou Gradle. O projeto usa apenas Java puro e a classe
`Thread`, alem de `synchronized`, `wait` e `notifyAll` para coordenacao.

## Compilar

Execute dentro da pasta do projeto:

```bash
javac -d out src/sistema/*.java
```

## Executar a versao sequencial

```bash
java -cp out sistema.SistemaPassagensSequencial
```

Tambem e possivel informar parametros:

```bash
java -cp out sistema.SistemaPassagensSequencial <assentos_por_voo> <tempo_min_ms> <tempo_max_ms>
```

Exemplo:

```bash
java -cp out sistema.SistemaPassagensSequencial 50 200 700
```

## Executar a versao com threads

```bash
java -cp out sistema.SistemaPassagensThreads
```

Tambem e possivel informar parametros:

```bash
java -cp out sistema.SistemaPassagensThreads <threads> <assentos_por_voo> <tempo_min_ms> <tempo_max_ms>
```

Exemplo:

```bash
java -cp out sistema.SistemaPassagensThreads 4 50 200 700
```

## Comandos interativos

As duas versoes aceitam:

```text
listar
mapa <voo>
comprar <passageiro_sem_espaco> <voo> <assento>
lote <quantidade>
status
ajuda
sair
```

A versao paralela tambem aceita:

```text
lote_async <quantidade>
```

Esse comando coloca os pedidos na fila e devolve o prompt imediatamente, enquanto as threads
continuam trabalhando em segundo plano.

## Sugestao de demonstracao

1. Compile o projeto.
2. Rode a versao sequencial com `java -cp out sistema.SistemaPassagensSequencial 50 200 700`.
3. Digite `lote 20` e observe que um unico atendente processa todos os pedidos, um por vez.
4. Rode a versao paralela com `java -cp out sistema.SistemaPassagensThreads 4 50 200 700`.
5. Digite `lote 20` e observe os logs de `Atendente-1`, `Atendente-2`, `Atendente-3` e
   `Atendente-4` trabalhando ao mesmo tempo.
6. Digite `lote_async 20`, depois `status`, para ver que o menu continua disponivel enquanto as
   threads processam a fila.

O ganho esperado aparece no tempo total do comando `lote`: com varios atendentes, o lote tende a
terminar em menos tempo do que na versao sequencial, porque as validacoes de pagamento e reserva sao
processadas em paralelo. A reserva do assento e protegida com `synchronized`, entao duas threads nao
conseguem vender o mesmo assento ao mesmo tempo.
