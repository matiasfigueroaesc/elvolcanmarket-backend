package cl.elvolcan.ventas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Si despacho estaba caído al momento de pagar, la boleta queda con despachoRegistrado=false.
 * Esta tarea revisa cada minuto (app.reintento-despacho-ms) y vuelve a avisar a despacho.
 * En las pruebas se apaga con app.reintentos=false y se llama al servicio directamente.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.reintentos", havingValue = "true", matchIfMissing = true)
public class DespachosPendientes {

  private static final Logger log = LoggerFactory.getLogger(DespachosPendientes.class);

  private final BoletasService service;

  public DespachosPendientes(BoletasService service) {
    this.service = service;
  }

  @Scheduled(
      fixedDelayString = "${app.reintento-despacho-ms:60000}",
      initialDelayString = "${app.reintento-despacho-ms:60000}")
  public void reintentar() {
    int n = service.reintentarDespachosPendientes();
    if (n > 0) log.info("Se registraron {} despachos pendientes.", n);
  }
}
