package cl.elvolcan.usuarios.service;

import java.util.Locale;

/** Limpieza de textos antes de guardarlos. */
final class Textos {
  private Textos() {}

  /** Correo sin espacios y en minúsculas: "Camila.Toro@Gmail.com " = "camila.toro@gmail.com". */
  static String correo(String correo) {
    return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
  }

  /** Quita espacios y convierte "" en null (para campos opcionales). */
  static String limpiar(String texto) {
    if (texto == null) return null;
    String t = texto.trim();
    return t.isEmpty() ? null : t;
  }
}
