package sistema;

final class Estatisticas {
    private int totalRecebidos;
    private int totalConcluidos;
    private int sucessos;
    private int falhas;

    synchronized void registrarRecebido() {
        totalRecebidos++;
        notifyAll();
    }

    synchronized void registrarConclusao(boolean sucesso) {
        totalConcluidos++;
        if (sucesso) {
            sucessos++;
        } else {
            falhas++;
        }
        notifyAll();
    }

    synchronized int totalRecebidos() {
        return totalRecebidos;
    }

    synchronized void esperarConclusoesAte(int alvo) throws InterruptedException {
        while (totalConcluidos < alvo) {
            wait();
        }
    }

    synchronized String resumo(int tamanhoFila) {
        return "recebidos=" + totalRecebidos
                + ", concluidos=" + totalConcluidos
                + ", confirmados=" + sucessos
                + ", recusados=" + falhas
                + ", na fila=" + tamanhoFila + ".";
    }
}
