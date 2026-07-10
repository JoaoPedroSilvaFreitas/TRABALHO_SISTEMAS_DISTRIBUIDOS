/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package framework;

import java.util.concurrent.ConcurrentHashMap;

public class GerenciadorConexoes {
    
    // Mapeia o destino ("IP:Porta") para a instância de Msg conectada
    private final ConcurrentHashMap<String, Msg> conexoesAtivas = new ConcurrentHashMap<>();

    public void enviarMensagem(String destino, Evento ev) {
        if (destino == null || !destino.contains(":")) {
            System.err.println("[ERRO] Destino mal formatado: " + destino);
            return;
        }

        // Recupera a conexão existente ou cria uma nova atomicamente
        Msg conexao = conexoesAtivas.computeIfAbsent(destino, this::criarNovaConexao);

        if (conexao != null) {
            // Tenta enviar. A quebra de linha (\n) é exigida pelo BufferedReader do recebedor
            int status = conexao.envia(ev.toString() + "\n");
            
            // O código original de Msg.envia() retorna 1 em caso de IOException (Broken pipe, etc)
            if (status == 1) {
                System.err.println("[ERRO] Falha no envio (Socket quebrado) para " + destino + ". Removendo do cache.");
                conexao.termina();
                conexoesAtivas.remove(destino);
            }
        }
    }

    private Msg criarNovaConexao(String destino) {
        String[] partes = destino.split(":");
        String ip = partes[0];
        int porta = Integer.parseInt(partes[1]);

        Msg novaMsg = new Msg();
        if (novaMsg.conecta(ip, porta) == 0) {
            return novaMsg;
        } else {
            System.err.println("[ERRO] Conexão recusada ao tentar alcançar: " + destino);
            return null;
        }
    }
    
    public void fecharTodas() {
        for (Msg m : conexoesAtivas.values()) {
            m.termina();
        }
        conexoesAtivas.clear();
    }
}