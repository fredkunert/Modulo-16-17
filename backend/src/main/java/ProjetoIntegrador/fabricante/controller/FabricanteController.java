package ProjetoIntegrador.fabricante.controller;
import ProjetoIntegrador.comum.*;
import ProjetoIntegrador.fabricante.dto.*;
import ProjetoIntegrador.fabricante.service.FabricanteService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;
@RestController
@RequestMapping("/api/produtos/fabricantes")
public class FabricanteController {
    private final FabricanteService service;
    public FabricanteController(FabricanteService service) { this.service=service; }
    @PostMapping
    @PreAuthorize("hasAuthority('produtos.item.criar')")
    public ResponseEntity<ApiResponse<FabricanteResponse>> criar(@Valid @RequestBody FabricanteCriacaoRequest request) {
        var data=service.criar(request);
        return ResponseEntity.created(URI.create("/api/produtos/fabricantes/"+data.id())).body(ApiResponse.ok(data));
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('produtos.item.ver')")
    public ApiResponse<FabricanteResponse> buscar(@PathVariable UUID id) { return ApiResponse.ok(service.buscar(id)); }
    @GetMapping
    @PreAuthorize("hasAuthority('produtos.item.ver')")
    public ApiResponse<PaginaResponse<FabricanteResponse>> listar(@RequestParam(defaultValue="0") int pagina,
        @RequestParam(defaultValue="20") int tamanho,@RequestParam(defaultValue="nome,asc") String ordenar) {
        return ApiResponse.ok(service.listar(Paginacao.criar(pagina,tamanho,ordenar)));
    }
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('produtos.item.editar')")
    public ApiResponse<FabricanteResponse> atualizar(@PathVariable UUID id,@RequestBody FabricanteAtualizacaoRequest request) {
        return ApiResponse.ok(service.atualizar(id,request));
    }
}
