package ProjetoIntegrador.comum;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import java.util.List;
@RestControllerAdvice

public class GlobalExceptionHandler {
    @ExceptionHandler(RegraException.class)
    ResponseEntity<ApiResponse<Void>> regra(RegraException e) {
        return ResponseEntity.status(e.getStatus()).body(ApiResponse.erro(e.getCampo(), e.getCodigo(), e.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> validacao(MethodArgumentNotValidException e) {
        var erros=e.getBindingResult().getFieldErrors().stream()
            .map(f -> new ApiResponse.ErroCampo(f.getField(), "CAMPO_INVALIDO", f.getDefaultMessage())).toList();
        return ResponseEntity.badRequest().body(new ApiResponse<>(false,null,"Dados inválidos.",erros));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> json(HttpMessageNotReadableException e) {
        for (Throwable t=e; t!=null; t=t.getCause()) {
            if(t instanceof RegraException r) return regra(r);
            if(t instanceof UnrecognizedPropertyException u)
                return ResponseEntity.badRequest().body(ApiResponse.erro(u.getPropertyName(),"CAMPO_NAO_PERMITIDO","Campo não permitido nesta rota."));
        }
        return ResponseEntity.badRequest().body(ApiResponse.erro(null,"FORMATO_INVALIDO","JSON, tipo de campo ou enum inválido."));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ApiResponse<Void>> parametro(Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.erro(null,"PARAMETRO_INVALIDO","Parâmetro obrigatório ausente ou inválido."));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> integridade(DataIntegrityViolationException e) {
        String causa=String.valueOf(e.getMostSpecificCause().getMessage());
        boolean sku=causa.toLowerCase(java.util.Locale.ROOT).contains("uk_itens_tenant_sku");
        return ResponseEntity.status(409).body(ApiResponse.erro(sku?"sku":null,sku?"SKU_DUPLICADO":"CONFLITO_INTEGRIDADE",
            sku?"SKU já cadastrado neste tenant.":"Operação em conflito com os dados cadastrados."));
    }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiResponse<Void>> concorrencia(Exception e) {
        return ResponseEntity.status(409).body(ApiResponse.erro(null,"ALTERACAO_CONCORRENTE","O registro foi alterado. Consulte novamente antes de editar."));
    }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> proibido(Exception e) {
        return ResponseEntity.status(403).body(ApiResponse.erro(null,"SEM_PERMISSAO","Permissão necessária ausente."));
    }
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiResponse<Void>> autenticacao(Exception e) {
        return ResponseEntity.status(401).body(ApiResponse.erro(null,"NAO_AUTENTICADO","Token ausente ou inválido."));
    }
}
