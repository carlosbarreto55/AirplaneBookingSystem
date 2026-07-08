package sistema;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CatalogoVoos {
    private CatalogoVoos() {
    }

    static List<Voo> criarVoos(int assentosPorVoo) {
        List<Voo> voos = new ArrayList<Voo>();
        voos.add(new Voo("V001", "Manaus", "Sao Paulo", assentosPorVoo, 1820.00));
        voos.add(new Voo("V002", "Manaus", "Rio de Janeiro", assentosPorVoo, 1975.50));
        voos.add(new Voo("V003", "Manaus", "Brasilia", assentosPorVoo, 1450.90));
        voos.add(new Voo("V004", "Manaus", "Fortaleza", assentosPorVoo, 1325.00));
        return voos;
    }

    static Voo procurar(List<Voo> voos, String codigo) {
        String codigoNormalizado = codigo.toUpperCase(Locale.ROOT);
        for (int i = 0; i < voos.size(); i++) {
            Voo voo = voos.get(i);
            if (voo.codigo().equals(codigoNormalizado)) {
                return voo;
            }
        }
        return null;
    }

    static String listar(List<Voo> voos) {
        StringBuilder saida = new StringBuilder();
        saida.append("Voos disponiveis:\n");
        for (int i = 0; i < voos.size(); i++) {
            saida.append("  ").append(voos.get(i).resumo()).append('\n');
        }
        return saida.toString();
    }
}
