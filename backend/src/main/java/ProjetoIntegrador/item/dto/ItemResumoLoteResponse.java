package ProjetoIntegrador.item.dto;
import java.util.*;
public record ItemResumoLoteResponse(List<ItemResumoResponse> itens,List<UUID> idsNaoEncontrados) {}
