package cl.elvolcan.despacho.dto;

import java.time.Instant;
import java.util.List;

/** Despacho completo, para el panel admin y para el cliente dueño de la compra. */
public record DespachoResponse(
    Long boletaId,
    String numero,
    Long usuarioId,
    String region,
    String comuna,
    String entrega,
    String zonaId,
    String estado,
    Instant actualizado,
    List<CambioEstadoResponse> historial) {}
