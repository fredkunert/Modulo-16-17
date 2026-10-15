package ProjetoIntegrador.item.dto;
import ProjetoIntegrador.item.model.*;
import ProjetoIntegrador.precificacao.PrecoPublicado;
import java.util.UUID;
import java.math.BigDecimal;
public record ItemResumoResponse(UUID id,String sku,String nome,UnidadeComercial unidade,Periodicidade periodicidade,
    String quantidadeMinima,int casasDecimaisQuantidade,boolean ativo,String moeda,
    BigDecimal precoVenda,BigDecimal precoMinimo,BigDecimal descontoMaximo) {
    public static ItemResumoResponse from(Item i,PrecoPublicado p) {
        return new ItemResumoResponse(i.getId(),i.getSku(),i.getNome(),i.getUnidade(),i.getPeriodicidade(),
            i.getQuantidadeMinima().setScale(4).toPlainString(),i.getUnidade().casasDecimais(),i.isAtivo(),"BRL",
            p==null?null:p.getPrecoVenda(),p==null?null:p.getPrecoMinimo(),p==null?null:p.getDescontoMaximo());
    }
}
