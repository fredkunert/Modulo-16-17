package ProjetoIntegrador.distribuidor.controller;
import ProjetoIntegrador.comum.*;
import ProjetoIntegrador.distribuidor.dto.*;
import ProjetoIntegrador.distribuidor.service.DistribuidorService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;
@RestController
@RequestMapping("/api/produtos/distribuidores")
public class DistribuidorController {
    private final DistribuidorService service;
    public DistribuidorController(DistribuidorService service) { this.service=service; }
    @PostMapping
    @PreAuthorize("hasAuthority('produtos.item.criar')")
    public ResponseEntity<ApiResponse<DistribuidorResponse>> criar(@Valid @RequestBody DistribuidorCriacaoRequest request) {
        var data=service.criar(request);
        return ResponseEntity.created(URI.create("/api/produtos/distribuidores/"+data.id())).body(ApiResponse.ok(data));
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('produtos.item.ver')")
    public ApiResponse<DistribuidorResponse> buscar(@PathVariable UUID id) { return ApiResponse.ok(service.buscar(id)); }
    @GetMapping
    @PreAuthorize("hasAuthority('produtos.item.ver')")
    public ApiResponse<PaginaResponse<DistribuidorResponse>> listar(@RequestParam(defaultValue="0") int pagina,
        @RequestParam(defaultValue="20") int tamanho,@RequestParam(defaultValue="nome,asc") String ordenar) {
        return ApiResponse.ok(service.listar(Paginacao.criar(pagina,tamanho,ordenar)));
    }
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('produtos.item.editar')")
    public ApiResponse<DistribuidorResponse> atualizar(@PathVariable UUID id,@RequestBody DistribuidorAtualizacaoRequest request) {
        return ApiResponse.ok(service.atualizar(id,request));
    }
}
