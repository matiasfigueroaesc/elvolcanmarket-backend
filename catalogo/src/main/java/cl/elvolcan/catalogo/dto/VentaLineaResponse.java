package cl.elvolcan.catalogo.dto;

/**
 * Línea confirmada por el catálogo. El precio lo fija el catálogo (con la oferta aplicada),
 * nunca el navegador. Ventas copia estos datos en DETALLE_BOLETA.
 */
public record VentaLineaResponse(
    Long id, String codigo, String nombre, int precio, int cantidad, String imagen) {}
