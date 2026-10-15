package ProjetoIntegrador.item.dto;
import ProjetoIntegrador.item.model.*;
import ProjetoIntegrador.precificacao.PrecoPublicado;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
public record ItemResponse(UUID id,String sku,String nome,UUID fabricanteId,UUID distribuidorId,Categoria categoria,
    String descricao,UnidadeComercial unidade,Periodicidade periodicidade,String quantidadeMinima,
    int casasDecimaisQuantidade,boolean ativo,String moeda,BigDecimal precoVenda,BigDecimal precoMinimo,
    BigDecimal descontoMaximo,Instant criadoEm,Instant atualizadoEm) {
    public static ItemResponse from(Item i,PrecoPublicado p) {
        var r=ItemResumoResponse.from(i,p);
        return new ItemResponse(i.getId(),i.getSku(),i.getNome(),i.getFabricanteId(),i.getDistribuidorId(),i.getCategoria(),
            i.getDescricao(),i.getUnidade(),i.getPeriodicidade(),r.quantidadeMinima(),r.casasDecimaisQuantidade(),
            i.isAtivo(),"BRL",r.precoVenda(),r.precoMinimo(),r.descontoMaximo(),i.getCreatedAt(),i.getUpdatedAt());
    }
}
