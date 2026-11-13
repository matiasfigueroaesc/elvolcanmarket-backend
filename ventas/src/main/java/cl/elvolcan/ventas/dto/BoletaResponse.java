package cl.elvolcan.ventas.dto;

import java.time.Instant;
import java.util.List;

/**
 * Boleta con la misma forma que una "orden" de src/data/ordenes.js, para que las páginas
 * de React (OrderSummary, OrderTable, Boleta) no cambien.
 * estado = estado del despacho, lo informa el servicio despacho:
 *   "recibido" | "preparando" | "en camino" | "entregado", o null si el pago fue rechazado
 *   (no se despacha) o si despacho no respondió.
 */
public record BoletaResponse(
    Long id,
    String numero,
    Instant fecha,
    Long usuarioId,
    ClienteDto cliente,
    DireccionDto direccion,
    String entrega,
    List<ItemResponse> items,
    int total,
    String estadoPago,
    String estado) {}
