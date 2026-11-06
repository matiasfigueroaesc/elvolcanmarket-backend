package cl.elvolcan.usuarios.dto;

/** Expresiones regulares compartidas por los DTO (mismas reglas que validaciones.js del frontend). */
final class Reglas {
  private Reglas() {}

  /** Dominios permitidos desde la Eval 1: @duoc.cl, @profesor.duoc.cl y @gmail.com. */
  static final String CORREO = "^[^\\s@]+@(duoc\\.cl|profesor\\.duoc\\.cl|gmail\\.com)$";

  static final String MENSAJE_CORREO =
      "El correo debe tener un dominio válido (@duoc.cl, @profesor.duoc.cl o @gmail.com).";

  /** Teléfono opcional: vacío o 8 a 9 dígitos. */
  static final String TELEFONO = "^$|^\\d{8,9}$";
}
