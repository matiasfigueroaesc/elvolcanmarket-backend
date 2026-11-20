package cl.elvolcan.ventas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos del comprador, como "cliente" en src/data/ordenes.js. Sirve de entrada y de salida. */
public record ClienteDto(
    @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 50, message = "El nombre admite hasta 50 caracteres.")
        String nombre,
    @NotBlank(message = "Los apellidos son obligatorios.")
        @Size(max = 80, message = "Los apellidos admiten hasta 80 caracteres.")
        String apellidos,
    @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no es válido.")
        @Size(max = 120, message = "El correo admite hasta 120 caracteres.")
        String correo) {}
