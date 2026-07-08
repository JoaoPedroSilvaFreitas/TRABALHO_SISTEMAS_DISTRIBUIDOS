/*
 * FRAMEWORK:
 * Essa classe implementa o conceito de ENTIDADE de um protocolo.
 */
package framework;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Entidade { 
   public Queue<Evento> buffer;
   private Estado corrente;
   public Msg msg;
   public EventoThread ethread; 
   public Thread thread1;
   
   public void defPortaLocal(int p){
        msg =new Msg();
        msg.lPort=p;
   }
   
   public Entidade(){
       buffer = new ArrayDeque<>();
       // inicia thread de tratamento de eventos
       ethread = new EventoThread(this);
       thread1=new Thread(ethread);
       thread1.start();
   }
   
   public void transicao(Evento _e){
       corrente.transicao(_e);
   }
   
   public Evento pegaEvento(){
       Evento e = null;
       if(!buffer.isEmpty()){
           e = buffer.poll();
       }
       return e;
   }
   
   public void colocaEvento(Evento _e){
        buffer.add(_e);
   } 
   
   public void trabalha(){
        Evento e;
        while(true){            
            e = pegaEvento();
            while (e!=null){
                transicao(e);
                e=pegaEvento();                
            }
            try {
                Thread.sleep(70);
            } catch (InterruptedException ex) {
                Logger.getLogger(Entidade.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
   
   public void mudaEstado(Estado _e){
       corrente=_e;
       // Excuta ação de entrada
       corrente.acao();
   }
}
