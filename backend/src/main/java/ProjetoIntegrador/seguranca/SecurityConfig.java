package ProjetoIntegrador.seguranca;
import ProjetoIntegrador.comum.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http,ObjectMapper mapper,JwtAuthenticationConverter converter) throws Exception {
        http.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers("/api/produtos/health","/error").permitAll().anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((req,res,e) -> {
                    res.setStatus(401); res.setContentType("application/json");
                    res.setHeader("WWW-Authenticate","Bearer");
                    mapper.writeValue(res.getOutputStream(),ApiResponse.erro(null,"TOKEN_INVALIDO","Token ausente ou inválido."));
                }))
            .exceptionHandling(e -> e.accessDeniedHandler((req,res,ex) -> {
                res.setStatus(403); res.setContentType("application/json");
                mapper.writeValue(res.getOutputStream(),ApiResponse.erro(null,"SEM_PERMISSAO","Permissão necessária ausente."));
            }));
        return http.build();
    }
    @Bean
    JwtAuthenticationConverter converter(@Value("${produtos.jwt.permissions-claim}") String claim) {
        var authorities=new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(claim); authorities.setAuthorityPrefix("");
        var converter=new JwtAuthenticationConverter(); converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
    @Bean
    @Profile("!local")
    JwtDecoder jwtDecoder(@Value("${produtos.jwt.jwk-set-uri}") String jwks,
                          @Value("${produtos.jwt.issuer}") String issuer,
                          @Value("${produtos.jwt.audience}") String audience) {
        var decoder=NimbusJwtDecoder.withJwkSetUri(jwks).jwsAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(validadores(issuer,audience));
        return decoder;
    }
    public static OAuth2TokenValidator<Jwt> validadores(String issuer,String audience) {
        OAuth2TokenValidator<Jwt> contexto=jwt -> {
            String sub=jwt.getSubject();
            boolean svc=sub!=null && sub.startsWith("svc:");
            String identity=jwt.getClaimAsString(svc?"clientId":"tenant_id");
            if(jwt.getExpiresAt()==null || sub==null || sub.isBlank() || sub.length()>255 || identity==null || identity.isBlank()
                    || identity.length()>255 || jwt.getAudience()==null || !jwt.getAudience().contains(audience))
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Claims obrigatórios inválidos.",null));
            return OAuth2TokenValidatorResult.success();
        };
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),contexto);
    }
}
