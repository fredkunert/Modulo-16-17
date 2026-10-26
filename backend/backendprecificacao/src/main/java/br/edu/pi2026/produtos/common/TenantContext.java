package br.edu.pi2026.produtos.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Único ponto de leitura de tenant_id e do usuário autenticado.
 * O valor vem sempre do token — nunca de corpo ou query string
 * (Contrato de Integração, seção 6). Mesma regra do serviço de catálogo:
 * token de usuário traz tenant_id no claim; token de serviço (subject "svc:...")
 * informa o tenant no header X-Tenant-Id e se identifica pelo claim clientId.
 *
 * tenant_id e usuário são TEXTO (não UUID): o catálogo grava assim no banco
 * compartilhado (ex.: "tenant-local", "usuario-local").
 */
@Component
public class TenantContext {

    private final HttpServletRequest request;

    public TenantContext(HttpServletRequest request) {
        this.request = request;
    }

    public String getTenantId() {
        Jwt jwt = getJwt();
        boolean servico = ehServico(jwt);
        String tenantId = servico ? request.getHeader("X-Tenant-Id") : jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank() || tenantId.length() > 255) {
            throw new NegocioException(servico ? HttpStatus.BAD_REQUEST : HttpStatus.UNAUTHORIZED,
                    "tenant_id", "TENANT_OBRIGATORIO", "Contexto de tenant ausente ou inválido.");
        }
        return tenantId;
    }

    /** Identidade gravada nas colunas de auditoria (created_by/updated_by) e no histórico. */
    public String getUsuarioId() {
        Jwt jwt = getJwt();
        String autor = ehServico(jwt) ? jwt.getClaimAsString("clientId") : jwt.getSubject();
        if (autor == null || autor.isBlank() || autor.length() > 255) {
            throw new NegocioException(HttpStatus.UNAUTHORIZED, null, "AUTOR_INVALIDO",
                    "Identidade de auditoria ausente ou inválida.");
        }
        return autor;
    }

    private boolean ehServico(Jwt jwt) {
        return jwt.getSubject() != null && jwt.getSubject().startsWith("svc:");
    }

    private Jwt getJwt() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        throw new NegocioException(HttpStatus.UNAUTHORIZED, null, "TOKEN_INVALIDO", "Token inválido.");
    }

    /**
     * Checa se o token atual tem uma autoridade específica, além da que o
     * @PreAuthorize do endpoint já exige — usado quando uma permissão extra
     * muda o CONTEÚDO da resposta (ex.: mascarar campos sensíveis) em vez de
     * bloquear o endpoint inteiro.
     */
    public boolean possuiAutoridade(String autoridade) {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao != null && autenticacao.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(autoridade));
    }
}
