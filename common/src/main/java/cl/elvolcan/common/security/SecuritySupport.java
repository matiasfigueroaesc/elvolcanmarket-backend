package cl.elvolcan.common.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Seguridad compartida por todos los microservicios.
 *
 * Contrato del token (lo emite el servicio usuarios):
 *   - firmado con RS256; la clave pública se publica en el JWKS de usuarios
 *   - iss = app.issuer (por defecto "elvolcan-usuarios")
 *   - aud = "elvolcan-api"
 *   - roles = ["ADMIN"] | ["VENDEDOR"] | ["CLIENTE"]  → se convierten en ROLE_ADMIN, etc.
 */
public final class SecuritySupport {

  public static final String AUDIENCIA = "elvolcan-api";

  private SecuritySupport() {}

  /** Valida firma (con el JWKS de usuarios), emisor, vencimiento y audiencia. */
  public static JwtDecoder decoder(String jwksUrl, String issuer) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUrl).build();
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer),
            jwt ->
                jwt.getAudience() != null && jwt.getAudience().contains(AUDIENCIA)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Audiencia incorrecta", null))));
    return decoder;
  }

  /** Lee el claim "roles" del token y lo transforma en roles de Spring (ROLE_...). */
  public static JwtAuthenticationConverter converter() {
    JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName("roles");
    roles.setAuthorityPrefix("ROLE_");
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(roles);
    return converter;
  }

  /**
   * Configuración base: sin sesión de servidor (stateless), sin CSRF (usamos token, no
   * cookies de sesión), y respuestas 401/403 en JSON.
   */
  public static void base(HttpSecurity http) throws Exception {
    http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .oauth2ResourceServer(
            o ->
                o.jwt(j -> j.jwtAuthenticationConverter(converter()))
                    .authenticationEntryPoint(
                        (req, res, e) -> {
                          res.setStatus(401);
                          res.setContentType("application/json");
                          res.setCharacterEncoding("UTF-8");
                          res.getWriter()
                              .write(
                                  "{\"status\":401,\"mensaje\":\"Debes iniciar sesión o renovar tu"
                                      + " sesión.\"}");
                        }))
        .exceptionHandling(
            e ->
                e.accessDeniedHandler(
                    (req, res, ex) -> {
                      res.setStatus(403);
                      res.setContentType("application/json");
                      res.setCharacterEncoding("UTF-8");
                      res.getWriter()
                          .write("{\"status\":403,\"mensaje\":\"No tienes permisos para esta acción.\"}");
                    }));
  }
}
