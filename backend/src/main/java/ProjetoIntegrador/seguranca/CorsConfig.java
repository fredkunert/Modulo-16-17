package ProjetoIntegrador.seguranca;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
/** Libera o front (outra origem) a chamar a API. Origens configuráveis por CORS_ALLOWED_ORIGINS. */
@Configuration
public class CorsConfig {
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${produtos.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:4200}") List<String> origens) {
        var cfg=new CorsConfiguration();
        cfg.setAllowedOrigins(origens);
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","X-Tenant-Id"));
        cfg.setExposedHeaders(List.of("Location"));
        cfg.setMaxAge(3600L);
        var source=new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**",cfg);
        return source;
    }
}
