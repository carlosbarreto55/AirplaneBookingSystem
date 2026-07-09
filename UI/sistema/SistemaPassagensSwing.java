package sistema;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class SistemaPassagensSwing extends JFrame {
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final String[] CODIGOS_VOOS = {"V001", "V002", "V003", "V004"};

    private final JRadioButton modoSequencial = new JRadioButton("Sequencial");
    private final JRadioButton modoConcorrente = new JRadioButton("Com threads", true);
    private final JSpinner threadsSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 64, 1));
    private final JSpinner assentosSpinner = new JSpinner(new SpinnerNumberModel(1000, 1, 100000, 10));
    private final JSpinner tempoMinimoSpinner = new JSpinner(new SpinnerNumberModel(80, 0, 60000, 10));
    private final JSpinner tempoMaximoSpinner = new JSpinner(new SpinnerNumberModel(120, 0, 60000, 10));
    private final JSpinner loteSpinner = new JSpinner(new SpinnerNumberModel(80, 1, 100000, 1));
    private final JSpinner assentoSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
    private final JTextField passageiroField = new JTextField("Maria");
    private final JComboBox<String> vooCombo = new JComboBox<String>(CODIGOS_VOOS);

    private final JButton iniciarButton = new JButton("Iniciar simulacao");
    private final JButton resetarButton = new JButton("Resetar");
    private final JButton comprarButton = new JButton("Comprar passagem");
    private final JButton loteButton = new JButton("Executar lote");
    private final JButton loteAsyncButton = new JButton("Lote assincrono");
    private final JButton listarButton = new JButton("Listar voos");
    private final JButton mapaButton = new JButton("Mostrar mapa");
    private final JButton statusButton = new JButton("Status");
    private final JButton encerrarThreadsButton = new JButton("Encerrar threads");
    private final JButton limparLogButton = new JButton("Limpar log");

    private final JTextArea logArea = new JTextArea();
    private final JTextArea voosArea = new JTextArea();
    private final JTextArea mapaArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Sistema nao iniciado.");

    private List<Voo> voos = Collections.emptyList();
    private final Random random = new Random();
    private long proximoId = 1L;
    private boolean iniciado;
    private boolean concorrente;
    private int assentosPorVoo;
    private int tempoMinimoMs;
    private int tempoMaximoMs;

    private final ArrayDeque<PedidoCompra> filaSequencial = new ArrayDeque<PedidoCompra>();
    private boolean sequencialProcessando;
    private Timer timerSequencial;

    private FilaUi filaConcorrente;
    private final List<AtendenteUi> atendentes = new ArrayList<AtendenteUi>();
    private ContadoresUi contadores = new ContadoresUi();

    private final Object loteLock = new Object();
    private Set<Long> loteAtual = Collections.emptySet();
    private long loteInicioMs;
    private String loteDescricao = "";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SistemaPassagensSwing janela = new SistemaPassagensSwing();
                janela.setVisible(true);
            }
        });
    }

    private SistemaPassagensSwing() {
        super("Sistema de Passagens Aereas - Swing");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 700));
        setLocationByPlatform(true);

        configurarTextAreas();
        setContentPane(criarConteudo());
        configurarAcoes();
        atualizarHabilitacao();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                encerrarExecucaoAtual(false);
            }
        });

        pack();
    }

    private void configurarTextAreas() {
        Font fonteMono = new Font(Font.MONOSPACED, Font.PLAIN, 13);
        logArea.setFont(fonteMono);
        logArea.setEditable(false);
        logArea.setLineWrap(false);

        voosArea.setFont(fonteMono);
        voosArea.setEditable(false);

        mapaArea.setFont(fonteMono);
        mapaArea.setEditable(false);
    }

    private JPanel criarConteudo() {
        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        raiz.add(criarPainelParametros(), BorderLayout.NORTH);
        raiz.add(criarAbas(), BorderLayout.CENTER);
        raiz.add(criarBarraStatus(), BorderLayout.SOUTH);
        return raiz;
    }

    private JPanel criarPainelParametros() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.add(criarPainelEntrada(), BorderLayout.CENTER);
        painel.add(criarPainelBotoesGlobais(), BorderLayout.SOUTH);
        return painel;
    }

    private JPanel criarPainelEntrada() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Parametros e comandos"));

        ButtonGroup grupoModo = new ButtonGroup();
        grupoModo.add(modoSequencial);
        grupoModo.add(modoConcorrente);

        JPanel modoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        modoPanel.add(modoSequencial);
        modoPanel.add(modoConcorrente);

        int linha = 0;
        adicionarLinha(painel, linha++, "Modo:", modoPanel, "Threads atendentes:", threadsSpinner);
        adicionarLinha(painel, linha++, "Assentos por voo:", assentosSpinner,
                "Tempo minimo (ms):", tempoMinimoSpinner);
        adicionarLinha(painel, linha++, "Tempo maximo (ms):", tempoMaximoSpinner,
                "Quantidade do lote:", loteSpinner);
        adicionarLinha(painel, linha++, "Passageiro:", passageiroField,
                "Voo:", vooCombo);
        adicionarLinha(painel, linha++, "Assento:", assentoSpinner,
                "", criarPainelAcoesCompra());

        return painel;
    }

    private JPanel criarPainelAcoesCompra() {
        JPanel painel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        painel.add(comprarButton);
        painel.add(loteButton);
        painel.add(loteAsyncButton);
        return painel;
    }

    private void adicionarLinha(JPanel painel, int linha, String labelEsquerdo, java.awt.Component campoEsquerdo,
            String labelDireito, java.awt.Component campoDireito) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.gridy = linha;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.weightx = 0;
        painel.add(new JLabel(labelEsquerdo), c);

        c.gridx = 1;
        c.weightx = 1;
        painel.add(campoEsquerdo, c);

        c.gridx = 2;
        c.weightx = 0;
        painel.add(new JLabel(labelDireito), c);

        c.gridx = 3;
        c.weightx = 1;
        painel.add(campoDireito, c);
    }

    private JPanel criarPainelBotoesGlobais() {
        JPanel painel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        painel.add(iniciarButton);
        painel.add(resetarButton);
        painel.add(new JLabel("|"));
        painel.add(listarButton);
        painel.add(mapaButton);
        painel.add(statusButton);
        painel.add(encerrarThreadsButton);
        painel.add(limparLogButton);
        return painel;
    }

    private JTabbedPane criarAbas() {
        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Log", new JScrollPane(logArea));
        abas.addTab("Voos", new JScrollPane(voosArea));
        abas.addTab("Mapa de assentos", new JScrollPane(mapaArea));
        return abas;
    }

    private JPanel criarBarraStatus() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusLabel.setForeground(new Color(30, 70, 110));
        painel.add(statusLabel, BorderLayout.CENTER);
        return painel;
    }

    private void configurarAcoes() {
        modoSequencial.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                atualizarHabilitacao();
            }
        });
        modoConcorrente.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                atualizarHabilitacao();
            }
        });

        iniciarButton.addActionListener(e -> iniciarSimulacao());
        resetarButton.addActionListener(e -> resetarSimulacao());
        comprarButton.addActionListener(e -> comprarManual());
        loteButton.addActionListener(e -> executarLote(false));
        loteAsyncButton.addActionListener(e -> executarLote(true));
        listarButton.addActionListener(e -> listarVoos());
        mapaButton.addActionListener(e -> mostrarMapa());
        statusButton.addActionListener(e -> mostrarStatusNoLog());
        encerrarThreadsButton.addActionListener(e -> encerrarThreadsPeloBotao());
        limparLogButton.addActionListener(e -> logArea.setText(""));
    }

    private void iniciarSimulacao() {
        Parametros parametros = lerParametros();
        if (parametros == null) {
            return;
        }

        this.concorrente = parametros.concorrente;
        this.assentosPorVoo = parametros.assentosPorVoo;
        this.tempoMinimoMs = parametros.tempoMinimoMs;
        this.tempoMaximoMs = parametros.tempoMaximoMs;
        this.voos = CatalogoVoos.criarVoos(assentosPorVoo);
        this.contadores = new ContadoresUi();
        this.proximoId = 1L;
        this.iniciado = true;
        this.sequencialProcessando = false;
        this.filaSequencial.clear();
        limparLoteAtual();

        if (concorrente) {
            iniciarAtendentes(parametros.quantidadeThreads);
            adicionarLog("Sistema", "VERSAO COM THREADS iniciada com " + parametros.quantidadeThreads
                    + " atendentes.");
        } else {
            adicionarLog("Sistema", "VERSAO SEQUENCIAL iniciada. Os pedidos sao processados um por vez.");
        }

        listarVoos();
        atualizarStatus();
        atualizarHabilitacao();
    }

    private Parametros lerParametros() {
        boolean usarConcorrencia = modoConcorrente.isSelected();
        int quantidadeThreads = ((Integer) threadsSpinner.getValue()).intValue();
        int assentos = ((Integer) assentosSpinner.getValue()).intValue();
        int minimo = ((Integer) tempoMinimoSpinner.getValue()).intValue();
        int maximo = ((Integer) tempoMaximoSpinner.getValue()).intValue();

        if (usarConcorrencia && quantidadeThreads < 1) {
            mostrarErro("A quantidade de threads deve ser maior que zero.");
            return null;
        }
        if (assentos < 1) {
            mostrarErro("A quantidade de assentos deve ser maior que zero.");
            return null;
        }
        if (minimo < 0 || maximo < 0) {
            mostrarErro("Os tempos de atendimento nao podem ser negativos.");
            return null;
        }
        if (maximo < minimo) {
            mostrarErro("O tempo maximo deve ser maior ou igual ao tempo minimo.");
            return null;
        }

        return new Parametros(usarConcorrencia, quantidadeThreads, assentos, minimo, maximo);
    }

    private void iniciarAtendentes(int quantidadeThreads) {
        filaConcorrente = new FilaUi();
        atendentes.clear();
        for (int i = 0; i < quantidadeThreads; i++) {
            AtendenteUi atendente = new AtendenteUi(i + 1, filaConcorrente, voos, contadores);
            atendentes.add(atendente);
            atendente.start();
        }
    }

    private void comprarManual() {
        if (!validarSistemaIniciado()) {
            return;
        }

        PedidoCompra pedido = criarPedidoManual();
        if (pedido == null) {
            return;
        }

        receberPedido(pedido);
    }

    private PedidoCompra criarPedidoManual() {
        String passageiro = passageiroField.getText().trim();
        if (passageiro.length() == 0) {
            mostrarErro("Informe o nome do passageiro.");
            return null;
        }
        if (passageiro.matches(".*\\s+.*")) {
            mostrarErro("Use um nome de passageiro sem espacos, como no terminal.");
            return null;
        }

        String codigoVoo = (String) vooCombo.getSelectedItem();
        int assento = ((Integer) assentoSpinner.getValue()).intValue();
        if (assento < 1 || assento > assentosPorVoo) {
            mostrarErro("O assento deve estar entre 1 e " + assentosPorVoo + ".");
            return null;
        }

        return new PedidoCompra(proximoId++, passageiro, codigoVoo, assento, "manual-ui");
    }

    private void executarLote(boolean assincrono) {
        if (!validarSistemaIniciado()) {
            return;
        }
        if (assincrono && !concorrente) {
            mostrarErro("Lote assincrono esta disponivel apenas no modo com threads.");
            return;
        }
        if (haLoteEmAndamento()) {
            mostrarErro("Aguarde o lote atual terminar antes de iniciar outro.");
            return;
        }

        int quantidade = ((Integer) loteSpinner.getValue()).intValue();
        if (quantidade < 1) {
            mostrarErro("A quantidade do lote deve ser maior que zero.");
            return;
        }

        List<PedidoCompra> pedidos = new ArrayList<PedidoCompra>();
        Set<Long> ids = new HashSet<Long>();
        for (int i = 0; i < quantidade; i++) {
            PedidoCompra pedido = GeradorPedidos.aleatorio(proximoId++, voos, random, "lote-ui");
            pedidos.add(pedido);
            ids.add(Long.valueOf(pedido.id()));
        }

        iniciarMonitoramentoDeLote(ids, assincrono ? "lote assincrono" : "lote");
        adicionarLog("Principal", "Enviando " + quantidade + " pedidos para " + loteDescricao + ".");
        for (PedidoCompra pedido : pedidos) {
            receberPedido(pedido);
        }
    }

    private void receberPedido(PedidoCompra pedido) {
        contadores.registrarRecebido();
        if (concorrente) {
            filaConcorrente.adicionar(pedido);
            adicionarLog("Fila", "Pedido enfileirado: " + pedido.descricaoCurta()
                    + " Tamanho=" + filaConcorrente.tamanho());
        } else {
            filaSequencial.addLast(pedido);
            adicionarLog("Fila", "Pedido entrou na fila sequencial: " + pedido.descricaoCurta()
                    + " Tamanho=" + filaSequencial.size());
            processarProximoSequencial();
        }
        atualizarStatus();
    }

    private void processarProximoSequencial() {
        if (sequencialProcessando || filaSequencial.isEmpty() || !iniciado) {
            return;
        }

        final PedidoCompra pedido = filaSequencial.removeFirst();
        sequencialProcessando = true;
        int tempo = tempoAleatorio();
        adicionarLog("Atendente-Unico", "Recebeu " + pedido.descricaoCurta());
        adicionarLog("Atendente-Unico", "Validando pagamento e assento por " + tempo + " ms.");

        timerSequencial = new Timer(tempo, e -> {
            timerSequencial.stop();
            boolean sucesso = finalizarReserva("Atendente-Unico", pedido);
            registrarConclusao(pedido, sucesso, contadores);
            sequencialProcessando = false;
            atualizarStatus();
            processarProximoSequencial();
        });
        timerSequencial.setRepeats(false);
        timerSequencial.start();
    }

    private boolean finalizarReserva(String origem, PedidoCompra pedido) {
        Voo voo = CatalogoVoos.procurar(voos, pedido.codigoVoo());
        if (voo == null) {
            adicionarLog(origem, "RECUSADO pedido #" + pedido.id() + ": voo inexistente.");
            return false;
        }

        ResultadoReserva resultado = voo.reservar(pedido.passageiro(), pedido.assento());
        if (resultado.sucesso()) {
            adicionarLog(origem, "CONFIRMADO pedido #" + pedido.id() + ": " + resultado.mensagem());
        } else {
            adicionarLog(origem, "RECUSADO pedido #" + pedido.id() + ": " + resultado.mensagem());
        }
        return resultado.sucesso();
    }

    private void registrarConclusao(PedidoCompra pedido, boolean sucesso, ContadoresUi contadoresDaExecucao) {
        contadoresDaExecucao.registrarConclusao(sucesso);
        verificarLoteConcluido(pedido.id());
    }

    private void listarVoos() {
        if (!validarSistemaIniciado()) {
            return;
        }
        voosArea.setText(CatalogoVoos.listar(voos));
        adicionarLog("Sistema", "Listagem de voos atualizada.");
    }

    private void mostrarMapa() {
        if (!validarSistemaIniciado()) {
            return;
        }

        String codigo = ((String) vooCombo.getSelectedItem()).toUpperCase(Locale.ROOT);
        Voo voo = CatalogoVoos.procurar(voos, codigo);
        if (voo == null) {
            mostrarErro("Voo nao encontrado.");
            return;
        }
        mapaArea.setText(voo.mapaAssentos());
        adicionarLog("Sistema", "Mapa do voo " + codigo + " atualizado.");
    }

    private void mostrarStatusNoLog() {
        if (!validarSistemaIniciado()) {
            return;
        }
        adicionarLog("Sistema", montarResumoStatus());
        atualizarStatus();
    }

    private String montarResumoStatus() {
        int tamanhoFila = concorrente && filaConcorrente != null
                ? filaConcorrente.tamanho()
                : filaSequencial.size() + (sequencialProcessando ? 1 : 0);
        return contadores.resumo(tamanhoFila);
    }

    private void atualizarStatus() {
        statusLabel.setText(iniciado ? montarResumoStatus() : "Sistema nao iniciado.");
    }

    private void resetarSimulacao() {
        if (!iniciado) {
            logArea.setText("");
            voosArea.setText("");
            mapaArea.setText("");
            atualizarStatus();
            return;
        }

        encerrarExecucaoAtual(true);
    }

    private void encerrarThreadsPeloBotao() {
        if (!iniciado || !concorrente) {
            return;
        }
        encerrarExecucaoAtual(true);
    }

    private void encerrarExecucaoAtual(boolean limparTelaAoFinal) {
        if (timerSequencial != null) {
            timerSequencial.stop();
        }
        filaSequencial.clear();
        sequencialProcessando = false;
        limparLoteAtual();

        if (filaConcorrente != null) {
            filaConcorrente.fechar();
        }

        if (!atendentes.isEmpty()) {
            List<AtendenteUi> copia = new ArrayList<AtendenteUi>(atendentes);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (AtendenteUi atendente : copia) {
                        try {
                            atendente.join();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            finalizarReset(limparTelaAoFinal);
                        }
                    });
                }
            }, "Reset-UI").start();
        } else {
            finalizarReset(limparTelaAoFinal);
        }
    }

    private void finalizarReset(boolean limparTela) {
        atendentes.clear();
        filaConcorrente = null;
        iniciado = false;
        if (limparTela) {
            voosArea.setText("");
            mapaArea.setText("");
            adicionarLog("Sistema", "Simulacao encerrada/resetada.");
        }
        atualizarStatus();
        atualizarHabilitacao();
    }

    private boolean validarSistemaIniciado() {
        if (!iniciado) {
            mostrarErro("Inicie a simulacao antes de executar comandos.");
            return false;
        }
        return true;
    }

    private void atualizarHabilitacao() {
        boolean modoThreads = modoConcorrente.isSelected();
        threadsSpinner.setEnabled(!iniciado && modoThreads);
        modoSequencial.setEnabled(!iniciado);
        modoConcorrente.setEnabled(!iniciado);
        assentosSpinner.setEnabled(!iniciado);
        tempoMinimoSpinner.setEnabled(!iniciado);
        tempoMaximoSpinner.setEnabled(!iniciado);

        iniciarButton.setEnabled(!iniciado);
        resetarButton.setEnabled(iniciado);
        comprarButton.setEnabled(iniciado);
        loteButton.setEnabled(iniciado);
        loteAsyncButton.setEnabled(iniciado && concorrente);
        listarButton.setEnabled(iniciado);
        mapaButton.setEnabled(iniciado);
        statusButton.setEnabled(iniciado);
        encerrarThreadsButton.setEnabled(iniciado && concorrente);
    }

    private int tempoAleatorio() {
        return tempoMinimoMs + random.nextInt(tempoMaximoMs - tempoMinimoMs + 1);
    }

    private void adicionarLog(String origem, String mensagem) {
        Runnable tarefa = new Runnable() {
            @Override
            public void run() {
                String linha = String.format("[%s] %-18s | %s%n",
                        LocalTime.now().format(FORMATO_HORA), origem, mensagem);
                logArea.append(linha);
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            tarefa.run();
        } else {
            SwingUtilities.invokeLater(tarefa);
        }
    }

    private void mostrarErro(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Parametro invalido", JOptionPane.WARNING_MESSAGE);
    }

    private void iniciarMonitoramentoDeLote(Set<Long> ids, String descricao) {
        synchronized (loteLock) {
            loteAtual = new HashSet<Long>(ids);
            loteInicioMs = System.currentTimeMillis();
            loteDescricao = descricao;
        }
    }

    private boolean haLoteEmAndamento() {
        synchronized (loteLock) {
            return !loteAtual.isEmpty();
        }
    }

    private void verificarLoteConcluido(long idPedido) {
        String mensagem = null;
        synchronized (loteLock) {
            if (loteAtual.remove(Long.valueOf(idPedido)) && loteAtual.isEmpty()) {
                long duracao = System.currentTimeMillis() - loteInicioMs;
                mensagem = "Finalizado " + loteDescricao + " em " + duracao + " ms. "
                        + montarResumoStatus();
                loteDescricao = "";
            }
        }

        if (mensagem != null) {
            adicionarLog("Lote", mensagem);
        }
    }

    private void limparLoteAtual() {
        synchronized (loteLock) {
            loteAtual = Collections.emptySet();
            loteInicioMs = 0L;
            loteDescricao = "";
        }
    }

    private static final class Parametros {
        private final boolean concorrente;
        private final int quantidadeThreads;
        private final int assentosPorVoo;
        private final int tempoMinimoMs;
        private final int tempoMaximoMs;

        private Parametros(boolean concorrente, int quantidadeThreads, int assentosPorVoo,
                int tempoMinimoMs, int tempoMaximoMs) {
            this.concorrente = concorrente;
            this.quantidadeThreads = quantidadeThreads;
            this.assentosPorVoo = assentosPorVoo;
            this.tempoMinimoMs = tempoMinimoMs;
            this.tempoMaximoMs = tempoMaximoMs;
        }
    }

    private static final class ContadoresUi {
        private int recebidos;
        private int concluidos;
        private int confirmados;
        private int recusados;

        synchronized void registrarRecebido() {
            recebidos++;
        }

        synchronized void registrarConclusao(boolean sucesso) {
            concluidos++;
            if (sucesso) {
                confirmados++;
            } else {
                recusados++;
            }
        }

        synchronized String resumo(int tamanhoFila) {
            return "recebidos=" + recebidos
                    + ", concluidos=" + concluidos
                    + ", confirmados=" + confirmados
                    + ", recusados=" + recusados
                    + ", na fila=" + tamanhoFila + ".";
        }
    }

    private static final class FilaUi {
        private final ArrayDeque<PedidoCompra> pedidos = new ArrayDeque<PedidoCompra>();
        private boolean fechada;

        synchronized void adicionar(PedidoCompra pedido) {
            if (fechada) {
                throw new IllegalStateException("Fila fechada.");
            }
            pedidos.addLast(pedido);
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

    private final class AtendenteUi extends Thread {
        private final int numero;
        private final FilaUi fila;
        private final List<Voo> voosAtendimento;
        private final ContadoresUi contadoresAtendimento;
        private final Random randomAtendente;

        private AtendenteUi(int numero, FilaUi fila, List<Voo> voosAtendimento,
                ContadoresUi contadoresAtendimento) {
            super("Atendente-UI-" + numero);
            this.numero = numero;
            this.fila = fila;
            this.voosAtendimento = voosAtendimento;
            this.contadoresAtendimento = contadoresAtendimento;
            this.randomAtendente = new Random(System.nanoTime() + numero);
        }

        @Override
        public void run() {
            adicionarLog(nome(), "Thread iniciada e aguardando pedidos.");
            try {
                while (true) {
                    PedidoCompra pedido = fila.retirar();
                    if (pedido == null) {
                        adicionarLog(nome(), "Fila fechada e vazia. Encerrando.");
                        return;
                    }
                    processar(pedido);
                }
            } catch (InterruptedException e) {
                adicionarLog(nome(), "Thread interrompida.");
                interrupt();
            }
        }

        private void processar(PedidoCompra pedido) throws InterruptedException {
            adicionarLog(nome(), "Recebeu " + pedido.descricaoCurta());
            int tempo = tempoMinimoMs + randomAtendente.nextInt(tempoMaximoMs - tempoMinimoMs + 1);
            adicionarLog(nome(), "Validando pagamento e assento por " + tempo + " ms.");
            Thread.sleep(tempo);

            Voo voo = CatalogoVoos.procurar(voosAtendimento, pedido.codigoVoo());
            boolean sucesso;
            if (voo == null) {
                adicionarLog(nome(), "RECUSADO pedido #" + pedido.id() + ": voo inexistente.");
                sucesso = false;
            } else {
                ResultadoReserva resultado = voo.reservar(pedido.passageiro(), pedido.assento());
                sucesso = resultado.sucesso();
                if (sucesso) {
                    adicionarLog(nome(), "CONFIRMADO pedido #" + pedido.id() + ": " + resultado.mensagem());
                } else {
                    adicionarLog(nome(), "RECUSADO pedido #" + pedido.id() + ": " + resultado.mensagem());
                }
            }
            registrarConclusao(pedido, sucesso, contadoresAtendimento);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    atualizarStatus();
                }
            });
        }

        private String nome() {
            return "Atendente-" + numero;
        }
    }
}
