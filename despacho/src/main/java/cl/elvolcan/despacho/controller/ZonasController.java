package cl.elvolcan.despacho.controller;

import cl.elvolcan.despacho.dto.ZonaRequest;
import cl.elvolcan.despacho.dto.ZonaResponse;
import cl.elvolcan.despacho.service.ZonasService;
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

@Tag(name = "Zonas de despacho")
@RestController
@RequestMapping("/api/v1/zonas")
public class ZonasController {

  private final ZonasService service;

  public ZonasController(ZonasService service) {
    this.service = service;
  }

  @Operation(summary = "Lista las zonas de despacho (público)")
  @GetMapping
  public List<ZonaResponse> listar() {
    return service.listar();
  }

  @Operation(summary = "Zonas que cubren una comuna, ej. chillan-viejo (público)")
  @GetMapping("/comuna/{comuna}")
  public List<ZonaResponse> deComuna(@PathVariable String comuna) {
    return service.deComuna(comuna);
  }

  @Operation(summary = "Detalle de una zona (público)")
  @GetMapping("/{id}")
  public ZonaResponse obtener(@PathVariable String id) {
    return service.obtener(id);
  }

  @Operation(summary = "Crea una zona (ADMIN)")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ZonaResponse crear(@Valid @RequestBody ZonaRequest datos) {
    return service.crear(datos);
  }

  @Operation(summary = "Actualiza una zona (ADMIN). El id no cambia")
  @PutMapping("/{id}")
  public ZonaResponse actualizar(@PathVariable String id, @Valid @RequestBody ZonaRequest datos) {
    return service.actualizar(id, datos);
  }

  @Operation(summary = "Elimina una zona (ADMIN)")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable String id) {
    service.eliminar(id);
  }
}
