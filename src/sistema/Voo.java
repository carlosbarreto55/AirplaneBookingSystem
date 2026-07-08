package sistema;

final class Voo {
    private final String codigo;
    private final String origem;
    private final String destino;
    private final double preco;
    private final String[] passageirosPorAssento;

    Voo(String codigo, String origem, String destino, int totalAssentos, double preco) {
        this.codigo = codigo;
        this.origem = origem;
        this.destino = destino;
        this.preco = preco;
        this.passageirosPorAssento = new String[totalAssentos];
    }

    String codigo() {
        return codigo;
    }

    int totalAssentos() {
        return passageirosPorAssento.length;
    }

    synchronized ResultadoReserva reservar(String passageiro, int assento) {
        if (assento < 1 || assento > passageirosPorAssento.length) {
            return new ResultadoReserva(false, "Assento " + assento + " nao existe no voo " + codigo + ".");
        }

        int indice = assento - 1;
        if (passageirosPorAssento[indice] != null) {
            return new ResultadoReserva(false,
                    "Assento " + formatarAssento(assento) + " do voo " + codigo
                            + " ja foi vendido para " + passageirosPorAssento[indice] + ".");
        }

        passageirosPorAssento[indice] = passageiro;
        return new ResultadoReserva(true,
                "Bilhete confirmado: voo " + codigo + ", assento " + formatarAssento(assento)
                        + ", passageiro " + passageiro + ".");
    }

    synchronized int vendidos() {
        int total = 0;
        for (int i = 0; i < passageirosPorAssento.length; i++) {
            if (passageirosPorAssento[i] != null) {
                total++;
            }
        }
        return total;
    }

    synchronized int livres() {
        return passageirosPorAssento.length - vendidos();
    }

    synchronized String resumo() {
        return String.format("%-5s %-14s -> %-14s | assentos: %2d livres / %2d vendidos | R$ %.2f",
                codigo, origem, destino, livres(), vendidos(), preco);
    }

    synchronized String mapaAssentos() {
        StringBuilder saida = new StringBuilder();
        saida.append("Mapa do voo ").append(codigo).append(" (L = livre, X = vendido)\n");
        for (int i = 0; i < passageirosPorAssento.length; i++) {
            String estado = passageirosPorAssento[i] == null ? "L" : "X";
            saida.append("[").append(formatarAssento(i + 1)).append(":").append(estado).append("] ");
            if ((i + 1) % 10 == 0) {
                saida.append('\n');
            }
        }
        return saida.toString();
    }

    private String formatarAssento(int assento) {
        return String.format("%02d", assento);
    }
}
