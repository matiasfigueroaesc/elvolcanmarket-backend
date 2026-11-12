package cl.elvolcan.catalogo.dto;

/** Un producto y su cantidad, enviado por el servicio ventas. */
public record VentaItemRequest(Long id, int cantidad) {}
