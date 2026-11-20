package cl.elvolcan.ventas.client;

import cl.elvolcan.common.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/** Utilidades compartidas por los clientes HTTP de ventas. */
final class Http {
  private Http() {}

  /** Tiempos máximos: si el otro servicio no responde, el checkout no queda colgado. */
  static SimpleClientHttpRequestFactory conTiempos() {
    SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(3_000);
    f.setReadTimeout(10_000);
    return f;
  }

  /**
   * Convierte un error del otro servicio en ApiException.
   * 400 y 409 son errores de negocio (ej. "Stock insuficiente de ..."): se reenvían al
   * frontend con el mismo mensaje. Cualquier otro (403 clave interna, 500...) es un problema
   * entre servicios y se informa como 502 sin dar detalles.
   */
  static ApiException traducir(ClientHttpResponse res, ObjectMapper json, String servicio) {
    int status;
    String mensaje = null;
    try {
      status = res.getStatusCode().value();
      JsonNode cuerpo = json.readTree(res.getBody());
      if (cuerpo != null && cuerpo.hasNonNull("mensaje")) mensaje = cuerpo.get("mensaje").asText();
    } catch (Exception e) {
      status = 502;
    }
    if ((status == 400 || status == 409) && mensaje != null) return new ApiException(status, mensaje);
    return new ApiException(502, "El servicio " + servicio + " no respondió correctamente.");
  }
}
