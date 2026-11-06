package cl.elvolcan.usuarios.model;

import java.util.Map;

/**
 * Tipos de usuario del frontend y el rol que llevan en el JWT.
 * El frontend usa "administrador"; los demás servicios esperan el rol "ADMIN".
 */
public final class TipoUsuario {
  public static final String ADMINISTRADOR = "administrador";
  public static final String VENDEDOR = "vendedor";
  public static final String CLIENTE = "cliente";

  /** Para validar con @Pattern en los DTO. */
  public static final String PATRON = "administrador|vendedor|cliente";

  private static final Map<String, String> ROLES =
      Map.of(ADMINISTRADOR, "ADMIN", VENDEDOR, "VENDEDOR", CLIENTE, "CLIENTE");

  private TipoUsuario() {}

  /** "administrador" → "ADMIN", "vendedor" → "VENDEDOR", "cliente" → "CLIENTE". */
  public static String rol(String tipo) {
    String rol = ROLES.get(tipo);
    if (rol == null) throw new IllegalArgumentException("Tipo de usuario desconocido: " + tipo);
    return rol;
  }
}
