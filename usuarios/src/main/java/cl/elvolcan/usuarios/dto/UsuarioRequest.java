package cl.elvolcan.usuarios.dto;

import cl.elvolcan.usuarios.model.TipoUsuario;
import cl.elvolcan.usuarios.validation.RunValido;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Formulario del admin (UserForm) para crear o editar. Igual que el registro, pero con
 * "tipo". Al editar, la contraseña puede venir vacía: significa "no cambiarla"
 * (lo revisa UsuariosService, porque al crear sí es obligatoria).
 */
public record UsuarioRequest(
    @NotBlank(message = "El RUN es obligatorio.") @RunValido String run,
    @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 50, message = "El nombre admite hasta 50 caracteres.")
        String nombre,
    @NotBlank(message = "Los apellidos son obligatorios.")
        @Size(max = 100, message = "Los apellidos admiten hasta 100 caracteres.")
        String apellidos,
    @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "Ingresa un correo válido.")
        @Size(max = 120, message = "El correo admite hasta 120 caracteres.")
        @Pattern(regexp = Reglas.CORREO, flags = Pattern.Flag.CASE_INSENSITIVE, message = Reglas.MENSAJE_CORREO)
        String correo,
    @Size(max = 20, message = "La contraseña debe tener entre 4 y 20 caracteres.") String password,
    @Pattern(regexp = Reglas.TELEFONO, message = "Ingresa un teléfono válido (8 a 9 dígitos, solo números).")
        String telefono,
    @Past(message = "La fecha de nacimiento no puede ser futura.") LocalDate fechaNacimiento,
    @NotBlank(message = "Debes seleccionar un tipo de usuario.")
        @Pattern(regexp = TipoUsuario.PATRON, message = "El tipo debe ser administrador, vendedor o cliente.")
        String tipo,
    @NotBlank(message = "Debes seleccionar una región.") @Size(max = 50) String region,
    @NotBlank(message = "Debes seleccionar una comuna.") @Size(max = 50) String comuna,
    @NotBlank(message = "La dirección es obligatoria.")
        @Size(max = 300, message = "La dirección admite hasta 300 caracteres.")
        String direccion) {}
