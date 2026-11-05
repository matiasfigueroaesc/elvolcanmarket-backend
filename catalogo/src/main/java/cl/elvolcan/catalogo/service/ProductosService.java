package cl.elvolcan.catalogo.service;

import cl.elvolcan.catalogo.dto.ProductoRequest;
import cl.elvolcan.catalogo.dto.ProductoResponse;
import cl.elvolcan.catalogo.model.Producto;
import cl.elvolcan.catalogo.repository.ProductosRepository;
import cl.elvolcan.common.exception.ApiException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas de negocio de los productos. */
@Service
public class ProductosService {

  private final ProductosRepository productos;
  private final CategoriasService categorias;

  public ProductosService(ProductosRepository productos, CategoriasService categorias) {
    this.productos = productos;
    this.categorias = categorias;
  }

  /** Entidad → DTO de respuesta. */
  public static ProductoResponse vista(Producto p) {
    return new ProductoResponse(
        p.id,
        p.codigo,
        p.nombre,
        p.precio,
        p.precioOferta,
        p.stock,
        p.stockCritico,
        p.imagen,
        p.descripcion,
        p.unidad,
        p.categoria == null ? null : p.categoria.id);
  }

  /**
   * Catálogo activo con filtros opcionales (los mismos de la tienda y del panel):
   * texto (nombre o código), categoría y "solo ofertas".
   */
  public List<ProductoResponse> listar(String texto, Long categoriaId, Boolean soloOfertas) {
    String buscado = Textos.normalizar(texto);
    Stream<Producto> lista = productos.findByActivoTrueOrderByIdAsc().stream();
    if (!buscado.isEmpty())
      lista =
          lista.filter(
              p ->
                  Textos.normalizar(p.nombre).contains(buscado)
                      || Textos.normalizar(p.codigo).contains(buscado));
    if (categoriaId != null)
      lista = lista.filter(p -> p.categoria != null && categoriaId.equals(p.categoria.id));
    if (Boolean.TRUE.equals(soloOfertas)) lista = lista.filter(p -> p.precioOferta != null);
    return lista.map(ProductosService::vista).toList();
  }

  /** Productos con stock igual o menor a su stock crítico. */
  public List<ProductoResponse> listarCriticos() {
    return productos.findByActivoTrueOrderByIdAsc().stream()
        .filter(p -> p.stock <= p.stockCritico)
        .map(ProductosService::vista)
        .toList();
  }

  public ProductoResponse obtener(Long id) {
    return vista(buscarActivo(id));
  }

  /** Crea (id == null) o actualiza un producto. */
  @Transactional
  public ProductoResponse guardar(Long id, ProductoRequest datos) {
    if (datos.precioOferta() != null && datos.precioOferta() >= datos.precio())
      throw new ApiException(400, "El precio de oferta debe ser menor al precio normal.");

    String codigo = datos.codigo().trim().toUpperCase(Locale.ROOT);
    boolean repetido =
        id == null
            ? productos.existsByCodigoIgnoreCase(codigo)
            : productos.existsByCodigoIgnoreCaseAndIdNot(codigo, id);
    if (repetido) throw new ApiException(409, "Ya existe un producto con el código " + codigo + ".");

    Producto p = id == null ? new Producto() : buscarActivo(id);
    p.codigo = codigo;
    p.nombre = datos.nombre().trim();
    p.precio = datos.precio();
    p.precioOferta = datos.precioOferta();
    p.stock = datos.stock();
    p.stockCritico = datos.stockCritico();
    p.imagen = Textos.limpiar(datos.imagen());
    p.descripcion = Textos.limpiar(datos.descripcion());
    String unidad = Textos.limpiar(datos.unidad());
    p.unidad = unidad == null ? "Unidad" : unidad;
    p.categoria = categorias.buscar(datos.categoriaId());
    return vista(productos.saveAndFlush(p));
  }

  /**
   * Borrado lógico: el producto deja de verse, pero la fila se conserva porque
   * las boletas antiguas lo referencian. El código se libera para poder reutilizarlo.
   */
  @Transactional
  public void eliminar(Long id) {
    Producto p = buscarActivo(id);
    p.activo = false;
    p.codigo = p.codigo + "#ELIMINADO-" + p.id;
    productos.save(p);
  }

  private Producto buscarActivo(Long id) {
    return productos
        .findByIdAndActivoTrue(id)
        .orElseThrow(() -> new ApiException(404, "Producto no encontrado."));
  }
}
