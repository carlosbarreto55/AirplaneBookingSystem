package sistema;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

public final class SistemaPassagensSequencial {
    private static long proximoId = 1L;
    private static int recebidos;
    private static int concluidos;
    private static int confirmados;
    private static int recusados;

    private SistemaPassagensSequencial() {
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int assentosPorVoo = parametroInicial(args, 0, "Assentos por voo", 40, 1, scanner);
        int tempoMinimoMs = parametroInicial(args, 1, "Tempo minimo de atendimento em ms", 200, 0, scanner);
        int tempoMaximoMs = parametroInicial(args, 2, "Tempo maximo de atendimento em ms", 700, tempoMinimoMs, scanner);

        List<Voo> voos = CatalogoVoos.criarVoos(assentosPorVoo);
        Random random = new Random();

        Log.linha();
        Log.info("Sistema", "VERSAO SEQUENCIAL iniciada. Um unico atendente processa tudo.");
        Log.info("Sistema", "Enquanto um pedido esta sendo processado, o menu fica bloqueado.");
        imprimirAjuda();

        boolean executando = true;
        while (executando) {
            System.out.print("seq> ");
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
                comprar(voos, partes, tempoMinimoMs, tempoMaximoMs, random);
            } else if ("lote".equals(comando)) {
                executarLote(voos, partes, tempoMinimoMs, tempoMaximoMs, random);
            } else if ("status".equals(comando)) {
                Log.info("Sistema", resumo());
            } else if ("sair".equals(comando)) {
                executando = false;
            } else {
                Log.info("Sistema", "Comando desconhecido. Digite ajuda.");
            }
        }

        Log.info("Sistema", "Versao sequencial encerrada. " + resumo());
    }

    private static void imprimirAjuda() {
        Log.linha();
        System.out.println("Comandos da versao sequencial:");
        System.out.println("  listar");
        System.out.println("  mapa <voo>");
        System.out.println("  comprar <passageiro_sem_espaco> <voo> <assento>");
        System.out.println("  lote <quantidade>");
        System.out.println("  status");
        System.out.println("  ajuda");
        System.out.println("  sair");
        Log.linha();
    }

    private static void comprar(List<Voo> voos, String[] partes, int tempoMinimoMs,
            int tempoMaximoMs, Random random) {
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
        processarPedido(voos, pedido, tempoMinimoMs, tempoMaximoMs, random);
    }

    private static void executarLote(List<Voo> voos, String[] partes, int tempoMinimoMs,
            int tempoMaximoMs, Random random) {
        if (partes.length != 2) {
            Log.info("Sistema", "Uso: lote <quantidade>");
            return;
        }

        Integer quantidade = inteiro(partes[1]);
        if (quantidade == null || quantidade.intValue() <= 0) {
            Log.info("Sistema", "Quantidade invalida.");
            return;
        }

        Log.info("Atendente-Unico", "Iniciando lote sequencial com " + quantidade + " pedidos.");
        long inicio = System.currentTimeMillis();
        for (int i = 0; i < quantidade.intValue(); i++) {
            PedidoCompra pedido = GeradorPedidos.aleatorio(proximoId++, voos, random, "lote");
            processarPedido(voos, pedido, tempoMinimoMs, tempoMaximoMs, random);
        }
        long duracao = System.currentTimeMillis() - inicio;
        Log.info("Atendente-Unico", "Lote sequencial finalizado em " + duracao + " ms. " + resumo());
    }

    private static void processarPedido(List<Voo> voos, PedidoCompra pedido, int tempoMinimoMs,
            int tempoMaximoMs, Random random) {
        recebidos++;
        Log.info("Atendente-Unico", "Recebeu " + pedido.descricaoCurta());
        int tempo = tempoMinimoMs + random.nextInt(tempoMaximoMs - tempoMinimoMs + 1);
        Log.info("Atendente-Unico", "Validando pagamento e assento por " + tempo + " ms.");
        TempoSequencial.esperar(tempo);

        Voo voo = CatalogoVoos.procurar(voos, pedido.codigoVoo());
        boolean sucesso = false;
        if (voo == null) {
            Log.info("Atendente-Unico", "RECUSADO pedido #" + pedido.id() + ": voo inexistente.");
        } else {
            ResultadoReserva resultado = voo.reservar(pedido.passageiro(), pedido.assento());
            sucesso = resultado.sucesso();
            if (sucesso) {
                Log.info("Atendente-Unico", "CONFIRMADO pedido #" + pedido.id() + ": " + resultado.mensagem());
            } else {
                Log.info("Atendente-Unico", "RECUSADO pedido #" + pedido.id() + ": " + resultado.mensagem());
            }
        }

        concluidos++;
        if (sucesso) {
            confirmados++;
        } else {
            recusados++;
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

    private static String resumo() {
        return "recebidos=" + recebidos
                + ", concluidos=" + concluidos
                + ", confirmados=" + confirmados
                + ", recusados=" + recusados + ".";
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
