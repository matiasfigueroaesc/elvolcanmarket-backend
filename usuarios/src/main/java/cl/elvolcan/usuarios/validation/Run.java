package cl.elvolcan.usuarios.validation;

import java.util.Locale;

/** RUN chileno: misma regla que esRunValido de src/utils/validaciones.js del frontend. */
public final class Run {
  private Run() {}

  /** "19.123.456-1" → "191234561". Así el mismo RUN escrito distinto no se repite. */
  public static String normalizar(String run) {
    return run == null ? "" : run.replaceAll("[.\\s-]", "").toUpperCase(Locale.ROOT);
  }

  /** Dígito verificador con módulo 11 (multiplicadores 2, 3, 4, 5, 6, 7 de derecha a izquierda). */
  public static boolean esValido(String run) {
    String valor = normalizar(run);
    if (!valor.matches("\\d{6,8}[0-9K]")) return false;
    int suma = 0;
    int multiplicador = 2;
    for (int i = valor.length() - 2; i >= 0; i--) {
      suma += (valor.charAt(i) - '0') * multiplicador;
      multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
    }
    int resto = 11 - (suma % 11);
    char esperado = resto == 11 ? '0' : resto == 10 ? 'K' : (char) ('0' + resto);
    return valor.charAt(valor.length() - 1) == esperado;
  }
}
