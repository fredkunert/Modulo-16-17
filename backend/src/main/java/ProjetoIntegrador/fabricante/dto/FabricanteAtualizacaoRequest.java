package ProjetoIntegrador.fabricante.dto;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import ProjetoIntegrador.comum.Patch;
import java.util.*;
public record FabricanteAtualizacaoRequest(Map<String,JsonNode> campos) {
    @JsonCreator(mode=JsonCreator.Mode.DELEGATING)
    public FabricanteAtualizacaoRequest { Patch.validar(campos,Set.of("nome","ativo")); }
}
