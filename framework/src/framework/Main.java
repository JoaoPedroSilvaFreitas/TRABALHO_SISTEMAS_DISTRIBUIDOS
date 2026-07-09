package framework;

public class Main {
    public static void main(String[] args) {
        System.out.println("Iniciando depuração do Nó DHT...");

        // 1. Instanciação da Entidade (Nó local)
        Entidade noLocal = new Entidade();
        
        // Define a porta local (ex: 8080)
        noLocal.defPortaLocal(8080);

        // 2. Definição do Estado Inicial
        EstadoDHTInicial estadoInicial = new EstadoDHTInicial(noLocal);
        noLocal.mudaEstado(estadoInicial);

        // 3. Inicialização da Thread de Rede (usando a especialização)
        SocketThreadCustomizada threadRede = new SocketThreadCustomizada(noLocal.msg, noLocal);
        Thread tRede = new Thread(threadRede);
        tRede.start();

        // 4. Injeção manual de um evento para teste (Simulando uma ação do usuário)
        System.out.println("Injetando evento de teste manual no buffer...");
        noLocal.colocaEvento(new Evento(1, "DADO_TESTE", "Nó de Origem", "Param 3"));

        // 5. Teste do Timer
        Timeout timerTeste = new Timeout(noLocal, 3000); // Dispara evento tipo 3 em 3 segundos
        new Thread(timerTeste).start();
    }
}

// --- CLASSES DE ESPECIALIZAÇÃO PARA DEBUG ---

// Especialização da SocketThread para visualizar o desempacotamento
class SocketThreadCustomizada extends SocketThread {

    public SocketThreadCustomizada(Msg _m, Entidade _u) {
        super(_m, _u);
    }

    @Override
    public void desempacota() {
        System.out.println("[REDE] Desempacotando mensagem: " + tmp);
        // Aqui você converteria a string 'tmp' em um objeto Evento
        // e usaria ent.colocaEvento(novoEvento);
    }
}

// Especialização do Estado para mapear as transições e processar eventos
class EstadoDHTInicial extends Estado {

    public EstadoDHTInicial(Entidade _e) {
        super(_e);
    }

    @Override
    public void acao() {
        System.out.println("[ESTADO] Nó entrou no Estado Inicial.");
    }

    @Override
    public void transicao(Evento _e) {
        System.out.println("[TRANSICAO] Processando Evento Code: " + _e.code);
        
        switch (_e.code) {
            case 1:
                System.out.println("[LOGICA] Mensagem recebida C1: " + _e.C1);
                break;
            case 3:
                System.out.println("[LOGICA] Timeout atingido. Possível falha de nó vizinho.");
                break;
            default:
                System.out.println("[ERRO] Evento desconhecido.");
                break;
        }
    }
}