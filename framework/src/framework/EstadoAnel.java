package framework;

public class EstadoAnel extends Estado {
    public int idLocal;
    public int idSucessor;
    public String ipSucessor;
    public int portaSucessor;
    
    // Espaço lógico do anel
    private final int TAMANHO_ANEL = 1024;

    public EstadoAnel(Entidade _e, String ipLocal, int portaLocal) {
        super(_e);
        // Gera o ID utilizando hash nativo e módulo matemático simples
        this.idLocal = gerarId(ipLocal + ":" + portaLocal);
        
        // Nó inicia isolado, apontando para si mesmo
        this.idSucessor = this.idLocal;
        this.ipSucessor = ipLocal;
        this.portaSucessor = portaLocal;
    }

    private int gerarId(String chave) {
        return Math.abs(chave.hashCode()) % TAMANHO_ANEL;
    }

    @Override
    public void acao() {
        System.out.println("[ESTADO] Nó ativo no anel. ID: " + idLocal);
    }

    @Override
    public void transicao(Evento _e) {
        switch (_e.code) {
            case 4: // LOOKUP_REQ
                tratarLookupReq(_e);
                break;
            case 1: // JOIN_REQ
                // A ser implementado
                break;
            default:
                System.out.println("[ERRO] Evento não suportado no EstadoAnel: " + _e.code);
        }
    }

    private void tratarLookupReq(Evento e) {
        int idBusca = Integer.parseInt(e.C1);
        String origem = e.C2; // Formato esperado: "IP:Porta"

        boolean responsavel = pertenceAoIntervalo(idLocal, idSucessor, idBusca);

        if (responsavel) {
            System.out.println("[ROTEAMENTO] Responsabilidade confirmada para a chave: " + idBusca);
            String meuEndereco = ent.msg.pegaHostLocal() + ":" + ent.msg.lPort;
            // Retorna LOOKUP_RESP (code 5) para a origem
            Evento resp = new Evento(5, String.valueOf(idBusca), meuEndereco, null);
            enviarMensagem(origem, resp);
        } else {
            System.out.println("[ROTEAMENTO] Repassando busca da chave " + idBusca + " para sucessor ID: " + idSucessor);
            // Repassa o evento original inalterado para o sucessor
            enviarMensagem(ipSucessor + ":" + portaSucessor, e);
        }
    }

    private boolean pertenceAoIntervalo(int atual, int sucessor, int busca) {
        // Se o nó é o único no anel, ele responde por todo o espaço
        if (atual == sucessor) {
            return true;
        }
        
        if (atual < sucessor) {
            return busca > atual && busca <= sucessor;
        } else {
            // Trata a condição em que o anel dá a volta no zero
            return busca > atual || busca <= sucessor;
        }
    }

    private void enviarMensagem(String destino, Evento ev) {
        String[] partes = destino.split(":");
        String ip = partes[0];
        int porta = Integer.parseInt(partes[1]);
        
        Msg enviador = new Msg();
        if (enviador.conecta(ip, porta) == 0) {
            // Utiliza a vírgula herdada do Evento.toString() e adiciona quebra de linha para o BufferedReader do destino
            enviador.envia(ev.toString() + "\n");
            enviador.termina();
        } else {
            System.out.println("[ERRO] Falha de conexão ao repassar mensagem para: " + destino);
        }
    }
}