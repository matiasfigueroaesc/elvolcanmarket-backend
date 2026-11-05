package cl.elvolcan.catalogo.model;

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
 * Tabla PRODUCTO del modelo de la pauta. Los precios son pesos chilenos enteros.
 * Producto → Categoria es una relación JPA porque ambas tablas viven en el mismo servicio.
 */
@Entity
@Table(name = "producto")
public class Producto {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  /** Código interno, ej. CL002. Único entre los productos activos. */
  @Column(nullable = false, unique = true, length = 60)
  public String codigo;

  @Column(nullable = false, length = 150)
  public String nombre;

  @Column(nullable = false)
  public int precio;

  /** null cuando el producto no está en oferta. */
  public Integer precioOferta;

  @Column(nullable = false)
  public int stock;

  /** Si stock <= stockCritico, el producto aparece en "productos críticos". */
  @Column(nullable = false)
  public int stockCritico;

  @Column(length = 500)
  public String imagen;

  @Column(length = 1000)
  public String descripcion;

  @Column(nullable = false, length = 30)
  public String unidad = "Unidad";

  /**
   * Obligatoria para productos activos (lo valida el servicio). Puede quedar en null solo en
   * productos eliminados, para que su categoría vacía se pueda borrar.
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "categoria_id")
  public Categoria categoria;

  /** Borrado lógico: eliminar no borra la fila (las boletas antiguas siguen siendo válidas). */
  @Column(nullable = false)
  public boolean activo = true;

  public Producto() {}

  /** Precio que se cobra: el de oferta si existe. */
  public int precioFinal() {
    return precioOferta != null ? precioOferta : precio;
  }
}
