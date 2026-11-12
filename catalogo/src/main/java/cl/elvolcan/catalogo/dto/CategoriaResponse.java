package cl.elvolcan.catalogo.dto;

/** Categoría tal como la espera el frontend (src/data/categorias.js). */
public record CategoriaResponse(
    Long id, String nombre, String slug, String descripcion, String imagen) {}
