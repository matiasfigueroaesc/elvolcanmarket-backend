package cl.elvolcan.despacho.repository;

import cl.elvolcan.despacho.model.Despacho;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DespachosRepository extends JpaRepository<Despacho, Long> {

  Optional<Despacho> findByBoletaId(Long boletaId);

  Optional<Despacho> findByNumero(String numero);

  List<Despacho> findByBoletaIdIn(Collection<Long> boletaIds);

  List<Despacho> findAllByOrderByBoletaIdDesc();

  List<Despacho> findByEstadoOrderByBoletaIdDesc(String estado);
}
