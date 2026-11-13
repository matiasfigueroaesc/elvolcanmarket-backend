package cl.elvolcan.despacho.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Crear o editar una zona (ADMIN). El id solo se usa al crear (en PUT manda la URL).
 * Debe tener cobertura (comunas) o detalle (texto libre), como la Zona Comercial.
 */
public record ZonaRequest(
    @Pattern(regexp = "[a-z0-9-]{2,40}", message = "El id usa minúsculas, números y guiones (2 a 40).")
        String id,
    @NotBlank(message = "El nombre es obligatorio.")
        @Size(min = 3, max = 80, message = "El nombre debe tener entre 3 y 80 caracteres.")
        String nombre,
    @NotNull(message = "La cobertura es obligatoria (puede ser una lista vacía).")
        @Size(max = 30, message = "Máximo 30 comunas por zona.")
        List<@NotNull @Valid CoberturaDto> cobertura,
    @Size(max = 200, message = "El detalle admite hasta 200 caracteres.") String detalle,
    @NotBlank(message = "Los días son obligatorios.") @Size(max = 60) String dias,
    @NotBlank(message = "El horario es obligatorio.") @Size(max = 40) String horario,
    @NotBlank(message = "El tiempo estimado es obligatorio.") @Size(max = 40) String tiempo) {}
