package sistema;

final class ResultadoReserva {
    private final boolean sucesso;
    private final String mensagem;

    ResultadoReserva(boolean sucesso, String mensagem) {
        this.sucesso = sucesso;
        this.mensagem = mensagem;
    }

    boolean sucesso() {
        return sucesso;
    }

    String mensagem() {
        return mensagem;
    }
}
