package cl.elvolcan.ventas.controller;

import cl.elvolcan.common.exception.ApiException;
import cl.elvolcan.ventas.dto.BoletaRequest;
import cl.elvolcan.ventas.dto.BoletaResponse;
import cl.elvolcan.ventas.service.BoletasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de boletas. Quién puede llamar a cada uno se define en SecurityConfig;
 * que un cliente solo vea SUS boletas lo controla el servicio.
 */
@Tag(name = "Boletas")
@RestController
@RequestMapping("/api/v1/boletas")
public class BoletasController {

  private final BoletasService service;

  public BoletasController(BoletasService service) {
    this.service = service;
  }

  @Operation(
      summary =
          "Checkout (público). Con sesión la boleta queda a nombre del usuario; sin sesión, como"
              + " invitado. Responde 201 también si el pago fue rechazado (estadoPago=rechazado)")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BoletaResponse crear(
      @Valid @RequestBody BoletaRequest datos, @AuthenticationPrincipal Jwt jwt) {
    return service.crear(datos, jwt != null ? id(jwt.getSubject()) : null);
  }

  @Operation(summary = "Mis compras: boletas del usuario con sesión, más nuevas primero")
  @GetMapping("/mias")
  public List<BoletaResponse> mias(Authentication auth) {
    return service.listarDeUsuario(id(auth.getName()));
  }

  @Operation(
      summary = "Lista de boletas (ADMIN, VENDEDOR). Filtros opcionales: texto, estadoPago, usuarioId")
  @GetMapping
  public List<BoletaResponse> listar(
      @RequestParam(required = false) String texto,
      @RequestParam(required = false) String estadoPago,
      @RequestParam(required = false) Long usuarioId) {
    return service.listar(texto, estadoPago, usuarioId);
  }

  @Operation(summary = "Detalle de una boleta (ADMIN y VENDEDOR cualquiera; CLIENTE solo las suyas)")
  @GetMapping("/{id}")
  public BoletaResponse obtener(@PathVariable Long id, Authentication auth) {
    return service.obtener(id, idOpcional(auth.getName()), esPersonal(auth));
  }

  // ---------- Datos del token ----------

  /** El "sub" del JWT es el id del usuario (lo define el servicio usuarios). */
  private static Long id(String subject) {
    Long id = idOpcional(subject);
    if (id == null) throw new ApiException(401, "Debes iniciar sesión o renovar tu sesión.");
    return id;
  }

  private static Long idOpcional(String subject) {
    try {
      return Long.valueOf(subject);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static boolean esPersonal(Authentication auth) {
    return auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_VENDEDOR"));
  }
}
