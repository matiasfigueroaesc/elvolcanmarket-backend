package cl.elvolcan.ventas.dto;

/** Cuerpo del descuento de stock: POST /internal/v1/ventas/{claveVenta} del catálogo. */
public record CatalogoItem(Long id, int cantidad) {}
