package sistema;

final class TempoSequencial {
    private TempoSequencial() {
    }

    static void esperar(int milissegundos) {
        long fim = System.nanoTime() + (milissegundos * 1_000_000L);
        while (System.nanoTime() < fim) {
            // Espera ocupada para simular processamento sem criar ou usar threads.
        }
    }
}
