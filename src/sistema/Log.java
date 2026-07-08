package sistema;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

final class Log {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Log() {
    }

    static synchronized void info(String origem, String mensagem) {
        System.out.printf("[%s] %-18s | %s%n", LocalTime.now().format(FORMATO), origem, mensagem);
    }

    static synchronized void linha() {
        System.out.println("--------------------------------------------------------------------------");
    }
}
