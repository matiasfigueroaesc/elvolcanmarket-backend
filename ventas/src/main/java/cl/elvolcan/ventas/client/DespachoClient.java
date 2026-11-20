package cl.elvolcan.ventas.client;

import cl.elvolcan.ventas.dto.DespachoNuevo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Llamadas de ventas al servicio despacho (servicio a servicio, con X-Internal-Key).
 *  - registrar: al confirmar el pago se crea el despacho en estado "recibido".
 *  - estados: el estado actual de varias boletas, para responder con la forma del frontend.
 */
@Component
public class DespachoClient {

  private static final Logger log = LoggerFactory.getLogger(DespachoClient.class);

  private final RestClient http;
  private final ObjectMapper json;

  public DespachoClient(
      RestClient.Builder builder,
      ObjectMapper json,
      @Value("${app.despacho-url}") String url,
      @Value("${app.internal-key}") String clave) {
    this.json = json;
    this.http =
        builder
            .requestFactory(Http.conTiempos())
            .baseUrl(url)
            .defaultHeader("X-Internal-Key", clave)
            .build();
  }

  /** Lanza excepción si despacho no lo confirma (quien llama decide si reintentar). */
  public void registrar(DespachoNuevo despacho) {
    http.post()
        .uri("/internal/v1/despachos")
        .contentType(MediaType.APPLICATION_JSON)
        .body(despacho)
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            (req, res) -> {
              throw Http.traducir(res, json, "despacho");
            })
        .toBodilessEntity();
  }

  /**
   * { boletaId: estado }. Si despacho no responde se devuelve un mapa vacío: las boletas se
   * siguen mostrando, solo que sin estado de despacho.
   */
  public Map<Long, String> estados(List<Long> boletaIds) {
    if (boletaIds.isEmpty()) return Map.of();
    try {
      Map<Long, String> r =
          http.post()
              .uri("/internal/v1/despachos/estados")
              .contentType(MediaType.APPLICATION_JSON)
              .body(boletaIds)
              .retrieve()
              .body(new ParameterizedTypeReference<Map<Long, String>>() {});
      return r != null ? r : Map.of();
    } catch (Exception e) {
      log.warn("No se pudo consultar el estado de los despachos: {}", e.getMessage());
      return Map.of();
    }
  }
}
