package cl.elvolcan.catalogo.service;

import java.text.Normalizer;
import java.util.Locale;

/** Utilidades de texto usadas por los servicios. */
public final class Textos {
  private Textos() {}

  /** "  Cilindros de GAS " → "cilindros de gas" ; "Ñuble" → "nuble" (sin tildes). */
  public static String normalizar(String texto) {
    if (texto == null) return "";
    return Normalizer.normalize(texto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
  }

  /** "Mangueras y Conexiones" → "mangueras-y-conexiones" (igual que slugify del frontend). */
  public static String slug(String texto) {
    return normalizar(texto).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
  }

  /** Convierte "" o "   " en null, y quita espacios en los extremos. */
  public static String limpiar(String texto) {
    return texto == null || texto.isBlank() ? null : texto.trim();
  }
}
