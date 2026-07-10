/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package framework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorRedeDHT implements Runnable {
    private final Entidade ent;
    private final int porta;
    private boolean continua = true;

    public ServidorRedeDHT(Entidade ent, int porta) {
        this.ent = ent;
        this.porta = porta;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("[REDE] Servidor de escuta ativo na porta " + porta);
            
            while (continua) {
                // Bloqueia aguardando conexões. Quando chegam, delega para uma thread de leitura.
                Socket socketCliente = serverSocket.accept();
                new Thread(new LeitorConexao(socketCliente, ent)).start();
            }
        } catch (IOException e) {
            System.err.println("[ERRO] Falha no ServerSocket: " + e.getMessage());
        }
    }
    
    public void parar() {
        this.continua = false;
    }
}

// Classe auxiliar para ler continuamente da conexão aberta
class LeitorConexao implements Runnable {
    private final Socket socket;
    private final Entidade ent;

    public LeitorConexao(Socket socket, Entidade ent) {
        this.socket = socket;
        this.ent = ent;
    }

    @Override
    public void run() {
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String linha;
            // Lê ininterruptamente enquanto o GerenciadorConexoes do vizinho mantiver o socket aberto
            while ((linha = leitor.readLine()) != null) {
                desempacota(linha);
            }
        } catch (IOException e) {
            System.out.println("[REDE] Conexão com vizinho encerrada.");
        }
    }

    private void desempacota(String tmp) {
        if (tmp == null || tmp.trim().isEmpty()) return;

        String[] partes = tmp.split(",", 4);
        try {
            int code = Integer.parseInt(partes[0].trim());
            String c1 = (partes.length > 1 && !partes[1].trim().equals("null")) ? partes[1].trim() : null;
            String c2 = (partes.length > 2 && !partes[2].trim().equals("null")) ? partes[2].trim() : null;
            String c3 = (partes.length > 3 && !partes[3].trim().equals("null")) ? partes[3].trim() : null;

            ent.colocaEvento(new Evento(code, c1, c2, c3));
        } catch (NumberFormatException e) {
            System.err.println("[ERRO] Código de evento não reconhecido: " + tmp);
        }
    }
}