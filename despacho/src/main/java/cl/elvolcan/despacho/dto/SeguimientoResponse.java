package cl.elvolcan.despacho.dto;

import java.util.List;

/**
 * Seguimiento público (/seguimiento de la tienda). Cualquiera que tenga el número lo puede
 * consultar, por eso NO trae datos personales: solo la etapa, el historial y la zona.
 * etapas = ESTADOS_ORDEN, para pintar la línea de tiempo.
 */
public record SeguimientoResponse(
    String numero,
    String estado,
    String entrega,
    String zona,
    String tiempo,
    List<String> etapas,
    List<CambioEstadoResponse> historial) {}
