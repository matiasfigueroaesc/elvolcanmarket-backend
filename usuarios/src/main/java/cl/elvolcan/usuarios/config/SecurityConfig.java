package cl.elvolcan.usuarios.config;

import cl.elvolcan.common.security.SecuritySupport;
import cl.elvolcan.usuarios.security.JwtKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permisos de usuarios (la primera regla que coincide gana):
 *  - login, registro, refresh, logout y jwks: públicos (todavía no hay token)
 *  - Swagger y salud: públicos
 *  - /auth/me: cualquier usuario con sesión
 *  - /usuarios/**: solo ADMIN
 *  - todo lo demás: denegado
 */
@Configuration
public class SecurityConfig {

  /** BCrypt: hash lento y con "sal" aleatoria; dos usuarios con la misma clave tienen hashes distintos. */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** Este servicio valida sus propios tokens con la clave pública en memoria. */
  @Bean
  JwtDecoder decoder(JwtKeys keys, @Value("${app.issuer}") String issuer) throws Exception {
    return keys.decoder(issuer);
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    SecuritySupport.base(http);
    http.authorizeHttpRequests(
        a ->
            a.requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/auth/login",
                    "/api/v1/auth/registro",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/logout")
                .permitAll()
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/auth/jwks",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/me")
                .authenticated()
                .requestMatchers("/api/v1/usuarios/**")
                .hasRole("ADMIN")
                .anyRequest()
                .denyAll());
    return http.build();
  }
}
