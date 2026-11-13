package cl.elvolcan.despacho.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Cambio de estado desde el panel admin (select de Boleta.jsx). */
public record EstadoRequest(
    @NotBlank(message = "El estado es obligatorio.")
        @Pattern(
            regexp = "recibido|preparando|en camino|entregado",
            message = "Estado inválido. Usa: recibido, preparando, en camino o entregado.")
        String estado) {}
