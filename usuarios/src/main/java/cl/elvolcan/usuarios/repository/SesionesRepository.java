package cl.elvolcan.usuarios.repository;

import cl.elvolcan.usuarios.model.SesionRefresh;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SesionesRepository extends JpaRepository<SesionRefresh, String> {

  /**
   * Bloquea la fila mientras dura la transacción: si llegan dos refresh con el mismo
   * token al mismo tiempo, solo uno lo puede usar.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from SesionRefresh s where s.tokenHash = :hash")
  Optional<SesionRefresh> bloquear(String hash);

  /** Cierra todas las sesiones de un usuario (al editarlo o eliminarlo). */
  void deleteByUsuarioId(Long usuarioId);
}
