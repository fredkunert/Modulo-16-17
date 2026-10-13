package ProjetoIntegrador.fabricante.dto;
import ProjetoIntegrador.fabricante.model.Fabricante;
import java.util.UUID;
public record FabricanteResponse(UUID id,String nome,boolean ativo) {
    public static FabricanteResponse from(Fabricante p) { return new FabricanteResponse(p.getId(),p.getNome(),p.isAtivo()); }
}
