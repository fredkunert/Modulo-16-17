package ProjetoIntegrador.seguranca.local;

import ProjetoIntegrador.comum.ApiResponse;
import ProjetoIntegrador.comum.RegraException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@Profile("local")
public class TokenLocalController {
    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;
    private final String permissionsClaim;

    public TokenLocalController(JwtEncoder encoder,
                                @Value("${produtos.jwt.issuer}") String issuer,
                                @Value("${produtos.jwt.audience}") String audience,
                                @Value("${produtos.jwt.permissions-claim}") String permissionsClaim) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.audience = audience;
        this.permissionsClaim = permissionsClaim;
    }

    public record Solicitacao(String tenantId, String perfil) {}
    public record TokenResponse(String accessToken, String tokenType, long expiresIn,
                                String tenantId, List<String> permissoes) {}

    @PostMapping(AutenticacaoLocalConfig.TOKEN_PATH)
    public ResponseEntity<ApiResponse<TokenResponse>> emitir(@RequestBody(required = false) Solicitacao request) {
        String tenant = request == null || request.tenantId() == null ? "tenant-local" : request.tenantId();
        String perfil = request == null || request.perfil() == null ? "catalogo" : request.perfil();
        if (tenant.isBlank() || tenant.length() > 255) {
            throw RegraException.formato("tenantId", "Informe um tenant não vazio de até 255 caracteres.");
        }
        List<String> permissoes = switch (perfil) {
            case "catalogo" -> List.of("produtos.item.criar", "produtos.item.editar",
                    "produtos.item.ver", "produtos.item.ver_resumo");
            case "resumo" -> List.of("produtos.item.ver_resumo");
            default -> throw RegraException.formato("perfil", "Use catalogo ou resumo.");
        };
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer).subject("usuario-local").audience(List.of(audience))
                .issuedAt(agora).expiresAt(agora.plusSeconds(3600))
                .claim("tenant_id", tenant).claim(permissionsClaim, permissoes)
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(new TokenResponse(token, "Bearer", 3600, tenant, permissoes)));
    }
}
