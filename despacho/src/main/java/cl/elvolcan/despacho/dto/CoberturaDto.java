package cl.elvolcan.despacho.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** { comuna, sector? } como en ZONAS_DESPACHO del frontend. comuna = value de regiones.js. */
public record CoberturaDto(
    @NotBlank(message = "La comuna es obligatoria.") @Size(max = 60) String comuna,
    @Size(max = 100, message = "El sector admite hasta 100 caracteres.") String sector) {}
