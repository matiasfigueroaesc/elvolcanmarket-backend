package cl.elvolcan.ventas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dirección de entrega, como "direccion" en src/data/ordenes.js.
 * region y comuna son los "value" de src/data/regiones.js (ej. "nuble", "chillan-viejo").
 */
public record DireccionDto(
    @NotBlank(message = "La calle es obligatoria.")
        @Size(max = 150, message = "La calle admite hasta 150 caracteres.")
        String calle,
    @Size(max = 50, message = "El departamento admite hasta 50 caracteres.") String departamento,
    @NotBlank(message = "Selecciona una región.")
        @Size(max = 60, message = "Región inválida.")
        String region,
    @NotBlank(message = "Selecciona una comuna.")
        @Size(max = 60, message = "Comuna inválida.")
        String comuna,
    @Size(max = 300, message = "Las indicaciones admiten hasta 300 caracteres.")
        String indicaciones) {}
