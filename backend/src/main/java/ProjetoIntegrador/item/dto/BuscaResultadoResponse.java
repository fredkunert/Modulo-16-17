package ProjetoIntegrador.item.dto;
import ProjetoIntegrador.item.model.Item;
import java.util.UUID;
public record BuscaResultadoResponse(UUID id,String titulo,String subtitulo,String rota) {
    public static BuscaResultadoResponse from(Item i) {
        return new BuscaResultadoResponse(i.getId(),i.getNome(),i.getSku()+" · "+i.getUnidade()+" · "+i.getPeriodicidade(),"/itens/"+i.getId());
    }
}
