package sistema;

final class TempoThreads {
    private TempoThreads() {
    }

    static void dormir(int milissegundos) throws InterruptedException {
        Thread.sleep(milissegundos);
    }
}
