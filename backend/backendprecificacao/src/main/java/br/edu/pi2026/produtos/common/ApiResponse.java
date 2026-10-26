package br.edu.pi2026.produtos.common;

import java.util.List;

/**
 * Envelope de resposta obrigatório em toda a plataforma (Contrato de Integração, seção 8.2).
 */
public record ApiResponse<T>(boolean success, T data, String message, List<ApiErro> errors) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, List.of());
    }

    public static <T> ApiResponse<T> erro(String message, List<ApiErro> errors) {
        return new ApiResponse<>(false, null, message, errors);
    }

    public static <T> ApiResponse<T> erro(String message) {
        return erro(message, List.of());
    }
}
