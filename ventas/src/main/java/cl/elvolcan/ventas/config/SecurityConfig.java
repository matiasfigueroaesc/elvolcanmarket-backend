package cl.elvolcan.ventas.config;

import cl.elvolcan.common.security.SecuritySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ESQUELETO: por ahora solo están abiertos Swagger y salud; todo lo demás se deniega.
 * Al agregar endpoints, declarar aquí quién puede usarlos, antes de anyRequest().
 * Ver catalogo/config/SecurityConfig.java como ejemplo.
 */
@Configuration
public class SecurityConfig {

  @Bean
  JwtDecoder decoder(@Value("${app.jwks}") String jwks, @Value("${app.issuer}") String issuer) {
    return SecuritySupport.decoder(jwks, issuer);
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    SecuritySupport.base(http);
    http.authorizeHttpRequests(
        a ->
            a.requestMatchers(
                    "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health")
                .permitAll()
                .anyRequest()
                .denyAll());
    return http.build();
  }
}
