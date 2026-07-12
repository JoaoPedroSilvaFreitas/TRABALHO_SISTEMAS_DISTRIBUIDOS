package framework;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EstadoAnel extends Estado {
    public int idLocal;
    public String ipLocal;
    public int portaLocal;
    public int idSucessor;
    public String ipSucessor;
    public int portaSucessor;
    private final HashMap<String, String> armazenamento = new HashMap<>();

    // implementa logica de predecessor para nao precisar apenas olhar para "frente"

    public int idPredecessor;
    public String ipPredecessor;
    public int portaPredecessor;

    // Espaço lógico do anel
    private final int TAMANHO_ANEL = 1024;

    public EstadoAnel(Entidade _e, String ipLocal, int portaLocal) {
        super(_e);
        // Gera o ID utilizando hash nativo e módulo matemático simples
        this.idLocal = gerarId(ipLocal + ":" + portaLocal);
        this.ipLocal = ipLocal;
        this.portaLocal = portaLocal;

        // Nó inicia isolado, apontando para si mesmo
        this.idSucessor = this.idLocal;
        this.ipSucessor = ipLocal;
        this.portaSucessor = portaLocal;

        // inicializa o predecessor como local
        this.idPredecessor = this.idLocal;
        this.ipPredecessor = ipLocal;
        this.portaPredecessor = portaLocal;

    }

    private int gerarId(String chave) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(chave.getBytes(StandardCharsets.UTF_8));

            // Combina os dois primeiros bytes do SHA-256 e os reduz ao espaço
            // lógico do anel. Como 1024 = 2^10, o resultado fica entre 0 e 1023.
            int primeirosBits = ((hash[0] & 0xff) << 8) | (hash[1] & 0xff);
            return primeirosBits % TAMANHO_ANEL;
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 faz parte obrigatória da plataforma Java.
            throw new IllegalStateException("SHA-256 não está disponível", ex);
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Grava o estado atual do nó em um arquivo JSON, usado pela visualização do anel
    private void exportarStatus() {
        File pasta = new File("dht_status");
        if (!pasta.exists()) {
            pasta.mkdirs();
        }

        StringBuilder chavesJson = new StringBuilder("[");
        boolean primeira = true;
        for (Map.Entry<String, String> par : armazenamento.entrySet()) {
            if (!primeira) chavesJson.append(",");
            chavesJson.append("{\"chave\":\"").append(escapeJson(par.getKey()))
                    .append("\",\"valor\":\"").append(escapeJson(par.getValue())).append("\"}");
            primeira = false;
        }
        chavesJson.append("]");

        String conteudo = "{"
                + "\"idLocal\":" + idLocal + ","
                + "\"endereco\":\"" + ipLocal + ":" + portaLocal + "\","
                + "\"idSucessor\":" + idSucessor + ","
                + "\"enderecoSucessor\":\"" + ipSucessor + ":" + portaSucessor + "\","
                + "\"idPredecessor\":" + idPredecessor + ","
                + "\"enderecoPredecessor\":\"" + ipPredecessor + ":" + portaPredecessor + "\","
                + "\"totalChaves\":" + armazenamento.size() + ","
                + "\"chaves\":" + chavesJson + ","
                + "\"atualizadoEm\":" + System.currentTimeMillis()
                + "}";
        try (FileWriter fw = new FileWriter(new File(pasta, "no_" + portaLocal + ".json"))) {
            fw.write(conteudo);
        } catch (IOException ex) {
            System.err.println("[ERRO] Falha ao exportar status: " + ex.getMessage());
        }
    }

    @Override
    public void acao() {
        System.out.println("[ESTADO] Nó ativo no anel. ID: " + idLocal);
        exportarStatus();
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
            case 3: // rotina para verificar se houve a inserção de um novo nó
                estabilizar();
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
            case 9: // add: notify para passar possivel sucessor e salvar predecessor
                tratarNotify(_e);
                break;
            // casos do timeout: caso algum nó nao tenha salvo um nó novo, ele sempre
            // pergunta o predecessor para o próximo nó, verificando
            // se é ele mesmo ou se algum no foi inserido e ele tem que se atualizar
            case 10: // ASK_PREDECESSOR
                tratarAskPredecessor(_e);
                break;

            case 11: // confirmar predecessor e sucessor -> se o nó está situado de forma certa ->
                     // que foi solicitado no nó anterior
                tratarPredecessorResp(_e);
                break;
            case 12: // TRANSFER_KEYS_REQ
                tratarTransferKeysReq(_e);
                break;
            case 13: // TRANSFER_KEY
                tratarTransferKey(_e);
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

            // 1. Responde ao novo nó informando quem será o sucessor dele (meu sucessor
            // atual) e quem será o predecessor dele: eu mesmo, pois o novo nó está se
            // inserindo logo depois de mim no anel (entre mim e meu antigo sucessor).
            String meuEnderecoComoPredecessor = idLocal + ";" + ipLocal + ":" + portaLocal;

            Evento resp = new Evento(2, String.valueOf(idSucessor), ipSucessor + ":" + portaSucessor,
                    meuEnderecoComoPredecessor);
            enviarMensagem(origemNovoNo, resp);

            // 2. Atualiza o MEU sucessor para ser este novo nó
            this.idSucessor = idNovoNo;
            String[] partes = origemNovoNo.split(":");
            this.ipSucessor = partes[0];
            this.portaSucessor = Integer.parseInt(partes[1]);

            exportarStatus();

        } else {
            System.out.println("[JOIN] Repassando req do nó " + idNovoNo + " para o sucessor.");
            enviarMensagem(ipSucessor + ":" + portaSucessor, e);
        }
    }

    private void tratarJoinResp(Evento e) {
        this.idSucessor = Integer.parseInt(e.C1);
        String[] partesSucessor = e.C2.split(":");
        this.ipSucessor = partesSucessor[0];
        this.portaSucessor = Integer.parseInt(partesSucessor[1]);

        // C3 = "idPredecessor;ip:porta"
        String[] partesPred = e.C3.split(";");
        this.idPredecessor = Integer.parseInt(partesPred[0]);
        String[] enderecoPred = partesPred[1].split(":");
        this.ipPredecessor = enderecoPred[0];
        this.portaPredecessor = Integer.parseInt(enderecoPred[1]);

        System.out.println("[JOIN] Inserção concluída. Sucessor: " + idSucessor + " | Predecessor: " + idPredecessor);
        exportarStatus();

        // Avisa meu novo sucessor que agora eu sou o predecessor dele
        String meuEndereco = ipLocal + ":" + portaLocal;
        Evento notify = new Evento(9, String.valueOf(idLocal), meuEndereco, null);
        enviarMensagem(ipSucessor + ":" + portaSucessor, notify);

        // Antes do JOIN, o sucessor armazenava também as chaves que agora pertencem
        // ao novo nó. Solicita a ele as chaves do intervalo (predecessor, local].
        Evento transferencia = new Evento(12, String.valueOf(idPredecessor),
                String.valueOf(idLocal), meuEndereco);
        enviarMensagem(ipSucessor + ":" + portaSucessor, transferencia);
    }

    private void tratarPutReq(Evento e) {
        int idChave = gerarId(e.C1);
        if (souResponsavel(idChave)) {
            armazenamento.put(e.C1, e.C2);
            System.out.println("[ARMAZENAMENTO] Chave '" + e.C1 + "' salva localmente.");
            exportarStatus();
        } else {
            enviarMensagem(ipSucessor + ":" + portaSucessor, e);
        }
    }

    private void tratarGetReq(Evento e) {
        int idChave = gerarId(e.C1);
        String origem = e.C2;

        if (souResponsavel(idChave)) {
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
        String meuEndereco = ipLocal + ":" + portaLocal;
        Evento req = new Evento(1, String.valueOf(this.idLocal), meuEndereco, null);
        enviarMensagem(ipConhecido + ":" + portaConhecida, req);
    }

    // Métodos chamados pelo console interativo da Main para injetar PUT/GET
    // a partir deste nó, que roteia normalmente pelo anel até o responsável.
    public void solicitarPut(String chave, String valor) {
        tratarPutReq(new Evento(6, chave, valor, null));
    }

    public void solicitarGet(String chave) {
        String meuEndereco = ipLocal + ":" + portaLocal;
        tratarGetReq(new Evento(7, chave, meuEndereco, null));
    }

    private void tratarLookupReq(Evento e) {
        int idBusca = Integer.parseInt(e.C1);
        String origem = e.C2; // Formato esperado: "IP:Porta"

        boolean responsavel = souResponsavel(idBusca);

        if (responsavel) {
            System.out.println("[ROTEAMENTO] Responsabilidade confirmada para a chave: " + idBusca);
            String meuEndereco = ipLocal + ":" + portaLocal;
            // Retorna LOOKUP_RESP (code 5) para a origem
            Evento resp = new Evento(5, String.valueOf(idBusca), meuEndereco, null);
            enviarMensagem(origem, resp);
        } else {
            System.out
                    .println("[ROTEAMENTO] Repassando busca da chave " + idBusca + " para sucessor ID: " + idSucessor);
            // Repassa o evento original inalterado para o sucessor
            enviarMensagem(ipSucessor + ":" + portaSucessor, e);
        }
    }

    // No Chord, cada nó responde pelas chaves no intervalo
    // (seu predecessor, seu próprio ID].
    private boolean souResponsavel(int idChave) {
        return pertenceAoIntervalo(idPredecessor, idLocal, idChave);
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

    private void tratarNotify(Evento e) {
        int idCandidato = Integer.parseInt(e.C1);
        String[] partes = e.C2.split(":");
        String ipCandidato = partes[0];
        int portaCandidato = Integer.parseInt(partes[1]);

        // Só aceita o candidato se ele estiver estritamente entre meu predecessor atual
        // e eu
        if (idPredecessor == idLocal || pertenceAoIntervaloAberto(idPredecessor, idLocal, idCandidato)) {
            this.idPredecessor = idCandidato;
            this.ipPredecessor = ipCandidato;
            this.portaPredecessor = portaCandidato;
            System.out.println("[NOTIFY] Novo predecessor: " + idCandidato);
            exportarStatus();
        }
    }

    //
    private boolean pertenceAoIntervaloAberto(int predecessor, int local, int candidato) {
        if (predecessor == local) {
            return true; // eu ainda não tenho predecessor real, qualquer um serve
        }
        if (predecessor < local) {
            return candidato > predecessor && candidato < local;
        } else {
            return candidato > predecessor || candidato < local;
        }
    }

    private void estabilizar() {
        String meuEndereco = ipLocal + ":" + portaLocal;
        Evento pedido = new Evento(10, meuEndereco, null, null);
        enviarMensagem(ipSucessor + ":" + portaSucessor, pedido);

        // Grava o status a cada rodada para que a visualização saiba que o nó
        // continua vivo (arquivos com "atualizadoEm" muito antigo são
        // considerados de um nó desligado e descartados no lado do cliente)
        exportarStatus();

        // Reagenda a próxima rodada de estabilização -> reinicia o timeout para
        // verificar novamente dps -> rotina
        // a main tem o primeiro timeout, depois mantem
        Timeout proximoTimer = new Timeout(ent, 5000); // a cada 5 segundos
        new Thread(proximoTimer).start();
    }

    private void tratarAskPredecessor(Evento e) {
        String origem = e.C1;
        Evento resp = new Evento(11, String.valueOf(idPredecessor), ipPredecessor + ":" + portaPredecessor, null);
        enviarMensagem(origem, resp);
    }

    private void tratarPredecessorResp(Evento e) {
        int idCandidato = Integer.parseInt(e.C1);
        String[] endereco = e.C2.split(":");
        String ipCandidato = endereco[0];
        int portaCandidato = Integer.parseInt(endereco[1]);

        // Se o predecessor do meu sucessor está estritamente entre mim e meu sucessor,
        // ele deveria ser meu novo sucessor (alguém se inseriu ali sem eu saber)
        if (pertenceAoIntervaloAberto(idLocal, idSucessor, idCandidato)) {
            this.idSucessor = idCandidato;
            this.ipSucessor = ipCandidato;
            this.portaSucessor = portaCandidato;
            System.out.println("[STABILIZE] Sucessor corrigido para: " + idCandidato);
            exportarStatus();
        }

        // Independente de ter corrigido ou não, notifico meu sucessor (atual) que sou o
        // predecessor dele
        String meuEndereco = ipLocal + ":" + portaLocal;
        Evento notify = new Evento(9, String.valueOf(idLocal), meuEndereco, null);
        enviarMensagem(ipSucessor + ":" + portaSucessor, notify);
    }

    private void tratarTransferKeysReq(Evento e) {
        int idPredecessorNovoNo = Integer.parseInt(e.C1);
        int idNovoNo = Integer.parseInt(e.C2);
        String enderecoNovoNo = e.C3;
        int totalTransferido = 0;

        Iterator<Map.Entry<String, String>> it = armazenamento.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> par = it.next();
            int idChave = gerarId(par.getKey());

            if (pertenceAoIntervalo(idPredecessorNovoNo, idNovoNo, idChave)) {
                // Evento próprio de transferência: o destino armazena diretamente, sem
                // recalcular a rota enquanto o anel ainda está se estabilizando.
                Evento chaveTransferida = new Evento(13, par.getKey(), par.getValue(), null);
                enviarMensagem(enderecoNovoNo, chaveTransferida);
                it.remove();
                totalTransferido++;
            }
        }

        if (totalTransferido > 0) {
            System.out.println("[TRANSFERENCIA] " + totalTransferido
                    + " chave(s) enviada(s) ao novo nó " + idNovoNo + ".");
            exportarStatus();
        }
    }

    private void tratarTransferKey(Evento e) {
        armazenamento.put(e.C1, e.C2);
        System.out.println("[TRANSFERENCIA] Chave '" + e.C1 + "' recebida do antigo responsável.");
        exportarStatus();
    }

}
