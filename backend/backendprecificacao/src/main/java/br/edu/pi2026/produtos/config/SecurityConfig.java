package br.edu.pi2026.produtos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * STUB SÓ PARA TESTE LOCAL — ver aviso em DevAuthFilter.java. Desliga a
 * validação real de JWT (que exigiria o serviço de identidade no ar) e deixa
 * o DevAuthFilter autenticar toda requisição sozinho. @EnableMethodSecurity
 * continua ligado, então os @PreAuthorize dos controllers valem normalmente
 * — só a fonte do token que é falsa.
 */
@Configuration
@Profile("local")
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChainLocal(HttpSecurity http, DevAuthFilter devAuthFilter) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(devAuthFilter, AnonymousAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
