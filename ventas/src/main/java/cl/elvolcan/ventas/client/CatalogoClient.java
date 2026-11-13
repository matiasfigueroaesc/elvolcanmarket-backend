package cl.elvolcan.ventas.client;

import cl.elvolcan.common.exception.ApiException;
import cl.elvolcan.ventas.dto.CatalogoItem;
import cl.elvolcan.ventas.dto.CatalogoProducto;
import cl.elvolcan.ventas.dto.CatalogoVenta;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Llamadas de ventas al catálogo (servicio a servicio, sin pasar por el Gateway).
 * Contrato en el README: "Descuento de stock (ventas → catálogo)".
 */
@Component
public class CatalogoClient {

  private final RestClient http;
  private final ObjectMapper json;

  public CatalogoClient(
      RestClient.Builder builder,
      ObjectMapper json,
      @Value("${app.catalogo-url}") String url,
      @Value("${app.internal-key}") String clave) {
    this.json = json;
    this.http =
        builder
            .requestFactory(Http.conTiempos())
            .baseUrl(url)
            .defaultHeader("X-Internal-Key", clave)
            .build();
  }

  /**
   * Descuenta el stock y devuelve las líneas con el precio real.
   * Si se corta la red se reintenta una vez: el catálogo es idempotente por la clave de la venta,
   * así que repetir la llamada nunca descuenta dos veces.
   */
  public CatalogoVenta vender(String claveVenta, List<CatalogoItem> items) {
    try {
      return enviarVenta(claveVenta, items);
    } catch (ResourceAccessException primera) {
      try {
        return enviarVenta(claveVenta, items);
      } catch (ResourceAccessException segunda) {
        throw new ApiException(
            503, "No pudimos confirmar el stock en este momento. Intenta nuevamente.");
      }
    }
  }

  private CatalogoVenta enviarVenta(String claveVenta, List<CatalogoItem> items) {
    return http.post()
        .uri("/internal/v1/ventas/{clave}", claveVenta)
        .contentType(MediaType.APPLICATION_JSON)
        .body(items)
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            (req, res) -> {
              throw Http.traducir(res, json, "catálogo");
            })
        .body(CatalogoVenta.class);
  }

  /** Datos públicos de un producto (para la boleta de un pago rechazado). */
  public CatalogoProducto producto(Long id) {
    try {
      return http.get()
          .uri("/api/v1/productos/{id}", id)
          .retrieve()
          .onStatus(
              s -> s.value() == 404,
              (req, res) -> {
                throw new ApiException(409, "Un producto ya no está disponible.");
              })
          .onStatus(
              HttpStatusCode::isError,
              (req, res) -> {
                throw Http.traducir(res, json, "catálogo");
              })
          .body(CatalogoProducto.class);
    } catch (ResourceAccessException e) {
      throw new ApiException(503, "El catálogo no está disponible. Intenta nuevamente.");
    }
  }
}
