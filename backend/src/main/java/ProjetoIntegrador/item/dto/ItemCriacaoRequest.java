package ProjetoIntegrador.item.dto;
import ProjetoIntegrador.item.model.*;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record ItemCriacaoRequest(@NotBlank @Size(max=64) String sku,@NotBlank String nome,
    UUID fabricanteId,UUID distribuidorId,@NotNull Categoria categoria,String descricao,
    @NotNull UnidadeComercial unidade,@NotNull Periodicidade periodicidade,@NotNull String quantidadeMinima,Boolean ativo) {}
