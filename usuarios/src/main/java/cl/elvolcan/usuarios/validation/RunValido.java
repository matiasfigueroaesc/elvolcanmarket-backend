package cl.elvolcan.usuarios.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validación propia para usar en los DTO igual que @NotBlank: @RunValido String run.
 * Si falla, el error sale en "campos.run" del JSON, como los demás.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RunValido.Validador.class)
public @interface RunValido {
  String message() default "El RUN ingresado no es válido.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class Validador implements ConstraintValidator<RunValido, String> {
    @Override
    public boolean isValid(String valor, ConstraintValidatorContext ctx) {
      // El vacío lo informa @NotBlank, para no mostrar dos mensajes en el mismo campo.
      return valor == null || valor.isBlank() || Run.esValido(valor);
    }
  }
}
