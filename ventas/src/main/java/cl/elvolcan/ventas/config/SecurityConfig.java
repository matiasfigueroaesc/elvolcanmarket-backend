package cl.elvolcan.ventas.config;

import cl.elvolcan.common.security.SecuritySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permisos de ventas (la primera regla que coincide gana):
 *  - Swagger y salud: abiertos
 *  - crear boleta (checkout): público, porque se puede comprar como invitado
 *  - listar todas las boletas: ADMIN y VENDEDOR
 *  - mis boletas y detalle: con sesión (el servicio revisa que la boleta sea del cliente)
 *  - todo lo demás: denegado
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
                .requestMatchers(HttpMethod.POST, "/api/v1/boletas")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/boletas")
                .hasAnyRole("ADMIN", "VENDEDOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/boletas/**")
                .authenticated()
                .anyRequest()
                .denyAll());
    return http.build();
  }
}
