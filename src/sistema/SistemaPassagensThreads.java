package sistema;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

public final class SistemaPassagensThreads {
    private static long proximoId = 1L;

    private SistemaPassagensThreads() {
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int quantidadeThreads = parametroInicial(args, 0, "Quantidade de threads atendentes", 4, 1, scanner);
        int assentosPorVoo = parametroInicial(args, 1, "Assentos por voo", 40, 1, scanner);
        int tempoMinimoMs = parametroInicial(args, 2, "Tempo minimo de atendimento em ms", 200, 0, scanner);
        int tempoMaximoMs = parametroInicial(args, 3, "Tempo maximo de atendimento em ms", 700, tempoMinimoMs, scanner);

        List<Voo> voos = CatalogoVoos.criarVoos(assentosPorVoo);
        FilaPedidos fila = new FilaPedidos();
        Estatisticas estatisticas = new Estatisticas();
        Atendente[] atendentes = iniciarAtendentes(quantidadeThreads, fila, voos, estatisticas,
                tempoMinimoMs, tempoMaximoMs);
        Random random = new Random();

        Log.linha();
        Log.info("Sistema", "VERSAO PARALELA iniciada com " + quantidadeThreads + " threads atendentes.");
        Log.info("Sistema", "O menu continua aceitando comandos enquanto as threads processam pedidos.");
        imprimirAjuda();

        try {
            executarLoop(scanner, voos, fila, estatisticas, random);
        } catch (InterruptedException e) {
            Log.info("Sistema", "Thread principal interrompida. Encerrando.");
            Thread.currentThread().interrupt();
        } finally {
            encerrar(fila, atendentes, estatisticas);
        }
    }

    private static Atendente[] iniciarAtendentes(int quantidadeThreads, FilaPedidos fila, List<Voo> voos,
            Estatisticas estatisticas, int tempoMinimoMs, int tempoMaximoMs) {
        Atendente[] atendentes = new Atendente[quantidadeThreads];
        for (int i = 0; i < quantidadeThreads; i++) {
            atendentes[i] = new Atendente(i + 1, fila, voos, estatisticas, tempoMinimoMs, tempoMaximoMs);
            atendentes[i].start();
        }
        return atendentes;
    }

    private static void executarLoop(Scanner scanner, List<Voo> voos, FilaPedidos fila,
            Estatisticas estatisticas, Random random) throws InterruptedException {
        boolean executando = true;
        while (executando) {
            System.out.print("threads> ");
            if (!scanner.hasNextLine()) {
                break;
            }

            String linha = scanner.nextLine().trim();
            if (linha.length() == 0) {
                continue;
            }

            String[] partes = linha.split("\\s+");
            String comando = partes[0].toLowerCase(Locale.ROOT);

            if ("ajuda".equals(comando)) {
                imprimirAjuda();
            } else if ("listar".equals(comando)) {
                System.out.print(CatalogoVoos.listar(voos));
            } else if ("mapa".equals(comando)) {
                mostrarMapa(voos, partes);
            } else if ("comprar".equals(comando)) {
                comprar(fila, estatisticas, partes);
            } else if ("lote".equals(comando)) {
                executarLote(voos, fila, estatisticas, partes, random, true);
            } else if ("lote_async".equals(comando)) {
                executarLote(voos, fila, estatisticas, partes, random, false);
            } else if ("status".equals(comando)) {
                Log.info("Sistema", estatisticas.resumo(fila.tamanho()));
            } else if ("sair".equals(comando)) {
                executando = false;
            } else {
                Log.info("Sistema", "Comando desconhecido. Digite ajuda.");
            }
        }
    }

    private static void imprimirAjuda() {
        Log.linha();
        System.out.println("Comandos da versao paralela:");
        System.out.println("  listar");
        System.out.println("  mapa <voo>");
        System.out.println("  comprar <passageiro_sem_espaco> <voo> <assento>");
        System.out.println("  lote <quantidade>          (envia pedidos e aguarda todas as threads terminarem)");
        System.out.println("  lote_async <quantidade>    (envia pedidos e devolve o prompt imediatamente)");
        System.out.println("  status");
        System.out.println("  ajuda");
        System.out.println("  sair");
        Log.linha();
    }

