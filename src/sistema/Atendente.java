package sistema;

import java.util.List;
import java.util.Random;

final class Atendente extends Thread {
    private final FilaPedidos fila;
    private final List<Voo> voos;
    private final Estatisticas estatisticas;
    private final int tempoMinimoMs;
    private final int tempoMaximoMs;
    private final Random random;

    Atendente(int numero, FilaPedidos fila, List<Voo> voos, Estatisticas estatisticas,
            int tempoMinimoMs, int tempoMaximoMs) {
        super("Atendente-" + numero);
        this.fila = fila;
        this.voos = voos;
        this.estatisticas = estatisticas;
        this.tempoMinimoMs = tempoMinimoMs;
        this.tempoMaximoMs = tempoMaximoMs;
        this.random = new Random(System.nanoTime() + (numero * 97L));
    }

    @Override
    public void run() {
        Log.info(getName(), "Thread iniciada e aguardando pedidos.");
        try {
            while (true) {
                PedidoCompra pedido = fila.retirar();
                if (pedido == null) {
                    Log.info(getName(), "Fila fechada e vazia. Encerrando atendimento.");
                    return;
                }

                boolean sucesso = processar(pedido);
                estatisticas.registrarConclusao(sucesso);
            }
        } catch (InterruptedException e) {
            Log.info(getName(), "Interrompida durante o processamento.");
            interrupt();
        }
    }

    private boolean processar(PedidoCompra pedido) throws InterruptedException {
        Log.info(getName(), "Recebeu " + pedido.descricaoCurta());
        int tempo = tempoAleatorio();
        Log.info(getName(), "Validando pagamento e assento por " + tempo + " ms.");
        TempoThreads.dormir(tempo);

        Voo voo = CatalogoVoos.procurar(voos, pedido.codigoVoo());
        if (voo == null) {
            Log.info(getName(), "RECUSADO pedido #" + pedido.id() + ": voo inexistente.");
            return false;
        }

        ResultadoReserva resultado = voo.reservar(pedido.passageiro(), pedido.assento());
        if (resultado.sucesso()) {
            Log.info(getName(), "CONFIRMADO pedido #" + pedido.id() + ": " + resultado.mensagem());
        } else {
            Log.info(getName(), "RECUSADO pedido #" + pedido.id() + ": " + resultado.mensagem());
        }
        return resultado.sucesso();
    }

    private int tempoAleatorio() {
        return tempoMinimoMs + random.nextInt(tempoMaximoMs - tempoMinimoMs + 1);
    }
}
