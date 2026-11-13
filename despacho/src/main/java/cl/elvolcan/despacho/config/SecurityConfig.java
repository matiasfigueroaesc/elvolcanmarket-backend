package cl.elvolcan.despacho.config;

import cl.elvolcan.common.security.SecuritySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permisos de despacho (la primera regla que coincide gana):
 *  - Swagger, salud e interno (protegido por clave): abiertos
 *  - leer zonas y seguimiento por número: público
 *  - crear, editar y eliminar zonas: ADMIN
 *  - listar despachos: ADMIN y VENDEDOR
 *  - ver un despacho: con sesión (el servicio revisa que sea del cliente)
 *  - cambiar el estado: ADMIN (el vendedor solo lee órdenes)
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
                    "/internal/v1/despachos/**")
                .permitAll()
                .requestMatchers(
                    HttpMethod.GET, "/api/v1/zonas/**", "/api/v1/despachos/seguimiento/**")
                .permitAll()
                .requestMatchers("/api/v1/zonas/**")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/despachos")
                .hasAnyRole("ADMIN", "VENDEDOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/despachos/**")
                .authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/v1/despachos/**")
                .hasRole("ADMIN")
                .anyRequest()
                .denyAll());
    return http.build();
  }
}
