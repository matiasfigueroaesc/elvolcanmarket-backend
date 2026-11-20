package cl.elvolcan.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla CATEGORIA del modelo de la pauta. */
@Entity
@Table(name = "categoria")
public class Categoria {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 80)
  public String nombre;

  /** Nombre normalizado (minúsculas, sin tildes) para no permitir nombres repetidos. */
  @Column(nullable = false, unique = true, length = 80)
  public String nombreClave;

  /** Nombre convertido a URL: "Mangueras y Conexiones" → "mangueras-y-conexiones". */
  @Column(nullable = false, unique = true, length = 100)
  public String slug;

  @Column(length = 500)
  public String descripcion;

  @Column(length = 500)
  public String imagen;

  /** JPA necesita un constructor vacío. */
  public Categoria() {}
}
