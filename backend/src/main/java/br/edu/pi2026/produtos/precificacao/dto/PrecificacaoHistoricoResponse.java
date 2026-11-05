package br.edu.pi2026.produtos.precificacao.dto;

import br.edu.pi2026.produtos.precificacao.CamposSensiveis;
import br.edu.pi2026.produtos.precificacao.PrecificacaoHistorico;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * valorAnterior/valorNovo são Object de propósito: o histórico guarda tudo
 * como texto no banco (uma linha pode ser tanto um campo decimal quanto um
 * campo de tipo), mas a API precisa devolver número quando o campo é
 * decimal — "valores decimais na API devem ser números, não texto" (ajuste
 * na Especificação). O Jackson serializa BigDecimal como número e String
 * como string automaticamente, então a escolha do tipo em tempo de
 * execução já resolve o formato do JSON.
 */
public record PrecificacaoHistoricoResponse(
        String campoAlterado, Object valorAnterior, Object valorNovo,
        String alteradoPor, OffsetDateTime alteradoEm
) {
    private static final Set<String> CAMPOS_DECIMAIS = Set.of(
            "custo", "preco", "margemPercentual", "markupValor", "impostosValor", "comissaoValor"
    );

    /**
     * A linha do histórico continua aparecendo (prova que algo mudou), mas
     * valorAnterior/valorNovo vêm null se o campo alterado for sensível e o
     * chamador não tiver produtos.precificacao.ver_valores_sensiveis.
     */
    public static PrecificacaoHistoricoResponse de(PrecificacaoHistorico h, boolean valoresSensiveisVisiveis) {
        boolean oculto = CamposSensiveis.NOMES.contains(h.getCampoAlterado()) && !valoresSensiveisVisiveis;
        return new PrecificacaoHistoricoResponse(
                h.getCampoAlterado(),
                oculto ? null : converter(h.getCampoAlterado(), h.getValorAnterior()),
                oculto ? null : converter(h.getCampoAlterado(), h.getValorNovo()),
                h.getAlteradoPor(),
                h.getAlteradoEm()
        );
    }

    private static Object converter(String campo, String valorTexto) {
        if (valorTexto == null) {
            return null;
        }
        if (CAMPOS_DECIMAIS.contains(campo)) {
            return new BigDecimal(valorTexto);
        }
        return valorTexto;
    }
}
