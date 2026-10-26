package br.edu.pi2026.produtos.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Traduz toda exceção para o envelope de resposta padrão, inclusive as de erro
 * (Contrato de Integração, seção 8.2) e os códigos HTTP da seção 8.4.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> tratar(ObjectOptimisticLockingFailureException ex) {
        ApiErro erro = new ApiErro(null, "CONFLITO_CONCORRENCIA",
                "Este registro foi alterado por outra pessoa enquanto você editava. Recarregue e tente novamente.");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.erro("Conflito de concorrência ao salvar.", List.of(erro)));
    }

    /** Rede de segurança: regra de integridade do banco violada que a validação da API não pegou. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> tratar(DataIntegrityViolationException ex) {
        ApiErro erro = new ApiErro(null, "INTEGRIDADE_VIOLADA",
                "Os valores informados violam uma regra de integridade dos dados.");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.erro("Dados inconsistentes.", List.of(erro)));
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> tratar(NaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.erro(ex.getMessage()));
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ApiResponse<Void>> tratar(NegocioException ex) {
        ApiErro erro = new ApiErro(ex.getCampo(), ex.getCodigo(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.erro(ex.getMessage(), List.of(erro)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> tratar(MethodArgumentNotValidException ex) {
        List<ApiErro> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErro(fe.getField(), "CAMPO_INVALIDO", fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.erro("Dados inválidos.", erros));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> tratar(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.erro("Sem permissão para executar esta operação."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> tratar(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.erro("Erro inesperado ao processar a requisição."));
    }
}
