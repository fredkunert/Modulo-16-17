package ProjetoIntegrador.item.controller;

import ProjetoIntegrador.comum.ApiResponse;
import ProjetoIntegrador.item.service.CompartilhamentoService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/public/produtos/itens")
public class ProdutoPublicoController {

    private final CompartilhamentoService service;

    public ProdutoPublicoController(CompartilhamentoService service) {
        this.service = service;
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> consultar(@PathVariable UUID token) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(ApiResponse.ok(service.consultar(token)));
    }
}