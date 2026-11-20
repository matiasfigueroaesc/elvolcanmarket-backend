package cl.elvolcan.despacho.model;

import java.util.List;

/** Etapas de un despacho, en orden (ESTADOS_ORDEN de src/data/ordenes.js del frontend). */
public final class Estados {
  private Estados() {}

  public static final String RECIBIDO = "recibido";
  public static final String PREPARANDO = "preparando";
  public static final String EN_CAMINO = "en camino";
  public static final String ENTREGADO = "entregado";

  public static final List<String> ORDEN = List.of(RECIBIDO, PREPARANDO, EN_CAMINO, ENTREGADO);

  /** Un despacho solo avanza: no se puede volver a una etapa anterior ni repetir la actual. */
  public static boolean puedeAvanzar(String actual, String nuevo) {
    return ORDEN.indexOf(nuevo) > ORDEN.indexOf(actual);
  }
}
