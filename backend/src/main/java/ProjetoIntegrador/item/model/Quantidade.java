package ProjetoIntegrador.item.model;
import ProjetoIntegrador.comum.RegraException;
import java.math.BigDecimal;
public final class Quantidade {
    private Quantidade() {}
    public static BigDecimal ler(String texto) {
        if(texto==null || texto.length()>100 || !texto.matches("-?[0-9]+(\\.[0-9]+)?"))
            throw RegraException.formato("quantidadeMinima","Informe uma string decimal com ponto, sem expoente.");
        return new BigDecimal(texto);
    }
    public static void validar(BigDecimal quantidade,UnidadeComercial unidade) {
        if(quantidade.signum()<=0) throw new RegraException(422,"quantidadeMinima","QUANTIDADE_INVALIDA","Quantidade mínima deve ser maior que zero.");
        BigDecimal n=quantidade.stripTrailingZeros();
        if(Math.max(n.scale(),0)>unidade.casasDecimais())
            throw new RegraException(400,"quantidadeMinima","PRECISAO_EXCEDIDA","Quantidade incompatível com a precisão da unidade.");
        if(n.precision()-n.scale()>11)
            throw new RegraException(400,"quantidadeMinima","VALOR_FORA_FAIXA","Quantidade excede numeric(15,4).");
    }
}
