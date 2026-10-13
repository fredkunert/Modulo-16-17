package ProjetoIntegrador.item.dto;
import ProjetoIntegrador.comum.Patch;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
public record ItemAtualizacaoRequest(Map<String,JsonNode> campos) {
    @JsonCreator(mode=JsonCreator.Mode.DELEGATING)
    public ItemAtualizacaoRequest {
        Patch.validar(campos,Set.of("sku","nome","fabricanteId","distribuidorId","categoria","descricao","unidade","periodicidade","quantidadeMinima","ativo"));
    }
}
