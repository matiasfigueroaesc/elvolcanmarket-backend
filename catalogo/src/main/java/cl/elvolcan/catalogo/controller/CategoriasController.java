package cl.elvolcan.catalogo.controller;

import cl.elvolcan.catalogo.dto.CategoriaRequest;
import cl.elvolcan.catalogo.dto.CategoriaResponse;
import cl.elvolcan.catalogo.service.CategoriasService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Categorías")
@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriasController {

  private final CategoriasService service;

  public CategoriasController(CategoriasService service) {
    this.service = service;
  }

  @Operation(summary = "Lista las categorías (público)")
  @GetMapping
  public List<CategoriaResponse> listar() {
    return service.listar();
  }

  @Operation(summary = "Detalle de una categoría por id (público)")
  @GetMapping("/{id}")
  public CategoriaResponse obtener(@PathVariable Long id) {
    return service.obtener(id);
  }

  @Operation(summary = "Busca una categoría por su slug, para /categorias/:slug de la tienda (público)")
  @GetMapping("/slug/{slug}")
  public CategoriaResponse obtenerPorSlug(@PathVariable String slug) {
    return service.obtenerPorSlug(slug);
  }

  @Operation(summary = "Crea una categoría (ADMIN)")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CategoriaResponse crear(@Valid @RequestBody CategoriaRequest datos) {
    return service.guardar(null, datos);
  }

  @Operation(summary = "Actualiza una categoría (ADMIN)")
  @PutMapping("/{id}")
  public CategoriaResponse actualizar(
      @PathVariable Long id, @Valid @RequestBody CategoriaRequest datos) {
    return service.guardar(id, datos);
  }

  @Operation(summary = "Elimina una categoría sin productos (ADMIN)")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id) {
    service.eliminar(id);
  }
}
