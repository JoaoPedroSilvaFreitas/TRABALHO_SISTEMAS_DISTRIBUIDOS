/*
 * FRAMEWORK:
 * Classe que implementa o conceito de EVENTO.
 */
package framework;

public class Evento {
    public int code;
    public String C1;
    public String C2;
    public String C3;
    public Evento(int _c, String _n, String _m, String _p){
        code=_c;
        C1=_n;
        C2=_m;
        C3=_p;
    }
    @Override
    public String toString(){
        return String.valueOf(code)+","+C1+","+C2+","+C3;    
    }
}
