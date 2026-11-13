package cl.elvolcan.despacho.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Despacho de una boleta pagada. Lo crea ventas al confirmar el pago (endpoint interno).
 * La boleta y el usuario viven en otros servicios: aquí solo se guardan sus ids, sin
 * relaciones JPA. Datos personales (nombre, calle) quedan en ventas; aquí solo lo
 * necesario para despachar y hacer seguimiento.
 */
@Entity
@Table(name = "despacho")
public class Despacho {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  /** Id de la boleta en ventas. Uno a uno: una boleta tiene un solo despacho. */
  @Column(nullable = false, unique = true)
  public Long boletaId;

  /** ORD-000001, para el seguimiento público. */
  @Column(nullable = false, unique = true, length = 20)
  public String numero;

  /** Dueño de la compra (null = invitado). Permite que el cliente vea su despacho. */
  public Long usuarioId;

  @Column(nullable = false, length = 60)
  public String region;

  @Column(nullable = false, length = 60)
  public String comuna;

  /** "normal" | "express". */
  @Column(length = 20)
  public String entrega;

  /** Zona que cubre la comuna (null si la comuna no tiene cobertura todavía). */
  @Column(length = 40)
  public String zonaId;

  @Column(nullable = false, length = 20)
  public String estado = Estados.RECIBIDO;

  @Column(nullable = false)
  public Instant actualizado;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "despacho_historial", joinColumns = @JoinColumn(name = "despacho_id"))
  @OrderColumn(name = "orden")
  public List<CambioEstado> historial = new ArrayList<>();

  public Despacho() {}

  public void cambiarEstado(String nuevo, Instant fecha) {
    estado = nuevo;
    actualizado = fecha;
    historial.add(new CambioEstado(nuevo, fecha));
  }
}
