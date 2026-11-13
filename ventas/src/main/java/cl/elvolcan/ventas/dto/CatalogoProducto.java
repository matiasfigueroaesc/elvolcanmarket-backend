package cl.elvolcan.ventas.dto;

/**
 * Parte de GET /api/v1/productos/{id} del catálogo que usa ventas.
 * Solo se consulta cuando el pago es rechazado (para mostrar la boleta sin tocar el stock).
 */
public record CatalogoProducto(
    Long id, String codigo, String nombre, int precio, Integer precioOferta, String imagen) {

  public int precioFinal() {
    return precioOferta != null ? precioOferta : precio;
  }
}
