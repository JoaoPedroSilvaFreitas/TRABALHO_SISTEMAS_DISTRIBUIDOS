package framework;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        // Uso: java framework.Main <portaLocal> [ipBootstrap] [portaBootstrap]
        int portaLocal = Integer.parseInt(args[0]);

        System.out.println("Iniciando nó DHT na porta " + portaLocal + "...");

        // 1. Instanciação da Entidade (Nó local)
        Entidade noLocal = new Entidade();
        noLocal.defPortaLocal(portaLocal);

        // Usa 127.0.0.1 fixo para testes na mesma máquina, evitando que
        // InetAddress.getLocalHost() resolva para outro endereço (ex: 127.0.1.1
        // no /etc/hosts do Ubuntu), o que faria os nós verem endereços diferentes
        // uns dos outros e quebraria o roteamento do anel.
        String ipLocal = "127.0.0.1";

        // 2. Definição do Estado Inicial
        EstadoAnel estadoInicial = new EstadoAnel(noLocal, ipLocal, portaLocal);
        noLocal.mudaEstado(estadoInicial);

        // 3. Inicialização da Thread de Rede (usando a especialização)
        ServidorRedeDHT servidor = new ServidorRedeDHT(noLocal, portaLocal);
        new Thread(servidor).start();

        // 4. Se foi passado um nó de bootstrap, solicita entrada no anel
        if (args.length >= 3) {
            String ipBootstrap = args[1];
            int portaBootstrap = Integer.parseInt(args[2]);
            estadoInicial.iniciarJoin(ipBootstrap, portaBootstrap);
        }

        // 5. Timer inicial de estabilização periódica
        Timeout timer = new Timeout(noLocal, 5000);
        new Thread(timer).start();

        // 6. Console interativo: "put chave valor" e "get chave"
        console(estadoInicial);
    }

    private static void console(EstadoAnel estado) {
        System.out.println("Comandos disponíveis: put <chave> <valor> | get <chave> | sair");
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(System.in))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                String[] partes = linha.trim().split("\\s+", 3);
                if (partes.length == 0 || partes[0].isEmpty()) {
                    continue;
                }
                switch (partes[0].toLowerCase()) {
                    case "put":
                        if (partes.length < 3) {
                            System.out.println("Uso: put <chave> <valor>");
                        } else {
                            estado.solicitarPut(partes[1], partes[2]);
                        }
                        break;
                    case "get":
                        if (partes.length < 2) {
                            System.out.println("Uso: get <chave>");
                        } else {
                            estado.solicitarGet(partes[1]);
                        }
                        break;
                    case "sair":
                        return;
                    default:
                        System.out.println("Comando desconhecido: " + partes[0]);
                }
            }
        } catch (IOException ex) {
            System.err.println("[ERRO] Falha na leitura do console: " + ex.getMessage());
        }
    }
}
