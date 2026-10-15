package ProjetoIntegrador.distribuidor.dto;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import ProjetoIntegrador.comum.Patch;
import java.util.*;
public record DistribuidorAtualizacaoRequest(Map<String,JsonNode> campos) {
    @JsonCreator(mode=JsonCreator.Mode.DELEGATING)
    public DistribuidorAtualizacaoRequest { Patch.validar(campos,Set.of("nome","ativo")); }
}
