package cl.elvolcan.ventas.repository;

import cl.elvolcan.ventas.model.Boleta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data genera la implementación a partir del nombre de cada método. */
@Repository
public interface BoletasRepository extends JpaRepository<Boleta, Long> {

  /** Todas menos las "pendiente" (compras que no alcanzaron a confirmarse). Más nuevas primero. */
  List<Boleta> findByEstadoPagoNotOrderByIdDesc(String estadoPago);

  List<Boleta> findByUsuarioIdAndEstadoPagoNotOrderByIdDesc(Long usuarioId, String estadoPago);

  /** Boletas pagadas que el servicio despacho todavía no registra. */
  List<Boleta> findByEstadoPagoAndDespachoRegistradoFalse(String estadoPago);
}
