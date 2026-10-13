package ProjetoIntegrador;
import ProjetoIntegrador.seguranca.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
class JwtValidationTest {
    Jwt.Builder token() {
        return Jwt.withTokenValue("test").header("alg","RS256").subject("user")
            .issuer("https://auth.test").audience(List.of("produtos"))
            .claim("tenant_id","a").expiresAt(Instant.now().plusSeconds(300));
    }
    @Test void validaIssuerAudienceExpiracaoEContexto() {
        var validator=SecurityConfig.validadores("https://auth.test","produtos");
        assertThat(validator.validate(token().build()).hasErrors()).isFalse();
        assertThat(validator.validate(token().issuer("https://outro.test").build()).hasErrors()).isTrue();
        assertThat(validator.validate(token().audience(List.of("outro")).build()).hasErrors()).isTrue();
        assertThat(validator.validate(token().claims(c->c.remove("aud")).build()).hasErrors()).isTrue();
        assertThat(validator.validate(token().expiresAt(Instant.now().minusSeconds(120)).build()).hasErrors()).isTrue();
        assertThat(validator.validate(token().claims(c->c.remove("exp")).build()).hasErrors()).isTrue();
        assertThat(validator.validate(token().claims(c->c.remove("tenant_id")).build()).hasErrors()).isTrue();
        assertThat(validator.validate(token().subject("svc:crm").claim("clientId","crm").claims(c->c.remove("tenant_id")).build()).hasErrors()).isFalse();
        assertThat(validator.validate(token().subject("svc:crm").build()).hasErrors()).isTrue();
    }
}
