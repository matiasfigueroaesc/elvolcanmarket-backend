package cl.elvolcan.ventas.dto;

/** Cuerpo de POST /internal/v1/despachos del servicio despacho (al confirmar el pago). */
public record DespachoNuevo(
    Long boletaId, String numero, Long usuarioId, String region, String comuna, String entrega) {}
