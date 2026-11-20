package cl.elvolcan.despacho.repository;

import cl.elvolcan.despacho.model.Zona;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZonasRepository extends JpaRepository<Zona, String> {

  /** El orden de la tabla del frontend (centro, oriente, rural, sur, comercial). */
  List<Zona> findAllByOrderByPosicionAsc();
}
