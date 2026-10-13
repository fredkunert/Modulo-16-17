package ProjetoIntegrador.fabricante.dto;
import jakarta.validation.constraints.NotBlank;
public record FabricanteCriacaoRequest(@NotBlank String nome,Boolean ativo) {}
