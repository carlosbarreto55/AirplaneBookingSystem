package sistema;

import java.util.List;
import java.util.Random;

final class GeradorPedidos {
    private static final String[] NOMES = {
            "Ana", "Bruno", "Carla", "Diego", "Elisa", "Fabio", "Giovana", "Hugo",
            "Iara", "Joao", "Katia", "Lucas", "Marina", "Nina", "Otavio", "Paula",
            "Renato", "Sofia", "Tiago", "Vanessa"
    };

    private GeradorPedidos() {
    }

    static PedidoCompra aleatorio(long id, List<Voo> voos, Random random, String origem) {
        Voo voo = voos.get(random.nextInt(voos.size()));
        String nome = NOMES[random.nextInt(NOMES.length)] + "-" + id;
        int assento = 1 + random.nextInt(voo.totalAssentos());
        return new PedidoCompra(id, nome, voo.codigo(), assento, origem);
    }
}
