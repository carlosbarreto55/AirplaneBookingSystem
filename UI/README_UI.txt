Interface Swing do Sistema de Passagens Aereas

Esta pasta contem apenas a interface grafica. O codigo original em src/ nao foi
alterado.

Compilar a aplicacao com a UI:

    javac -d out src/sistema/*.java UI/sistema/*.java

Executar:

    java -cp out sistema.SistemaPassagensSwing

Na janela, escolha:
- modo sequencial ou com threads;
- quantidade de threads;
- quantidade de assentos por voo;
- tempo minimo e maximo de atendimento;
- dados de compra manual ou quantidade do lote.

A interface valida parametros invalidos, como tempo maximo menor que o minimo,
assento fora do intervalo, passageiro vazio e tentativa de lote assincrono no
modo sequencial.
