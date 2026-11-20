package cl.elvolcan.despacho.dto;

import java.time.Instant;

public record CambioEstadoResponse(String estado, Instant fecha) {}
