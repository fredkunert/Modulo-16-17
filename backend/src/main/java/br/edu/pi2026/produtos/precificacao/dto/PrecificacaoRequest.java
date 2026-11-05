package br.edu.pi2026.produtos.precificacao.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Todo campo é opcional e independente — null significa "não informado",
 * nunca é tratado como zero nem preenchido automaticamente (Especificação
 * v0.2, seção 5.4, CA-11). As anotações de validação só checam FORMATO e
 * PRECISÃO (BE2-02: "validar formato, precisão... não corrigir divergências
 * matemáticas") — nenhuma delas compara um campo com outro.
 *
 * @Digits(integer, fraction) só valida quando o campo não é null, então
 * continua sendo possível deixar qualquer campo em branco.
 */
public record PrecificacaoRequest(
        @Pattern(regexp = "[A-Z]{3}", message = "Moeda deve ser um código ISO de 3 letras maiúsculas, ex.: BRL.")
        String moeda,

        // bate com custo numeric(15,2) da migration
        @Digits(integer = 13, fraction = 2, message = "Custo aceita no máximo 2 casas decimais.")
        BigDecimal custo,

        // bate com preco numeric(15,2)
        @Digits(integer = 13, fraction = 2, message = "Preço aceita no máximo 2 casas decimais.")
        BigDecimal preco,

        // bate com margem_percentual numeric(7,4)
        @Digits(integer = 3, fraction = 4, message = "Margem aceita no máximo 4 casas decimais.")
        BigDecimal margemPercentual,

        // bate com markup_valor numeric(15,4)
        @Digits(integer = 11, fraction = 4, message = "Markup aceita no máximo 4 casas decimais.")
        BigDecimal markupValor,

        @Pattern(regexp = "PERCENTUAL|FATOR", message = "Tipo de markup deve ser PERCENTUAL ou FATOR.")
        String markupTipo,

        // bate com impostos_valor numeric(15,4)
        @Digits(integer = 11, fraction = 4, message = "Impostos aceitam no máximo 4 casas decimais.")
        BigDecimal impostosValor,

        @Pattern(regexp = "PERCENTUAL|MONETARIO", message = "Tipo de impostos deve ser PERCENTUAL ou MONETARIO.")
        String impostosTipo,

        // bate com comissao_valor numeric(15,4)
        @Digits(integer = 11, fraction = 4, message = "Comissão aceita no máximo 4 casas decimais.")
        BigDecimal comissaoValor,

        @Pattern(regexp = "PERCENTUAL|MONETARIO", message = "Tipo de comissão deve ser PERCENTUAL ou MONETARIO.")
        String comissaoTipo
) {
}