    private static void comprar(FilaPedidos fila, Estatisticas estatisticas, String[] partes) {
        if (partes.length != 4) {
            Log.info("Sistema", "Uso: comprar <passageiro_sem_espaco> <voo> <assento>");
            return;
        }

        Integer assento = inteiro(partes[3]);
        if (assento == null) {
            Log.info("Sistema", "Assento invalido.");
            return;
        }

        PedidoCompra pedido = new PedidoCompra(proximoId++, partes[1], partes[2], assento.intValue(), "manual");
        estatisticas.registrarRecebido();
        fila.adicionar(pedido);
        Log.info("Principal", "Pedido manual entregue a fila. O menu ja pode receber outro comando.");
    }

    private static void executarLote(List<Voo> voos, FilaPedidos fila, Estatisticas estatisticas,
            String[] partes, Random random, boolean aguardar) throws InterruptedException {
        if (partes.length != 2) {
            Log.info("Sistema", "Uso: " + partes[0] + " <quantidade>");
            return;
        }

        Integer quantidade = inteiro(partes[1]);
        if (quantidade == null || quantidade.intValue() <= 0) {
            Log.info("Sistema", "Quantidade invalida.");
            return;
        }

        int alvoConclusoes = estatisticas.totalRecebidos() + quantidade.intValue();
        long inicio = System.currentTimeMillis();
        Log.info("Principal", "Enfileirando lote com " + quantidade + " pedidos.");
        for (int i = 0; i < quantidade.intValue(); i++) {
            PedidoCompra pedido = GeradorPedidos.aleatorio(proximoId++, voos, random, "lote");
            estatisticas.registrarRecebido();
            fila.adicionar(pedido);
        }

        if (aguardar) {
            Log.info("Principal", "Aguardando conclusao do lote pelas threads...");
            estatisticas.esperarConclusoesAte(alvoConclusoes);
            long duracao = System.currentTimeMillis() - inicio;
            Log.info("Principal", "Lote paralelo finalizado em " + duracao + " ms. "
                    + estatisticas.resumo(fila.tamanho()));
        } else {
            Log.info("Principal", "Lote enviado em modo assincrono. Use status para acompanhar.");
        }
    }

    private static void mostrarMapa(List<Voo> voos, String[] partes) {
        if (partes.length != 2) {
            Log.info("Sistema", "Uso: mapa <voo>");
            return;
        }

        Voo voo = CatalogoVoos.procurar(voos, partes[1]);
        if (voo == null) {
            Log.info("Sistema", "Voo nao encontrado.");
            return;
        }
        System.out.println(voo.mapaAssentos());
    }

    private static void encerrar(FilaPedidos fila, Atendente[] atendentes, Estatisticas estatisticas) {
        fila.fechar();
        for (int i = 0; i < atendentes.length; i++) {
            try {
                atendentes[i].join();
            } catch (InterruptedException e) {
                Log.info("Sistema", "Interrompido ao aguardar " + atendentes[i].getName() + ".");
                Thread.currentThread().interrupt();
                break;
            }
        }
        Log.info("Sistema", "Versao paralela encerrada. " + estatisticas.resumo(fila.tamanho()));
    }

    private static int parametroInicial(String[] args, int indice, String nome, int padrao, int minimo,
            Scanner scanner) {
        if (args.length > indice) {
            Integer valor = inteiro(args[indice]);
            if (valor != null && valor.intValue() >= minimo) {
                return valor.intValue();
            }
            Log.info("Sistema", "Parametro invalido para " + nome + ". Usando " + padrao + ".");
            return padrao;
        }

        System.out.print(nome + " [" + padrao + "]: ");
        if (!scanner.hasNextLine()) {
            return padrao;
        }
        String linha = scanner.nextLine().trim();
        if (linha.length() == 0) {
            return padrao;
        }
        Integer valor = inteiro(linha);
        if (valor == null || valor.intValue() < minimo) {
            Log.info("Sistema", "Valor invalido. Usando " + padrao + ".");
            return padrao;
        }
        return valor.intValue();
    }

    private static Integer inteiro(String texto) {
        try {
            return Integer.valueOf(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
