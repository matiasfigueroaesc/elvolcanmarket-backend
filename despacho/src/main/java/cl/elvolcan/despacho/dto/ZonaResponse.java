package cl.elvolcan.despacho.dto;

import java.util.List;

/** Zona tal como la usa el frontend (src/utils/zonasDespacho.js). */
public record ZonaResponse(
    String id,
    String nombre,
    List<CoberturaDto> cobertura,
    String detalle,
    String dias,
    String horario,
    String tiempo) {}
