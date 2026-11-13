package cl.elvolcan.ventas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Tabla DETALLE_BOLETA del modelo de la pauta: una fila por producto comprado.
 * Nombre, código y precio se copian desde la respuesta del catálogo, así la boleta
 * sigue mostrando lo que se cobró aunque después cambie el producto.
 * El producto NO es una relación JPA: vive en otro servicio, solo se guarda su id.
 */
@Entity
@Table(name = "detalle_boleta")
public class DetalleBoleta {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "boleta_id", nullable = false)
  public Boleta boleta;

  @Column(nullable = false)
  public Long productoId;

  @Column(length = 60)
  public String codigo;

  @Column(nullable = false, length = 150)
  public String nombre;

  /** Precio unitario cobrado (con la oferta aplicada). */
  @Column(nullable = false)
  public int precio;

  @Column(nullable = false)
  public int cantidad;

  @Column(nullable = false)
  public int subtotal;

  @Column(length = 500)
  public String imagen;

  public DetalleBoleta() {}

  public DetalleBoleta(
      Long productoId, String codigo, String nombre, int precio, int cantidad, String imagen) {
    this.productoId = productoId;
    this.codigo = codigo;
    this.nombre = nombre;
    this.precio = precio;
    this.cantidad = cantidad;
    this.subtotal = precio * cantidad;
    this.imagen = imagen;
  }
}
