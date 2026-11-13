package cl.elvolcan.ventas.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabla BOLETA del modelo de la pauta. Guarda una copia de los datos del cliente y de la
 * dirección tal como se ingresaron en el checkout (si el usuario cambia su perfil después,
 * la boleta no cambia). El usuario se guarda solo como id: vive en otro servicio.
 */
@Entity
@Table(name = "boleta")
public class Boleta {

  /** Estados de pago. "pendiente" solo existe mientras el catálogo confirma el stock. */
  public static final String PAGADO = "pagado";
  public static final String RECHAZADO = "rechazado";
  public static final String PENDIENTE = "pendiente";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  /** Número visible para el cliente: ORD-000001. Se asigna apenas se conoce el id. */
  @Column(unique = true, length = 20)
  public String numero;

  @Column(nullable = false)
  public Instant fecha;

  /** null cuando la compra se hizo como invitado. */
  public Long usuarioId;

  @Column(nullable = false, length = 50)
  public String clienteNombre;

  @Column(nullable = false, length = 80)
  public String clienteApellidos;

  @Column(nullable = false, length = 120)
  public String clienteCorreo;

  @Column(nullable = false, length = 150)
  public String calle;

  @Column(length = 50)
  public String departamento;

  @Column(nullable = false, length = 60)
  public String region;

  @Column(nullable = false, length = 60)
  public String comuna;

  @Column(length = 300)
  public String indicaciones;

  /** "normal" o "express" (src/utils/entrega.js del frontend). */
  @Column(nullable = false, length = 20)
  public String entrega;

  /** Pesos chilenos. Lo calcula el catálogo, nunca el navegador. */
  @Column(nullable = false)
  public int total;

  @Column(nullable = false, length = 20)
  public String estadoPago = PENDIENTE;

  /** false mientras el servicio despacho no confirma que recibió el pedido (se reintenta). */
  @Column(nullable = false)
  public boolean despachoRegistrado;

  @OneToMany(
      mappedBy = "boleta",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @OrderBy("id ASC")
  public List<DetalleBoleta> detalles = new ArrayList<>();

  public Boleta() {}

  public void agregar(DetalleBoleta detalle) {
    detalle.boleta = this;
    detalles.add(detalle);
  }
}
