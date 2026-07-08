package sistema;

import java.util.Locale;

final class PedidoCompra {
    private final long id;
    private final String passageiro;
    private final String codigoVoo;
    private final int assento;
    private final String origem;

    PedidoCompra(long id, String passageiro, String codigoVoo, int assento, String origem) {
        this.id = id;
        this.passageiro = passageiro;
        this.codigoVoo = codigoVoo.toUpperCase(Locale.ROOT);
        this.assento = assento;
        this.origem = origem;
    }

    long id() {
        return id;
    }

    String passageiro() {
        return passageiro;
    }

    String codigoVoo() {
        return codigoVoo;
    }

    int assento() {
        return assento;
    }

    String descricaoCurta() {
        return "#" + id + " [" + origem + "] " + passageiro + " solicita voo " + codigoVoo
                + ", assento " + assento + ".";
    }
}
