/*
 * FRAMEWORK:
 *   Classe para tratamento do buffer de mensagens
 *   do Socket.
 */
package framework;

public class EventoThread implements Runnable {
    public Entidade ent;
    public EventoThread(Entidade _ent) {
      ent=_ent;
    }
    @Override
    public void run() {   
        ent.trabalha();
    }
}