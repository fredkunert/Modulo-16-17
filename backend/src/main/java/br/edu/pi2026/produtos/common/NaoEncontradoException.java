package br.edu.pi2026.produtos.common;

/**
 * Mapeia para 404. Usada tanto para "não existe" quanto para "pertence a outro tenant" —
 * o Contrato de Integração exige que as duas situações respondam igual (seção 8.4),
 * para não revelar a um usuário que o registro existe em outra empresa.
 */
public class NaoEncontradoException extends RuntimeException {
    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
