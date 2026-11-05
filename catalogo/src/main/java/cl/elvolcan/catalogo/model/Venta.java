package cl.elvolcan.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Registro de cada descuento de stock pedido por ventas (idempotencia).
 * Si ventas reintenta la misma boleta (por ejemplo, por un corte de red),
 * se devuelve el mismo resultado y el stock NO se descuenta dos veces.
 */
@Entity
@Table(name = "venta_stock")
public class Venta {
  /** Identificador de la boleta que generó ventas. */
  @Id
  @Column(length = 64)
  public String id;

  /** Productos y cantidades pedidos, para detectar un reintento con otros datos. */
  @Column(nullable = false, length = 2000)
  public String huella;

  /** Respuesta entregada la primera vez, en JSON. */
  @Lob
  @Column(nullable = false)
  public String comprobante;

  public Venta() {}

  public Venta(String id, String huella, String comprobante) {
    this.id = id;
    this.huella = huella;
    this.comprobante = comprobante;
  }
}
