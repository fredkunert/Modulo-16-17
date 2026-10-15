package br.edu.pi2026.produtos.common;

import org.springframework.http.HttpStatus;

/**
 * Erro de regra de negócio (409 ou 422, conforme o caso).
 */
public class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String campo;
    private final String codigo;

    public NegocioException(HttpStatus status, String campo, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.campo = campo;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCampo() {
        return campo;
    }

    public String getCodigo() {
        return codigo;
    }
}
