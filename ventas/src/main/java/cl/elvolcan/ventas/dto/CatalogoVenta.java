package cl.elvolcan.ventas.dto;

import java.util.List;

/** Respuesta del catálogo al descontar stock: { lineas, total }. */
public record CatalogoVenta(List<CatalogoLinea> lineas, int total) {}
