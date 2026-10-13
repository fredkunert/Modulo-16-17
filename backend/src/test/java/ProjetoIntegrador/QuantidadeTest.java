package ProjetoIntegrador;
import ProjetoIntegrador.item.model.*;
import ProjetoIntegrador.comum.RegraException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class QuantidadeTest {
    @Test void validaTodasAsUnidades() {
        for(var unidade:UnidadeComercial.values()) {
            assertThatCode(()->Quantidade.validar(Quantidade.ler("1.0000"),unidade)).doesNotThrowAnyException();
            if(unidade.casasDecimais()==0)
                assertThatThrownBy(()->Quantidade.validar(Quantidade.ler("1.5"),unidade)).isInstanceOf(RegraException.class);
            else assertThatCode(()->Quantidade.validar(Quantidade.ler("0.0001"),unidade)).doesNotThrowAnyException();
        }
    }
    @Test void rejeitaPrecisaoEOverflowSemArredondar() {
        assertThatThrownBy(()->Quantidade.validar(Quantidade.ler("1.00001"),UnidadeComercial.GB))
            .isInstanceOfSatisfying(RegraException.class,e->assertThat(e.getCodigo()).isEqualTo("PRECISAO_EXCEDIDA"));
        assertThatThrownBy(()->Quantidade.validar(Quantidade.ler("100000000000"),UnidadeComercial.GB))
            .isInstanceOfSatisfying(RegraException.class,e->assertThat(e.getCodigo()).isEqualTo("VALOR_FORA_FAIXA"));
        assertThatCode(()->Quantidade.validar(Quantidade.ler("99999999999.9999"),UnidadeComercial.GB)).doesNotThrowAnyException();
    }
}
