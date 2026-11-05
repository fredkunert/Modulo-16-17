package br.edu.pi2026.produtos.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Formato de listagem paginada exigido pelo Contrato de Integração (seção 8.3).
 */
public record PaginaResposta<T>(List<T> itens, int pagina, int tamanho, long total) {

    public static <T> PaginaResposta<T> de(Page<T> page) {
        return new PaginaResposta<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
