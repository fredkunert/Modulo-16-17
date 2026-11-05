package br.edu.pi2026.produtos.precificacao;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * A API (e o front) falam MONETARIO; a tabela compartilhada produtos.precificacoes,
 * criada pelo catálogo, usa VALOR no CHECK de impostos_tipo/comissao_tipo.
 * A tradução fica só aqui — o contrato da API não muda.
 */
@Converter
public class TipoValorConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String valorDaApi) {
        return "MONETARIO".equals(valorDaApi) ? "VALOR" : valorDaApi;
    }

    @Override
    public String convertToEntityAttribute(String valorDoBanco) {
        return "VALOR".equals(valorDoBanco) ? "MONETARIO" : valorDoBanco;
    }
}
