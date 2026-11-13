package cl.elvolcan.ventas.dto;

/** Línea de la boleta, como "items" en src/data/ordenes.js (más codigo y subtotal). */
public record ItemResponse(
    Long id, String codigo, String nombre, int precio, int cantidad, int subtotal, String imagen) {}
