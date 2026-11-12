package cl.elvolcan.common.exception;

/**
 * Error de negocio con su código HTTP.
 * Ejemplo: throw new ApiException(404, "Producto no encontrado.");
 * ApiErrors lo convierte en una respuesta JSON.
 */
public class ApiException extends RuntimeException {
  public final int status;

  public ApiException(int status, String message) {
    super(message);
    this.status = status;
  }
}
