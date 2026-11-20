package cl.elvolcan.despacho.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Lo envía ventas (endpoint interno) cuando se confirma el pago de una boleta. */
public record DespachoNuevoRequest(
    @NotNull Long boletaId,
    @NotBlank @Size(max = 20) String numero,
    Long usuarioId,
    @NotBlank @Size(max = 60) String region,
    @NotBlank @Size(max = 60) String comuna,
    @Size(max = 20) String entrega) {}
