package cl.elvolcan.despacho.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Comuna cubierta por una zona; sector precisa qué parte (ej. "sector oriente"). */
@Embeddable
public class Cobertura {
  @Column(nullable = false, length = 60)
  public String comuna;

  @Column(length = 100)
  public String sector;

  public Cobertura() {}

  public Cobertura(String comuna, String sector) {
    this.comuna = comuna;
    this.sector = sector;
  }
}
