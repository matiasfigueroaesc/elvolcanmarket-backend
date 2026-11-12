package cl.elvolcan.catalogo.service;

import cl.elvolcan.catalogo.dto.CategoriaRequest;
import cl.elvolcan.catalogo.dto.CategoriaResponse;
import cl.elvolcan.catalogo.model.Categoria;
import cl.elvolcan.catalogo.model.Producto;
import cl.elvolcan.catalogo.repository.CategoriasRepository;
import cl.elvolcan.catalogo.repository.ProductosRepository;
import cl.elvolcan.common.exception.ApiException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas de negocio de las categorías. */
@Service
public class CategoriasService {

  private final CategoriasRepository categorias;
  private final ProductosRepository productos;

  public CategoriasService(CategoriasRepository categorias, ProductosRepository productos) {
    this.categorias = categorias;
    this.productos = productos;
  }

  /** Entidad → DTO de respuesta. */
  public static CategoriaResponse vista(Categoria c) {
    return new CategoriaResponse(c.id, c.nombre, c.slug, c.descripcion, c.imagen);
  }

  public List<CategoriaResponse> listar() {
    return categorias.findAllByOrderByIdAsc().stream().map(CategoriasService::vista).toList();
  }

  public CategoriaResponse obtener(Long id) {
    return vista(buscar(id));
  }

  public CategoriaResponse obtenerPorSlug(String slug) {
    return vista(
        categorias
            .findBySlug(slug)
            .orElseThrow(() -> new ApiException(404, "Categoría no encontrada.")));
  }

  /** Crea (id == null) o actualiza. El nombre no se puede repetir y el slug se genera solo. */
  @Transactional
  public CategoriaResponse guardar(Long id, CategoriaRequest datos) {
    long idActual = id == null ? -1L : id;
    Categoria c = id == null ? new Categoria() : buscar(id);
    String clave = Textos.normalizar(datos.nombre());
    // Si el nombre no cambió se conserva el slug, para no romper los enlaces de la tienda.
    String slug = clave.equals(c.nombreClave) ? c.slug : Textos.slug(datos.nombre());
    if (slug.isEmpty()) throw new ApiException(400, "El nombre debe incluir letras o números.");
    if (categorias.existsByNombreClaveAndIdNot(clave, idActual)
        || categorias.existsBySlugAndIdNot(slug, idActual))
      throw new ApiException(409, "Ya existe una categoría con ese nombre.");

    c.nombre = datos.nombre().trim();
    c.nombreClave = clave;
    c.slug = slug;
    c.descripcion = Textos.limpiar(datos.descripcion());
    c.imagen = Textos.limpiar(datos.imagen());
    return vista(categorias.saveAndFlush(c));
  }

  /**
   * No se puede eliminar una categoría con productos activos: quedarían productos sin
   * categoría y desaparecerían de la tienda (misma regla que el panel de la Eval 2).
   */
  @Transactional
  public void eliminar(Long id) {
    Categoria c = buscar(id);
    if (productos.countByCategoriaIdAndActivoTrue(id) > 0)
      throw new ApiException(409, "No se puede eliminar una categoría que tiene productos.");
    // Los productos ya eliminados (borrado lógico) se desvinculan para liberar la llave foránea.
    for (Producto p : productos.findByCategoriaId(id)) p.categoria = null;
    categorias.delete(c);
  }

  /** Usado también por ProductosService para validar el categoriaId recibido. */
  public Categoria buscar(Long id) {
    return categorias
        .findById(id)
        .orElseThrow(() -> new ApiException(404, "Categoría no encontrada."));
  }
}
