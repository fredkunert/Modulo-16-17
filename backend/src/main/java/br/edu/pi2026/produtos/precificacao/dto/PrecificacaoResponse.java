package br.edu.pi2026.produtos.precificacao.dto;

import br.edu.pi2026.produtos.precificacao.Precificacao;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PrecificacaoResponse(
        UUID itemId,
        String moeda,
        BigDecimal custo,
        BigDecimal preco,
        BigDecimal margemPercentual,
        BigDecimal markupValor,
        String markupTipo,
        BigDecimal impostosValor,
        String impostosTipo,
        BigDecimal comissaoValor,
        String comissaoTipo,
        long versao,
        OffsetDateTime updatedAt
) {
    /**
     * @param valoresSensiveisVisiveis vem de
     *   TenantContext.possuiAutoridade("produtos.precificacao.ver_valores_sensiveis").
     *   Quando falso, custo/margem/markup/comissão voltam null — preço e
     *   impostos continuam visíveis (ver CamposSensiveis).
     */
    public static PrecificacaoResponse de(Precificacao p, boolean valoresSensiveisVisiveis) {
        return new PrecificacaoResponse(
                p.getItemId(),
                p.getMoeda(),
                valoresSensiveisVisiveis ? p.getCusto() : null,
                p.getPreco(),
                valoresSensiveisVisiveis ? p.getMargemPercentual() : null,
                valoresSensiveisVisiveis ? p.getMarkupValor() : null,
                valoresSensiveisVisiveis ? p.getMarkupTipo() : null,
                p.getImpostosValor(),
                p.getImpostosTipo(),
                valoresSensiveisVisiveis ? p.getComissaoValor() : null,
                valoresSensiveisVisiveis ? p.getComissaoTipo() : null,
                p.getVersao(),
                p.getUpdatedAt()
        );
    }
}
