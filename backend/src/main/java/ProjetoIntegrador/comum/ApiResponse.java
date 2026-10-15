package ProjetoIntegrador.comum;
import java.util.List;

public record ApiResponse<T>(boolean success, T data, String message, List<ErroCampo> errors) {
    public record ErroCampo(String campo, String codigo, String detalhe) {}
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, data, null, List.of()); }
    public static ApiResponse<Void> erro(String campo, String codigo, String detalhe) {
        return new ApiResponse<>(false, null, detalhe, List.of(new ErroCampo(campo, codigo, detalhe)));
    }
}
