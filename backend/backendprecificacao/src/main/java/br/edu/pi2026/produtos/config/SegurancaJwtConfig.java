package br.edu.pi2026.produtos.config;

import br.edu.pi2026.produtos.common.ApiErro;
import br.edu.pi2026.produtos.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * Segurança de PRODUÇÃO (todo profile exceto "local"). Valida o JWT contra o
 * JWKS do serviço de identidade e transforma o claim de permissões em autoridades —
 * mesma regra e mesmas variáveis (JWT_ISSUER, JWT_JWKS_URI, JWT_AUDIENCE,
 * JWT_PERMISSIONS_CLAIM) do serviço de catálogo, então o MESMO token vale nos dois.
 */
@Configuration
@Profile("!local")
@EnableMethodSecurity
public class SegurancaJwtConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper mapper,
                                    JwtAuthenticationConverter converter) throws Exception {
        http.cors(Customizer.withDefaults())
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers("/error").permitAll().anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.setHeader("WWW-Authenticate", "Bearer");
                    mapper.writeValue(res.getOutputStream(), ApiResponse.erro("Token ausente ou inválido.",
                            List.of(new ApiErro(null, "TOKEN_INVALIDO", "Token ausente ou inválido."))));
                }))
            .exceptionHandling(e -> e.accessDeniedHandler((req, res, ex) -> {
                res.setStatus(403);
                res.setContentType("application/json");
                mapper.writeValue(res.getOutputStream(), ApiResponse.erro("Sem permissão para executar esta operação.",
                        List.of(new ApiErro(null, "SEM_PERMISSAO", "Permissão necessária ausente."))));
            }));
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(@Value("${produtos.jwt.permissions-claim:permissions}") String claim) {
        var autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName(claim);
        autoridades.setAuthorityPrefix("");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(autoridades);
        return converter;
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${produtos.jwt.jwk-set-uri}") String jwks,
                          @Value("${produtos.jwt.issuer}") String issuer,
                          @Value("${produtos.jwt.audience}") String audience) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwks).jwsAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(validadores(issuer, audience));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> validadores(String issuer, String audience) {
        OAuth2TokenValidator<Jwt> contexto = jwt -> {
            String sub = jwt.getSubject();
            boolean svc = sub != null && sub.startsWith("svc:");
            String identidade = jwt.getClaimAsString(svc ? "clientId" : "tenant_id");
            if (jwt.getExpiresAt() == null || sub == null || sub.isBlank() || sub.length() > 255
                    || identidade == null || identidade.isBlank() || identidade.length() > 255
                    || jwt.getAudience() == null || !jwt.getAudience().contains(audience)) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Claims obrigatórios inválidos.", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), contexto);
    }
}
