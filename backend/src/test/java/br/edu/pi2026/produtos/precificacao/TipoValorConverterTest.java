package br.edu.pi2026.produtos.precificacao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TipoValorConverterTest {

    private final TipoValorConverter conversor = new TipoValorConverter();

    @Test
    void monetarioDaApiViraValorNoBanco() {
        assertThat(conversor.convertToDatabaseColumn("MONETARIO")).isEqualTo("VALOR");
    }

    @Test
    void valorDoBancoVoltaComoMonetarioNaApi() {
        assertThat(conversor.convertToEntityAttribute("VALOR")).isEqualTo("MONETARIO");
    }

    @Test
    void percentualENuloPassamSemMudanca() {
        assertThat(conversor.convertToDatabaseColumn("PERCENTUAL")).isEqualTo("PERCENTUAL");
        assertThat(conversor.convertToEntityAttribute("PERCENTUAL")).isEqualTo("PERCENTUAL");
        assertThat(conversor.convertToDatabaseColumn(null)).isNull();
        assertThat(conversor.convertToEntityAttribute(null)).isNull();
    }
}
