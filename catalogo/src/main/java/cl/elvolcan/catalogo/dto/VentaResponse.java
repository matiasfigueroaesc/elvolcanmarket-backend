package cl.elvolcan.catalogo.dto;

import java.util.List;

/** Respuesta al descuento de stock: las líneas con su precio y el total a cobrar. */
public record VentaResponse(List<VentaLineaResponse> lineas, int total) {}
