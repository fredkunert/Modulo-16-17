package ProjetoIntegrador.comum;
public class RegraException extends RuntimeException {
    private final int status;
    private final String campo, codigo;
    public RegraException(int status, String campo, String codigo, String mensagem) {
        super(mensagem); this.status=status; this.campo=campo; this.codigo=codigo;
    }
    public int getStatus() { return status; }
    public String getCampo() { return campo; }
    public String getCodigo() { return codigo; }
    public static RegraException formato(String campo, String mensagem) {
        return new RegraException(400, campo, "FORMATO_INVALIDO", mensagem);
    }
    public static RegraException naoEncontrado() {
        return new RegraException(404, "id", "RECURSO_NAO_ENCONTRADO", "Recurso não encontrado.");
    }
}
