package cl.elvolcan.usuarios.controller;

import cl.elvolcan.common.exception.ApiException;
import cl.elvolcan.usuarios.dto.AuthResultado;
import cl.elvolcan.usuarios.dto.LoginRequest;
import cl.elvolcan.usuarios.dto.RegistroRequest;
import cl.elvolcan.usuarios.dto.SesionResponse;
import cl.elvolcan.usuarios.dto.UsuarioResponse;
import cl.elvolcan.usuarios.security.JwtKeys;
import cl.elvolcan.usuarios.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticación. Login, refresh y logout usan la cookie del refresh token, por eso exigen
 * el encabezado X-Requested-With: ElVolcanMarket y un Origin permitido (protección CSRF:
 * un formulario de otro sitio no puede enviar ese encabezado).
 */
@Tag(name = "Autenticación")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  public static final String COOKIE = "ev_refresh";
  public static final String ENCABEZADO = "ElVolcanMarket";

  private final AuthService service;
  private final JwtKeys keys;
  private final boolean cookieSegura;
  private final Set<String> origenes;

  public AuthController(
      AuthService service,
      JwtKeys keys,
      @Value("${app.cookie-secure}") boolean cookieSegura,
      @Value("${app.allowed-origins}") String origenes,
      @Value("${app.gateway-url}") String gateway) {
    this.service = service;
    this.keys = keys;
    this.cookieSegura = cookieSegura;
    Set<String> permitidos = new HashSet<>(Arrays.asList(origenes.split(",")));
    permitidos.add(gateway); // Swagger del Gateway
    this.origenes = Set.copyOf(permitidos);
  }

  @Operation(summary = "Inicia sesión con correo y contraseña. Entrega el JWT y deja el refresh en cookie")
  @Parameter(name = "X-Requested-With", in = ParameterIn.HEADER, required = true,
      schema = @Schema(defaultValue = ENCABEZADO))
  @PostMapping("/login")
  public SesionResponse login(
      @Valid @RequestBody LoginRequest datos, HttpServletRequest req, HttpServletResponse res) {
    revisarOrigen(req);
    AuthResultado r = service.login(datos.correo(), datos.password());
    cookie(res, r.refresh(), AuthService.DURACION_REFRESH.toSeconds());
    return r.sesion();
  }

  @Operation(summary = "Registro público: crea siempre un cliente (201)")
  @PostMapping("/registro")
  @ResponseStatus(HttpStatus.CREATED)
  public UsuarioResponse registro(@Valid @RequestBody RegistroRequest datos) {
    return service.registrar(datos);
  }

  @Operation(summary = "Renueva el JWT usando la cookie del refresh (rota el refresh)")
  @Parameter(name = "X-Requested-With", in = ParameterIn.HEADER, required = true,
      schema = @Schema(defaultValue = ENCABEZADO))
  @PostMapping("/refresh")
  public SesionResponse refresh(
      @CookieValue(value = COOKIE, required = false) String token,
      HttpServletRequest req,
      HttpServletResponse res) {
    revisarOrigen(req);
    AuthResultado r = service.refresh(token);
    cookie(res, r.refresh(), AuthService.DURACION_REFRESH.toSeconds());
    return r.sesion();
  }

  @Operation(summary = "Cierra la sesión: invalida el refresh y borra la cookie (204)")
  @Parameter(name = "X-Requested-With", in = ParameterIn.HEADER, required = true,
      schema = @Schema(defaultValue = ENCABEZADO))
  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
      @CookieValue(value = COOKIE, required = false) String token,
      HttpServletRequest req,
      HttpServletResponse res) {
    revisarOrigen(req);
    service.logout(token);
    cookie(res, "", 0);
  }

  @Operation(summary = "Datos del usuario con sesión (requiere token)")
  @GetMapping("/me")
  public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
    return AuthService.vista(service.activo(Long.valueOf(jwt.getSubject())));
  }

  @Operation(summary = "Clave pública para validar el JWT (la usan los demás servicios)")
  @GetMapping("/jwks")
  public Map<String, Object> jwks() {
    return keys.jwksPublico();
  }

  private void revisarOrigen(HttpServletRequest req) {
    String origen = req.getHeader("Origin");
    if (!ENCABEZADO.equals(req.getHeader("X-Requested-With"))
        || (origen != null && !origenes.contains(origen)))
      throw new ApiException(403, "Origen o encabezado de sesión no permitido.");
  }

  /** HttpOnly: JavaScript no la lee. SameSite=Strict y path /api/v1/auth: solo viaja a estas rutas. */
  private void cookie(HttpServletResponse res, String valor, long segundos) {
    res.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from(COOKIE, valor)
            .httpOnly(true)
            .secure(cookieSegura)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(segundos)
            .build()
            .toString());
  }
}
