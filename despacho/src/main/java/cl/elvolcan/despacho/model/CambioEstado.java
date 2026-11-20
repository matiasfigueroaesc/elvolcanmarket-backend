package cl.elvolcan.despacho.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/** Una fila del historial: a qué estado pasó el despacho y cuándo. */
@Embeddable
public class CambioEstado {
  @Column(nullable = false, length = 20)
  public String estado;

  @Column(nullable = false)
  public Instant fecha;

  public CambioEstado() {}

  public CambioEstado(String estado, Instant fecha) {
    this.estado = estado;
    this.fecha = fecha;
  }
}
