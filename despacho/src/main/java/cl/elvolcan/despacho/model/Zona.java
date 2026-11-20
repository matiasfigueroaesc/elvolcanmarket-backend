package cl.elvolcan.despacho.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Zona de despacho, igual que ZONAS_DESPACHO de src/utils/zonasDespacho.js del frontend.
 * El id es texto ("centro", "oriente"...) para conservar los mismos valores del frontend.
 */
@Entity
@Table(name = "zona")
public class Zona {
  @Id
  @Column(length = 40)
  public String id;

  @Column(nullable = false, length = 80)
  public String nombre;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "zona_cobertura", joinColumns = @JoinColumn(name = "zona_id"))
  @OrderColumn(name = "orden")
  public List<Cobertura> cobertura = new ArrayList<>();

  /** Texto libre para zonas que no se definen por comuna (Zona Comercial). */
  @Column(length = 200)
  public String detalle;

  @Column(nullable = false, length = 60)
  public String dias;

  @Column(nullable = false, length = 40)
  public String horario;

  @Column(nullable = false, length = 40)
  public String tiempo;

  /** Orden en que se muestran las zonas (las nuevas quedan al final). */
  @Column(nullable = false)
  public int posicion;

  public Zona() {}

  public boolean cubre(String comuna) {
    return cobertura.stream().anyMatch(c -> c.comuna.equals(comuna));
  }
}
