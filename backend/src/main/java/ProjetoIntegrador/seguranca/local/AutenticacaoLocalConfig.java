package ProjetoIntegrador.seguranca.local;

import ProjetoIntegrador.comum.ApiResponse;
import ProjetoIntegrador.seguranca.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
<<<<<<< HEAD
import org.springframework.security.config.Customizer;
=======
>>>>>>> 40f9dc65ec16201a3b7dfb1e72d4833ebf4f2bc5
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.UUID;

/** Emissor efêmero de desenvolvimento. Só existe quando o perfil local é ativado. */
@Configuration
@Profile("local")
public class AutenticacaoLocalConfig {
    public static final String TOKEN_PATH = "/api/produtos/dev/token";
    private static final Set<String> LOOPBACK = Set.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1");

    @Bean
    KeyPair chavesLocais() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Bean
    JwtEncoder emissorLocal(KeyPair chavesLocais) {
        RSAKey chave = new RSAKey.Builder((RSAPublicKey) chavesLocais.getPublic())
                .privateKey((RSAPrivateKey) chavesLocais.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(chave)));
    }

    @Bean
    JwtDecoder decoderLocal(KeyPair chavesLocais,
                            @Value("${produtos.jwt.issuer}") String issuer,
                            @Value("${produtos.jwt.audience}") String audience) {
        var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) chavesLocais.getPublic()).build();
        decoder.setJwtValidator(SecurityConfig.validadores(issuer, audience));
        return decoder;
    }

    @Bean
    @Order(1)
    SecurityFilterChain rotaTokenLocal(HttpSecurity http, ObjectMapper mapper) throws Exception {
        // Não confia em X-Forwarded-For: só aceita a conexão originada no próprio computador.
        http.securityMatcher(TOKEN_PATH)
<<<<<<< HEAD
                .cors(Customizer.withDefaults())
=======
>>>>>>> 40f9dc65ec16201a3b7dfb1e72d4833ebf4f2bc5
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, TOKEN_PATH)
                        .access((authentication, context) -> new AuthorizationDecision(
                                LOOPBACK.contains(context.getRequest().getRemoteAddr())))
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            mapper.writeValue(response.getOutputStream(), ApiResponse.erro(null,
                                    "SOMENTE_LOCAL", "Emissão de token permitida somente neste computador."));
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            mapper.writeValue(response.getOutputStream(), ApiResponse.erro(null,
                                    "SOMENTE_LOCAL", "Emissão de token permitida somente neste computador."));
                        }));
        return http.build();
    }
}
