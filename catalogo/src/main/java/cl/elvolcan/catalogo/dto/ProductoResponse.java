package cl.elvolcan.catalogo.dto;

/** Producto tal como lo espera el frontend (mismos nombres que en src/data/productos.js). */
public record ProductoResponse(
    Long id,
    String codigo,
    String nombre,
    int precio,
    Integer precioOferta,
    int stock,
    int stockCritico,
    String imagen,
    String descripcion,
    String unidad,
    Long categoriaId) {}
