package cl.elvolcan.usuarios.service;

import cl.elvolcan.common.exception.ApiException;
import cl.elvolcan.usuarios.dto.UsuarioRequest;
import cl.elvolcan.usuarios.dto.UsuarioResponse;
import cl.elvolcan.usuarios.model.TipoUsuario;
import cl.elvolcan.usuarios.model.Usuario;
import cl.elvolcan.usuarios.repository.SesionesRepository;
import cl.elvolcan.usuarios.repository.UsuariosRepository;
import cl.elvolcan.usuarios.validation.Run;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD de usuarios del panel admin. */
@Service
public class UsuariosService {

  private final UsuariosRepository usuarios;
  private final SesionesRepository sesiones;
  private final AuthService auth;
  private final PasswordEncoder passwords;

  public UsuariosService(
      UsuariosRepository usuarios,
      SesionesRepository sesiones,
      AuthService auth,
      PasswordEncoder passwords) {
    this.usuarios = usuarios;
    this.sesiones = sesiones;
    this.auth = auth;
    this.passwords = passwords;
  }

  /** Mismos filtros que la página Usuarios del admin: tipo y texto en run, nombre, apellidos o correo. */
  public List<UsuarioResponse> listar(String texto, String tipo) {
    String buscado = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    Stream<Usuario> lista = usuarios.findByActivoTrueOrderByIdAsc().stream();
    if (tipo != null && !tipo.isBlank()) lista = lista.filter(u -> u.tipo.equals(tipo));
    if (!buscado.isEmpty())
      lista =
          lista.filter(
              u ->
                  Stream.of(u.run, u.nombre, u.apellidos, u.correo)
                      .anyMatch(c -> c.toLowerCase(Locale.ROOT).contains(buscado)));
    return lista.map(AuthService::vista).toList();
  }

  public UsuarioResponse obtener(Long id) {
    return AuthService.vista(buscar(id));
  }

  @Transactional
  public UsuarioResponse crear(UsuarioRequest datos) {
    if (datos.password() == null || datos.password().length() < 4)
      throw new ApiException(400, "La contraseña debe tener entre 4 y 20 caracteres.");
    Usuario u = new Usuario();
    u.run = Run.normalizar(datos.run());
    u.correo = Textos.correo(datos.correo());
    auth.validarUnicos(u.run, u.correo);
    u.passwordHash = passwords.encode(datos.password());
    copiar(datos, u);
    return AuthService.vista(usuarios.saveAndFlush(u));
  }

  /**
   * Editar no cambia el RUN (identifica al usuario; en el frontend el campo es readOnly).
   * Contraseña vacía = se mantiene la actual. Si cambia el tipo (rol) o la contraseña se
   * cierran sus sesiones, para que el cambio se aplique en el próximo inicio de sesión.
   * Editar solo datos personales no cierra la sesión.
   */
  @Transactional
  public UsuarioResponse editar(Long id, UsuarioRequest datos) {
    Usuario u = buscar(id);
    String correo = Textos.correo(datos.correo());
    if (usuarios.existsByCorreoAndIdNot(correo, id))
      throw new ApiException(409, "Ya existe un usuario con ese correo.");
    if (TipoUsuario.ADMINISTRADOR.equals(u.tipo)
        && !TipoUsuario.ADMINISTRADOR.equals(datos.tipo())
        && usuarios.countByTipoAndActivoTrue(TipoUsuario.ADMINISTRADOR) <= 1)
      throw new ApiException(409, "Debe existir al menos un administrador.");

    boolean cerrarSesiones = !u.tipo.equals(datos.tipo());
    String password = datos.password();
    if (password != null && !password.isEmpty()) {
      if (password.length() < 4)
        throw new ApiException(400, "La contraseña debe tener entre 4 y 20 caracteres.");
      u.passwordHash = passwords.encode(password);
      cerrarSesiones = true;
    }
    u.correo = correo;
    copiar(datos, u);
    if (cerrarSesiones) sesiones.deleteByUsuarioId(id);
    return AuthService.vista(usuarios.saveAndFlush(u));
  }

  /**
   * Borrado lógico: la fila se conserva porque las boletas de ventas guardan el id del
   * cliente. El correo y el RUN se liberan para que puedan volver a registrarse.
   */
  @Transactional
  public void eliminar(Long id, Long actorId) {
    Usuario u = buscar(id);
    if (id.equals(actorId)) throw new ApiException(409, "No puedes eliminar tu propia cuenta.");
    if (TipoUsuario.ADMINISTRADOR.equals(u.tipo)
        && usuarios.countByTipoAndActivoTrue(TipoUsuario.ADMINISTRADOR) <= 1)
      throw new ApiException(409, "No puedes eliminar el último administrador.");
    u.activo = false;
    u.correo = "eliminado-" + u.id + "@" + u.correo;
    u.run = "X" + u.id;
    sesiones.deleteByUsuarioId(id);
    usuarios.save(u);
  }

  private void copiar(UsuarioRequest d, Usuario u) {
    u.nombre = d.nombre().trim();
    u.apellidos = d.apellidos().trim();
    u.telefono = Textos.limpiar(d.telefono());
    u.fechaNacimiento = d.fechaNacimiento();
    u.tipo = d.tipo();
    u.region = d.region().trim();
    u.comuna = d.comuna().trim();
    u.direccion = d.direccion().trim();
  }

  private Usuario buscar(Long id) {
    return usuarios
        .findByIdAndActivoTrue(id)
        .orElseThrow(() -> new ApiException(404, "Usuario no encontrado."));
  }
}
