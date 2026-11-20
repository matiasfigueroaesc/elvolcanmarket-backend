package cl.elvolcan.usuarios.dto;

import jakarta.validation.constraints.NotBlank;

/** Formulario de Login del frontend: correo y contraseña. */
public record LoginRequest(
    @NotBlank(message = "El correo es obligatorio.") String correo,
    @NotBlank(message = "La contraseña es obligatoria.") String password) {}
