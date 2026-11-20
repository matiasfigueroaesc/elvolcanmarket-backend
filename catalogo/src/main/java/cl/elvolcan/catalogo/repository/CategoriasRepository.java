package cl.elvolcan.catalogo.repository;

import cl.elvolcan.catalogo.model.Categoria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriasRepository extends JpaRepository<Categoria, Long> {

  List<Categoria> findAllByOrderByIdAsc();

  Optional<Categoria> findBySlug(String slug);

  boolean existsByNombreClave(String nombreClave);

  boolean existsByNombreClaveAndIdNot(String nombreClave, Long id);

  boolean existsBySlugAndIdNot(String slug, Long id);
}
