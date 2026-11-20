package cl.elvolcan.catalogo.repository;

import cl.elvolcan.catalogo.model.Producto;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data genera la implementación a partir del nombre de cada método. */
@Repository
public interface ProductosRepository extends JpaRepository<Producto, Long> {

  List<Producto> findByActivoTrueOrderByIdAsc();

  Optional<Producto> findByIdAndActivoTrue(Long id);

  boolean existsByCodigoIgnoreCase(String codigo);

  boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

  long countByCategoriaIdAndActivoTrue(Long categoriaId);

  List<Producto> findByCategoriaId(Long categoriaId);

  /**
   * Bloquea la fila hasta que termine la transacción. Así dos compras al mismo tiempo
   * no pueden vender la misma última unidad.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Producto p where p.id = :id")
  Optional<Producto> bloquear(@Param("id") Long id);
}
