package br.edu.pi2026.produtos.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * ====================== STUB SÓ PARA TESTE LOCAL ======================
 * Autentica toda requisição com um usuário/tenant fixo, com todas as
 * permissões da precificação — sem precisar de token real nem do serviço de
 * identidade no ar. Só entra em ação com o profile "local"
 * (SPRING_PROFILES_ACTIVE=local); em qualquer outro profile, nem é
 * registrado no contexto do Spring.
 *
 * Crie os itens no serviço de catálogo com o token local dele (tenant
 * "tenant-local"): é esse tenant que toda requisição local aqui vai carregar.
 *
 * Fora do profile "local" esta classe nem é registrada: vale a SegurancaJwtConfig,
 * que valida o token contra o JWKS do serviço de identidade.
 * =========================================================================
 */
@Component
@Profile("local")
public class DevAuthFilter extends OncePerRequestFilter {

    // Mesmos valores do token local do serviço de catálogo, para os dois enxergarem os mesmos dados.
    public static final String TENANT_ID_TESTE = "tenant-local";
    public static final String USUARIO_ID_TESTE = "usuario-local";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Jwt jwt = Jwt.withTokenValue("dev-token-local")
                .header("alg", "none")
                .subject(USUARIO_ID_TESTE)
                .claim("tenant_id", TENANT_ID_TESTE)
                .claim("nome", "Usuário de Teste Local")
                .claim("perms", List.of(
                        "produtos.precificacao.ver",
                        "produtos.precificacao.editar",
                        "produtos.precificacao.ver_valores_sensiveis"
                ))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        List<GrantedAuthority> autoridades = List.of(
                new SimpleGrantedAuthority("produtos.precificacao.ver"),
                new SimpleGrantedAuthority("produtos.precificacao.editar"),
                new SimpleGrantedAuthority("produtos.precificacao.ver_valores_sensiveis")
        );

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, autoridades));
        chain.doFilter(request, response);
    }
}
