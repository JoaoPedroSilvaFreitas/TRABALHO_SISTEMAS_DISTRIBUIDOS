package framework;

public class Main {
    public static void main(String[] args) {
        System.out.println("Iniciando depuração do Nó DHT...");

        // 1. Instanciação da Entidade (Nó local)
        Entidade noLocal = new Entidade();
        
        // Define a porta local (ex: 8080)
        noLocal.defPortaLocal(8080);

        String ipLocal = noLocal.msg.pegaHostLocal();            

        // 2. Definição do Estado Inicial
        EstadoAnel estadoInicial = new EstadoAnel(noLocal, ipLocal, 8080);
        noLocal.mudaEstado(estadoInicial);

        // 3. Inicialização da Thread de Rede (usando a especialização)
        ServidorRedeDHT servidor = new ServidorRedeDHT(noLocal, 8080);
        new Thread(servidor).start();

        // 4. Injeção manual de um evento para teste (Simulando uma ação do usuário)
        System.out.println("Injetando evento de teste manual no buffer...");
        noLocal.colocaEvento(new Evento(1, "DADO_TESTE", "Nó de Origem", "Param 3"));

        // 5. Teste do Timer
        Timeout timerTeste = new Timeout(noLocal, 3000); // Dispara evento tipo 3 em 3 segundos
        new Thread(timerTeste).start();
    }
}

