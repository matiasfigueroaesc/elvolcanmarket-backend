package cl.elvolcan.usuarios.service;

import cl.elvolcan.common.exception.ApiException;
import cl.elvolcan.common.security.SecuritySupport;
import cl.elvolcan.usuarios.dto.AuthResultado;
import cl.elvolcan.usuarios.dto.RegistroRequest;
import cl.elvolcan.usuarios.dto.SesionResponse;
import cl.elvolcan.usuarios.dto.UsuarioResponse;
import cl.elvolcan.usuarios.model.SesionRefresh;
import cl.elvolcan.usuarios.model.TipoUsuario;
import cl.elvolcan.usuarios.model.Usuario;
import cl.elvolcan.usuarios.repository.SesionesRepository;
import cl.elvolcan.usuarios.repository.UsuariosRepository;
import cl.elvolcan.usuarios.security.JwtKeys;
import cl.elvolcan.usuarios.validation.Run;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login, registro, refresh y logout.
 * Access token (JWT RS256, 15 min) + refresh token (aleatorio, 7 días, en cookie HttpOnly).
 */
@Service
public class AuthService {

  /** Duración del refresh token. */
  public static final Duration DURACION_REFRESH = Duration.ofDays(7);

  private static final SecureRandom ALEATORIO = new SecureRandom();

  private final UsuariosRepository usuarios;
  private final SesionesRepository sesiones;
  private final PasswordEncoder passwords;
  private final JwtEncoder encoder;
  private final String issuer;
  private final long segundosAccess;

  public AuthService(
      UsuariosRepository usuarios,
      SesionesRepository sesiones,
      PasswordEncoder passwords,
      JwtKeys keys,
      @Value("${app.issuer}") String issuer,
      @Value("${app.access-seconds}") long segundosAccess) {
    this.usuarios = usuarios;
    this.sesiones = sesiones;
    this.passwords = passwords;
    this.encoder = keys.encoder();
    this.issuer = issuer;
    this.segundosAccess = segundosAccess;
  }

  /** Entidad → DTO (sin contraseña). */
  public static UsuarioResponse vista(Usuario u) {
    return new UsuarioResponse(
        u.id, u.run, u.nombre, u.apellidos, u.correo, u.telefono, u.fechaNacimiento, u.tipo,
        u.region, u.comuna, u.direccion);
  }

  /**
   * Mismo mensaje si el correo no existe o si la contraseña es incorrecta: así no se puede
   * averiguar qué correos están registrados.
   */
  @Transactional
  public AuthResultado login(String correo, String password) {
    Usuario u =
        usuarios
            .findByCorreo(Textos.correo(correo))
            .filter(x -> x.activo && passwords.matches(password, x.passwordHash))
            .orElseThrow(() -> new ApiException(401, "Correo o contraseña incorrectos."));
    return emitir(u);
  }

  /**
   * Rotación: el refresh usado se borra y se entrega uno nuevo. Si alguien roba un refresh
   * y el dueño lo usa primero, el robado ya no sirve.
   */
  @Transactional
  public AuthResultado refresh(String token) {
    SesionRefresh s =
        sesiones
            .bloquear(hash(token))
            .filter(x -> x.expira.isAfter(Instant.now()))
            .orElseThrow(() -> new ApiException(401, "La sesión expiró. Inicia sesión nuevamente."));
    sesiones.delete(s);
    sesiones.flush();
    return emitir(activo(s.usuarioId));
  }

  @Transactional
  public void logout(String token) {
    if (token != null && !token.isBlank()) sesiones.deleteById(hash(token));
  }

  /** Registro público: siempre crea un cliente. */
  @Transactional
  public UsuarioResponse registrar(RegistroRequest r) {
    Usuario u = new Usuario();
    u.run = Run.normalizar(r.run());
    u.correo = Textos.correo(r.correo());
    validarUnicos(u.run, u.correo);
    u.nombre = r.nombre().trim();
    u.apellidos = r.apellidos().trim();
    u.passwordHash = passwords.encode(r.password());
    u.telefono = Textos.limpiar(r.telefono());
    u.fechaNacimiento = r.fechaNacimiento();
    u.tipo = TipoUsuario.CLIENTE;
    u.region = r.region().trim();
    u.comuna = r.comuna().trim();
    u.direccion = r.direccion().trim();
    return vista(usuarios.saveAndFlush(u));
  }

  /** Lanza 409 si el RUN o el correo ya existen (mismos mensajes que crearUsuario del frontend). */
  void validarUnicos(String run, String correo) {
    if (usuarios.existsByCorreo(correo))
      throw new ApiException(409, "Ya existe un usuario con ese correo.");
    if (usuarios.existsByRun(run)) throw new ApiException(409, "Ya existe un usuario con ese RUN.");
  }

  /** Usuario de la sesión actual (para /auth/me). 401 si fue eliminado mientras tenía token. */
  public Usuario activo(Long id) {
    return usuarios
        .findByIdAndActivoTrue(id)
        .orElseThrow(() -> new ApiException(401, "La cuenta no está activa."));
  }

  private AuthResultado emitir(Usuario u) {
    Instant ahora = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(u.id.toString())
            .audience(List.of(SecuritySupport.AUDIENCIA))
            .issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(segundosAccess))
            .claim("roles", List.of(TipoUsuario.rol(u.tipo)))
            .claim("correo", u.correo)
            .claim("nombre", u.nombre)
            .build();
    String access =
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
            .getTokenValue();

    byte[] bytes = new byte[48];
    ALEATORIO.nextBytes(bytes);
    String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    sesiones.save(new SesionRefresh(hash(refresh), u.id, ahora.plus(DURACION_REFRESH)));

    return new AuthResultado(new SesionResponse(access, segundosAccess, vista(u)), refresh);
  }

  /** SHA-256 del refresh token: en la base se guarda el hash, nunca el token. */
  static String hash(String token) {
    if (token == null || token.isBlank())
      throw new ApiException(401, "No existe una sesión activa.");
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
