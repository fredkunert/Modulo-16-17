package ProjetoIntegrador.distribuidor.dto;
import jakarta.validation.constraints.NotBlank;
public record DistribuidorCriacaoRequest(@NotBlank String nome,Boolean ativo) {}
