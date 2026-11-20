package cl.elvolcan.despacho.controller;

import cl.elvolcan.common.exception.ApiException;
import cl.elvolcan.despacho.dto.DespachoNuevoRequest;
import cl.elvolcan.despacho.dto.DespachoResponse;
import cl.elvolcan.despacho.service.DespachosService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints servicio-a-servicio que usa ventas. No pasan por el Gateway y no usan JWT:
 * se protegen con la clave X-Internal-Key (igual que el interno del catálogo).
 * @Hidden los oculta de Swagger.
 */
@Hidden
@RestController
@RequestMapping("/internal/v1/despachos")
public class InternalController {

  private final DespachosService service;
  private final String clave;

  public InternalController(DespachosService service, @Value("${app.internal-key}") String clave) {
    this.service = service;
    this.clave = clave;
  }

  /** Crea el despacho de una boleta pagada (idempotente: repetirlo devuelve el mismo). */
  @PostMapping
  public DespachoResponse registrar(
      @RequestHeader(value = "X-Internal-Key", defaultValue = "") String recibida,
      @Valid @RequestBody DespachoNuevoRequest datos) {
    validarClave(recibida);
    return service.registrar(datos);
  }

  /** Cuerpo: [1, 2, 3] → { "1": "entregado", "2": "en camino" } */
  @PostMapping("/estados")
  public Map<Long, String> estados(
      @RequestHeader(value = "X-Internal-Key", defaultValue = "") String recibida,
      @RequestBody List<Long> boletaIds) {
    validarClave(recibida);
    return service.estados(boletaIds);
  }

  /** Comparación en tiempo constante (no revela cuántos caracteres coinciden). */
  private void validarClave(String recibida) {
    if (!MessageDigest.isEqual(
        clave.getBytes(StandardCharsets.UTF_8), recibida.getBytes(StandardCharsets.UTF_8)))
      throw new ApiException(403, "Servicio no autorizado.");
  }
}
