package framework;

import java.util.HashMap;

public class EstadoAnel extends Estado {
    public int idLocal;
    public int idSucessor;
    public String ipSucessor;
    public int portaSucessor;
    private final HashMap<String, String> armazenamento = new HashMap<>();
    
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
        return(chave.hashCode() & 0x7fffffff) % TAMANHO_ANEL; // Para impedir valores negativos
    }

    @Override
    public void acao() {
        System.out.println("[ESTADO] Nó ativo no anel. ID: " + idLocal);
    }

    @Override
    public void transicao(Evento _e) {
        switch (_e.code) {
            case 1: // JOIN_REQ
                tratarJoinReq(_e);
                break;
            case 2: // JOIN_RESP
                tratarJoinResp(_e);
                break;
            case 4: // LOOKUP_REQ
                tratarLookupReq(_e);
                break;
            case 6:
                tratarPutReq(_e);
                break;
            case 7:
                tratarGetReq(_e);
                break;
            case 8:
                tratarGetResp(_e);
                break;
            default:
                System.out.println("[ERRO] Evento não suportado: " + _e.code);
        }
    }
    
    private void tratarJoinReq(Evento e) {
        int idNovoNo = Integer.parseInt(e.C1);
        String origemNovoNo = e.C2;

        if (pertenceAoIntervalo(idLocal, idSucessor, idNovoNo)) {
            System.out.println("[JOIN] Aceitando nó " + idNovoNo + " no anel.");
            
            // 1. Responde ao novo nó informando quem será o sucessor dele (meu sucessor atual ou eu mesmo)
            Evento resp = new Evento(2, String.valueOf(idSucessor), ipSucessor + ":" + portaSucessor, null);
            enviarMensagem(origemNovoNo, resp);
            
            // 2. Atualiza o MEU sucessor para ser este novo nó
            String[] partes = origemNovoNo.split(":");
            this.idSucessor = idNovoNo;
            this.ipSucessor = partes[0];
            this.portaSucessor = Integer.parseInt(partes[1]);
        } else {
            System.out.println("[JOIN] Repassando req do nó " + idNovoNo + " para o sucessor.");
            enviarMensagem(ipSucessor + ":" + portaSucessor, e);
        }
    }

    private void tratarJoinResp(Evento e) {
        this.idSucessor = Integer.parseInt(e.C1);
        String[] partes = e.C2.split(":");
        this.ipSucessor = partes[0];
        this.portaSucessor = Integer.parseInt(partes[1]);
        System.out.println("[JOIN] Inserção concluída. Meu novo sucessor é ID: " + this.idSucessor);
    }
    
    private void tratarPutReq(Evento e) {
        int idChave = gerarId(e.C1);
        if (pertenceAoIntervalo(idLocal, idSucessor, idChave)) {
            armazenamento.put(e.C1, e.C2);
            System.out.println("[ARMAZENAMENTO] Chave '" + e.C1 + "' salva localmente.");
        } else {
            enviarMensagem(ipSucessor + ":" + portaSucessor, e);
        }
    }

    private void tratarGetReq(Evento e) {
        int idChave = gerarId(e.C1);
        String origem = e.C2;

        if (pertenceAoIntervalo(idLocal, idSucessor, idChave)) {
            String valor = armazenamento.getOrDefault(e.C1, "NULL");
            Evento resp = new Evento(8, e.C1, valor, null);
            enviarMensagem(origem, resp);
        } else {
            enviarMensagem(ipSucessor + ":" + portaSucessor, e);
        }
    }

    private void tratarGetResp(Evento e) {
        System.out.println("[APLICACAO] Resultado da busca pela chave '" + e.C1 + "': " + e.C2);
    }
    
    // Método a ser chamado pela Main quando o nó quiser entrar na rede
    public void iniciarJoin(String ipConhecido, int portaConhecida) {
        System.out.println("[JOIN] Solicitando entrada via nó " + ipConhecido + ":" + portaConhecida);
        String meuEndereco = ent.msg.pegaHostLocal() + ":" + ent.msg.lPort;
        Evento req = new Evento(1, String.valueOf(this.idLocal), meuEndereco, null);
        enviarMensagem(ipConhecido + ":" + portaConhecida, req);
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
        ent.conexoesSaida.enviarMensagem(destino, ev);
    }
 
}