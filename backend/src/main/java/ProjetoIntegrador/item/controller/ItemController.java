package ProjetoIntegrador.item.controller;
import ProjetoIntegrador.comum.*;
import ProjetoIntegrador.item.dto.*;
import ProjetoIntegrador.item.model.Categoria;
import ProjetoIntegrador.item.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.*;
@RestController
@RequestMapping("/api/produtos")
public class ItemController {
    private final ItemService service;
    public ItemController(ItemService service) { this.service=service; }
    @PostMapping("/itens")
    @PreAuthorize("hasAuthority('produtos.item.criar')")
    public ResponseEntity<ApiResponse<ItemResponse>> criar(@Valid @RequestBody ItemCriacaoRequest r) {
        var data=service.criar(r);
        return ResponseEntity.created(URI.create("/api/produtos/itens/"+data.id())).body(ApiResponse.ok(data));
    }
    @GetMapping("/itens")
    @PreAuthorize("hasAuthority('produtos.item.ver')")
    public ApiResponse<PaginaResponse<ItemResponse>> listar(@RequestParam(required=false) String q,
        @RequestParam(required=false) String categoria,@RequestParam(required=false) UUID fabricanteId,
        @RequestParam(required=false) UUID distribuidorId,@RequestParam(required=false) Boolean ativo,
        @RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho,
        @RequestParam(defaultValue="nome,asc") String ordenar) {
        return ApiResponse.ok(service.listar(q,categoria==null?null:Categoria.from(categoria),fabricanteId,distribuidorId,ativo,Paginacao.criar(pagina,tamanho,ordenar)));
    }
    @GetMapping("/itens/{id}")
    @PreAuthorize("hasAuthority('produtos.item.ver')")
    public ApiResponse<ItemResponse> buscar(@PathVariable UUID id) { return ApiResponse.ok(service.buscar(id)); }
    @PatchMapping("/itens/{id}")
    @PreAuthorize("hasAuthority('produtos.item.editar')")
    public ApiResponse<ItemResponse> atualizar(@PathVariable UUID id,@RequestBody ItemAtualizacaoRequest r) { return ApiResponse.ok(service.atualizar(id,r)); }
    @GetMapping("/itens/{id}/resumo")
    @PreAuthorize("hasAuthority('produtos.item.ver_resumo')")
    public ApiResponse<ItemResumoResponse> resumo(@PathVariable UUID id) { return ApiResponse.ok(service.resumo(id)); }
    @GetMapping("/itens/resumo")
    @PreAuthorize("hasAuthority('produtos.item.ver_resumo')")
    public ApiResponse<ItemResumoLoteResponse> lote(@RequestParam String ids) { return ApiResponse.ok(service.lote(ids)); }
    @GetMapping("/busca")
    @PreAuthorize("hasAuthority('produtos.item.ver_resumo')")
    public ApiResponse<List<BuscaResultadoResponse>> busca(@RequestParam String q) { return ApiResponse.ok(service.busca(q)); }
}
