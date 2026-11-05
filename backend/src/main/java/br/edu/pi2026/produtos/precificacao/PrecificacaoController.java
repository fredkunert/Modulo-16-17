package br.edu.pi2026.produtos.precificacao;

import br.edu.pi2026.produtos.common.ApiResponse;
import br.edu.pi2026.produtos.common.PaginaResposta;
import br.edu.pi2026.produtos.common.TenantContext;
import br.edu.pi2026.produtos.precificacao.dto.PrecificacaoHistoricoResponse;
import br.edu.pi2026.produtos.precificacao.dto.PrecificacaoRequest;
import br.edu.pi2026.produtos.precificacao.dto.PrecificacaoResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/produtos/itens/{itemId}/precificacao")
public class PrecificacaoController {

    private static final String PERMISSAO_VALORES_SENSIVEIS = "produtos.precificacao.ver_valores_sensiveis";

    private final PrecificacaoService precificacaoService;
    private final TenantContext tenantContext;

    public PrecificacaoController(PrecificacaoService precificacaoService, TenantContext tenantContext) {
        this.precificacaoService = precificacaoService;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('produtos.precificacao.ver')")
    public ResponseEntity<ApiResponse<PrecificacaoResponse>> consultar(@PathVariable UUID itemId) {
        Precificacao precificacao = precificacaoService.buscar(itemId);
        boolean valoresSensiveis = tenantContext.possuiAutoridade(PERMISSAO_VALORES_SENSIVEIS);
        return ResponseEntity.ok(ApiResponse.ok(PrecificacaoResponse.de(precificacao, valoresSensiveis)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('produtos.precificacao.editar')")
    public ResponseEntity<ApiResponse<PrecificacaoResponse>> salvar(@PathVariable UUID itemId,
                                                                      @Valid @RequestBody PrecificacaoRequest request) {
        Precificacao precificacao = precificacaoService.salvar(itemId, request);
        boolean valoresSensiveis = tenantContext.possuiAutoridade(PERMISSAO_VALORES_SENSIVEIS);
        return ResponseEntity.ok(ApiResponse.ok(PrecificacaoResponse.de(precificacao, valoresSensiveis)));
    }

    @GetMapping("/historico")
    @PreAuthorize("hasAuthority('produtos.precificacao.ver')")
    public ResponseEntity<ApiResponse<PaginaResposta<PrecificacaoHistoricoResponse>>> historico(
            @PathVariable UUID itemId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho
    ) {
        int tamanhoLimitado = Math.min(tamanho, 100); // Contrato de Integração, seção 8.3
        Pageable pageable = PageRequest.of(pagina, tamanhoLimitado);
        boolean valoresSensiveis = tenantContext.possuiAutoridade(PERMISSAO_VALORES_SENSIVEIS);

        Page<PrecificacaoHistorico> paginaHistorico = precificacaoService.historico(itemId, pageable);
        Page<PrecificacaoHistoricoResponse> paginaResposta = paginaHistorico
                .map(h -> PrecificacaoHistoricoResponse.de(h, valoresSensiveis));

        return ResponseEntity.ok(ApiResponse.ok(PaginaResposta.de(paginaResposta)));
    }
}
