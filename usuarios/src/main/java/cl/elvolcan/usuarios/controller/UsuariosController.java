package cl.elvolcan.usuarios.controller;

import cl.elvolcan.usuarios.dto.UsuarioRequest;
import cl.elvolcan.usuarios.dto.UsuarioResponse;
import cl.elvolcan.usuarios.service.UsuariosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

/** CRUD de usuarios del panel admin. Todo el controlador es solo ADMIN (ver SecurityConfig). */
@Tag(name = "Usuarios")
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuariosController {

  private final UsuariosService service;

  public UsuariosController(UsuariosService service) {
    this.service = service;
  }

  @Operation(summary = "Lista usuarios activos (ADMIN). Filtros opcionales: texto, tipo")
  @GetMapping
  public List<UsuarioResponse> listar(
      @RequestParam(required = false) String texto, @RequestParam(required = false) String tipo) {
    return service.listar(texto, tipo);
  }

  @Operation(summary = "Detalle de un usuario (ADMIN)")
  @GetMapping("/{id}")
  public UsuarioResponse obtener(@PathVariable Long id) {
    return service.obtener(id);
  }

  @Operation(summary = "Crea un usuario de cualquier tipo (ADMIN, 201)")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest datos) {
    return service.crear(datos);
  }

  @Operation(summary = "Edita un usuario (ADMIN). Contraseña vacía = no cambiarla")
  @PutMapping("/{id}")
  public UsuarioResponse editar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest datos) {
    return service.editar(id, datos);
  }

  @Operation(summary = "Elimina un usuario (ADMIN, borrado lógico, 204)")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    service.eliminar(id, Long.valueOf(jwt.getSubject()));
  }
}
