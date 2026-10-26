package br.edu.pi2026.produtos.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BE2-04. Prova que um conflito de lock otimista (duas edições da mesma
 * precificação quase ao mesmo tempo) vira 409 no envelope padrão, em vez de
 * estourar 500 ou deixar a segunda escrita sobrescrever a primeira calada.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void conflitoDeConcorrenciaVira409ComCodigoExplicito() {
        var excecao = new ObjectOptimisticLockingFailureException(
                "precificacoes", "algum-id");

        ResponseEntity<ApiResponse<Void>> resposta = handler.tratar(excecao);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().success()).isFalse();
        assertThat(resposta.getBody().errors()).hasSize(1);
        assertThat(resposta.getBody().errors().get(0).codigo()).isEqualTo("CONFLITO_CONCORRENCIA");
    }

    @Test
    void naoEncontradoVira404() {
        var excecao = new NaoEncontradoException("Item não encontrado.");

        ResponseEntity<ApiResponse<Void>> resposta = handler.tratar(excecao);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody().message()).isEqualTo("Item não encontrado.");
    }

    @Test
    void violacaoDeIntegridadeDoBancoVira422NoEnvelopePadrao() {
        var excecao = new DataIntegrityViolationException("check constraint violada");

        ResponseEntity<ApiResponse<Void>> resposta = handler.tratar(excecao);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resposta.getBody().success()).isFalse();
        assertThat(resposta.getBody().errors().get(0).codigo()).isEqualTo("INTEGRIDADE_VIOLADA");
    }
}
