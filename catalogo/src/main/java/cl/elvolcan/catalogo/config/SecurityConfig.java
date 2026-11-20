package cl.elvolcan.catalogo.config;

import cl.elvolcan.common.security.SecuritySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permisos del catálogo (la primera regla que coincide gana):
 *  - Swagger, salud e interno (protegido por clave): abiertos
 *  - productos críticos: ADMIN y VENDEDOR
 *  - leer productos y categorías: público (la tienda no exige sesión)
 *  - crear, editar y eliminar: solo ADMIN
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
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health",
                    "/internal/v1/ventas/**")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/productos/criticos")
                .hasAnyRole("ADMIN", "VENDEDOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/productos/**", "/api/v1/categorias/**")
                .permitAll()
                .requestMatchers("/api/v1/productos/**", "/api/v1/categorias/**")
                .hasRole("ADMIN")
                .anyRequest()
                .denyAll());
    return http.build();
  }
}
