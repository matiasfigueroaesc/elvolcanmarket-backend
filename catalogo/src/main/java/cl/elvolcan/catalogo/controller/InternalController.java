package cl.elvolcan.catalogo.controller;

import cl.elvolcan.catalogo.dto.VentaItemRequest;
import cl.elvolcan.catalogo.dto.VentaResponse;
import cl.elvolcan.catalogo.service.StockService;
import cl.elvolcan.common.exception.ApiException;
import io.swagger.v3.oas.annotations.Hidden;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint servicio-a-servicio: ventas lo llama al confirmar una boleta.
 * No pasa por el Gateway y no usa JWT: se protege con la clave X-Internal-Key.
 * @Hidden lo oculta de Swagger.
 */
@Hidden
@RestController
@RequestMapping("/internal/v1/ventas")
public class InternalController {

  private final StockService stock;
  private final String clave;

  public InternalController(StockService stock, @Value("${app.internal-key}") String clave) {
    this.stock = stock;
    this.clave = clave;
  }

  @PostMapping("/{boletaId}")
  public VentaResponse vender(
      @PathVariable String boletaId,
      @RequestHeader(value = "X-Internal-Key", defaultValue = "") String recibida,
      @RequestBody List<VentaItemRequest> items) {
    // Comparación en tiempo constante (no revela cuántos caracteres coinciden).
    if (!MessageDigest.isEqual(
        clave.getBytes(StandardCharsets.UTF_8), recibida.getBytes(StandardCharsets.UTF_8)))
      throw new ApiException(403, "Servicio no autorizado.");
    if (boletaId.length() > 64) throw new ApiException(400, "Identificador de boleta inválido.");
    return stock.vender(boletaId, items);
  }
}
