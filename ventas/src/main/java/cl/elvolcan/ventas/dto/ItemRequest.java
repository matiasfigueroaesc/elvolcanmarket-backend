package cl.elvolcan.ventas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Un producto del carrito. Solo se usan id y cantidad: si el navegador manda también
 * nombre o precio se ignoran, porque el precio lo fija el catálogo.
 */
public record ItemRequest(
    @NotNull(message = "Falta el producto.") Long id,
    @NotNull(message = "Falta la cantidad.")
        @Min(value = 1, message = "La cantidad debe ser al menos 1.")
        @Max(value = 99, message = "La cantidad máxima por producto es 99.")
        Integer cantidad) {}
