package cl.elvolcan.usuarios.repository;

import cl.elvolcan.usuarios.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data genera las consultas a partir del nombre de cada método. */
public interface UsuariosRepository extends JpaRepository<Usuario, Long> {
  Optional<Usuario> findByCorreo(String correo);

  Optional<Usuario> findByIdAndActivoTrue(Long id);

  List<Usuario> findByActivoTrueOrderByIdAsc();

  boolean existsByCorreo(String correo);

  boolean existsByRun(String run);

  boolean existsByCorreoAndIdNot(String correo, Long id);

  long countByTipoAndActivoTrue(String tipo);
}
