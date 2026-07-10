/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package framework;

public class SocketThreadDHT extends SocketThread {

    public SocketThreadDHT(Msg _m, Entidade _u) {
        super(_m, _u);
    }

    @Override
    public void desempacota() {
        if (tmp == null || tmp.trim().isEmpty()) {
            return;
        }

        // O parâmetro de limite 4 evita que vírgulas no campo de payload (C3) quebrem a estrutura
        String[] partes = tmp.split(",", 4);

        try {
            int code = Integer.parseInt(partes[0].trim());
            String c1 = (partes.length > 1 && !partes[1].trim().equals("null")) ? partes[1].trim() : null;
            String c2 = (partes.length > 2 && !partes[2].trim().equals("null")) ? partes[2].trim() : null;
            String c3 = (partes.length > 3 && !partes[3].trim().equals("null")) ? partes[3].trim() : null;

            Evento novoEvento = new Evento(code, c1, c2, c3);
            ent.colocaEvento(novoEvento);

        } catch (NumberFormatException e) {
            System.err.println("[ERRO] Código de evento não reconhecido: " + tmp);
        }
    }
}