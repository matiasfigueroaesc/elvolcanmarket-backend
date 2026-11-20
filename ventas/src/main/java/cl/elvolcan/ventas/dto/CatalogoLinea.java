package cl.elvolcan.ventas.dto;

/** Línea confirmada por el catálogo, con el precio real (oferta aplicada). */
public record CatalogoLinea(
    Long id, String codigo, String nombre, int precio, int cantidad, String imagen) {}
