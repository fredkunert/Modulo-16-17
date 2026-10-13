package ProjetoIntegrador.distribuidor.dto;
import ProjetoIntegrador.distribuidor.model.Distribuidor;
import java.util.UUID;
public record DistribuidorResponse(UUID id,String nome,boolean ativo) {
    public static DistribuidorResponse from(Distribuidor p) { return new DistribuidorResponse(p.getId(),p.getNome(),p.isAtivo()); }
}
