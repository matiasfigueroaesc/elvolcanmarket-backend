package cl.elvolcan.despacho.controller;

import cl.elvolcan.despacho.dto.DespachoResponse;
import cl.elvolcan.despacho.dto.EstadoRequest;
import cl.elvolcan.despacho.dto.SeguimientoResponse;
import cl.elvolcan.despacho.service.DespachosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de despachos. Se identifican por el id de la boleta (el mismo "id" de la orden
 * en el frontend), así el panel admin usa /admin/ordenes/:id para ambos servicios.
 */
@Tag(name = "Despachos")
@RestController
@RequestMapping("/api/v1/despachos")
public class DespachosController {

  private final DespachosService service;

  public DespachosController(DespachosService service) {
    this.service = service;
  }

  @Operation(summary = "Seguimiento público por número de pedido (ej. ORD-000001), sin datos personales")
  @GetMapping("/seguimiento/{numero}")
  public SeguimientoResponse seguimiento(@PathVariable String numero) {
    return service.seguimiento(numero);
  }

  @Operation(summary = "Lista de despachos (ADMIN, VENDEDOR). Filtro opcional: estado")
  @GetMapping
  public List<DespachoResponse> listar(@RequestParam(required = false) String estado) {
    return service.listar(estado);
  }

  @Operation(summary = "Despacho de una boleta (ADMIN y VENDEDOR cualquiera; CLIENTE solo el suyo)")
  @GetMapping("/{boletaId}")
  public DespachoResponse obtener(@PathVariable Long boletaId, Authentication auth) {
    return service.obtener(boletaId, idOpcional(auth.getName()), esPersonal(auth));
  }

  @Operation(summary = "Avanza el estado del despacho (ADMIN). Solo hacia adelante")
  @PatchMapping("/{boletaId}/estado")
  public DespachoResponse cambiarEstado(
      @PathVariable Long boletaId, @Valid @RequestBody EstadoRequest datos) {
    return service.cambiarEstado(boletaId, datos.estado());
  }

  /** El "sub" del JWT es el id del usuario. */
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
