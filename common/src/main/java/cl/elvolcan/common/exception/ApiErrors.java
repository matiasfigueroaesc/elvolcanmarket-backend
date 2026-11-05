package cl.elvolcan.common.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce las excepciones a un JSON uniforme:
 * { "timestamp": ..., "status": 400, "mensaje": "...", "campos": { "precio": "..." } }
 *
 * No lleva @ControllerAdvice: cada microservicio crea su GlobalExceptionHandler
 * que extiende esta clase y le pone @ControllerAdvice.
 */
public class ApiErrors {

  public record ErrorBody(
      Instant timestamp, int status, String mensaje, Map<String, String> campos) {}

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorBody> negocio(ApiException e) {
    return ResponseEntity.status(e.status)
        .body(new ErrorBody(Instant.now(), e.status, e.getMessage(), Map.of()));
  }

  /** Falla de @Valid: devuelve un mensaje por campo para mostrarlo en el formulario. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorBody> validacion(MethodArgumentNotValidException e) {
    Map<String, String> campos = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.badRequest()
        .body(new ErrorBody(Instant.now(), 400, "Revisa los campos del formulario.", campos));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorBody> duplicado(DataIntegrityViolationException e) {
    return ResponseEntity.status(409)
        .body(
            new ErrorBody(
                Instant.now(), 409, "El registro ya existe o está siendo utilizado.", Map.of()));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingRequestHeaderException.class
  })
  public ResponseEntity<ErrorBody> formato(Exception e) {
    return ResponseEntity.badRequest()
        .body(new ErrorBody(Instant.now(), 400, "Formato de solicitud inválido.", Map.of()));
  }
}
