package cl.elvolcan.catalogo.controller;

import cl.elvolcan.catalogo.dto.ProductoRequest;
import cl.elvolcan.catalogo.dto.ProductoResponse;
import cl.elvolcan.catalogo.service.ProductosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de productos. El controlador solo recibe HTTP y delega en el servicio.
 * Quién puede llamar a cada uno se define en SecurityConfig.
 */
@Tag(name = "Productos")
@RestController
@RequestMapping("/api/v1/productos")
public class ProductosController {

  private final ProductosService service;

  public ProductosController(ProductosService service) {
    this.service = service;
  }

  @Operation(summary = "Lista el catálogo (público). Filtros opcionales: texto, categoriaId, oferta")
  @GetMapping
  public List<ProductoResponse> listar(
      @RequestParam(required = false) String texto,
      @RequestParam(required = false) Long categoriaId,
      @RequestParam(required = false) Boolean oferta) {
    return service.listar(texto, categoriaId, oferta);
  }

  @Operation(summary = "Productos con stock crítico (ADMIN, VENDEDOR)")
  @GetMapping("/criticos")
  public List<ProductoResponse> criticos() {
    return service.listarCriticos();
  }

  @Operation(summary = "Detalle de un producto (público)")
  @GetMapping("/{id}")
  public ProductoResponse obtener(@PathVariable Long id) {
    return service.obtener(id);
  }

  @Operation(summary = "Crea un producto (ADMIN)")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProductoResponse crear(@Valid @RequestBody ProductoRequest datos) {
    return service.guardar(null, datos);
  }

  @Operation(summary = "Actualiza un producto (ADMIN)")
  @PutMapping("/{id}")
  public ProductoResponse actualizar(
      @PathVariable Long id, @Valid @RequestBody ProductoRequest datos) {
    return service.guardar(id, datos);
  }

  @Operation(summary = "Elimina un producto (ADMIN, borrado lógico)")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id) {
    service.eliminar(id);
  }
}
