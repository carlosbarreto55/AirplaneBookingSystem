package sistema;

import java.util.ArrayDeque;
import java.util.Deque;

final class FilaPedidos {
    private final Deque<PedidoCompra> pedidos = new ArrayDeque<PedidoCompra>();
    private boolean fechada;

    synchronized void adicionar(PedidoCompra pedido) {
        if (fechada) {
            throw new IllegalStateException("A fila ja foi fechada.");
        }
        pedidos.addLast(pedido);
        Log.info("Fila", "Pedido enfileirado: " + pedido.descricaoCurta() + " Tamanho=" + pedidos.size());
        notifyAll();
    }

    synchronized PedidoCompra retirar() throws InterruptedException {
        while (pedidos.isEmpty() && !fechada) {
            wait();
        }
        if (pedidos.isEmpty()) {
            return null;
        }
        return pedidos.removeFirst();
    }

    synchronized int tamanho() {
        return pedidos.size();
    }

    synchronized void fechar() {
        fechada = true;
        notifyAll();
    }
}
