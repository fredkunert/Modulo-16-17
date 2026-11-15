package ProjetoIntegrador.item.controller;
import ProjetoIntegrador.comum.ApiResponse;
import ProjetoIntegrador.item.dto.ItemPublicoResponse;
import ProjetoIntegrador.item.service.CompartilhamentoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/produtos")
public class CompartilhamentoController {
    private final CompartilhamentoService service;
    public CompartilhamentoController(CompartilhamentoService service) { this.service=service; }
    public record LinkPublico(UUID token) {}
    @PostMapping("/itens/{id}/compartilhamento")
    @PreAuthorize("hasAuthority('produtos.item.editar')")
    public ResponseEntity<ApiResponse<LinkPublico>> compartilhar(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(new LinkPublico(service.compartilhar(id))));
    }
    @DeleteMapping("/itens/{id}/compartilhamento")
    @PreAuthorize("hasAuthority('produtos.item.editar')")
    public ApiResponse<Void> revogar(@PathVariable UUID id) {
        service.revogar(id); return ApiResponse.ok(null);
    }
    @GetMapping("/publico/itens/{token}")
    public ResponseEntity<ApiResponse<ItemPublicoResponse>> consultar(@PathVariable UUID token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(service.consultar(token)));
    }
}
