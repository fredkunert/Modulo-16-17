package ProjetoIntegrador.comum;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiErrorController implements ErrorController {
    @RequestMapping("/error")
    public ResponseEntity<ApiResponse<Void>> erro(HttpServletRequest request) {
        Object atributo=request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status=atributo instanceof Integer codigo?codigo:500;
        String mensagem=switch(status) {
            case 404 -> "Recurso não encontrado.";
            case 405 -> "Método HTTP não permitido.";
            case 415 -> "Tipo de conteúdo não suportado.";
            default -> status>=500?"Não foi possível concluir a operação.":"Requisição inválida.";
        };
        return ResponseEntity.status(status).body(ApiResponse.erro(null,"HTTP_"+status,mensagem));
    }
}
