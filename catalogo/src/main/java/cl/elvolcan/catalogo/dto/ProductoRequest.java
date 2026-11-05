package cl.elvolcan.catalogo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos que envía el panel admin para crear o editar un producto.
 * Los nombres coinciden con los del frontend (src/data/productos.js).
 */
public record ProductoRequest(
    @NotBlank(message = "El código es obligatorio.")
        @Size(max = 20, message = "El código admite hasta 20 caracteres.")
        String codigo,
    @NotBlank(message = "El nombre es obligatorio.")
        @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres.")
        String nombre,
    @NotNull(message = "El precio es obligatorio.")
        @Min(value = 1, message = "El precio debe ser mayor a 0.")
        @Max(value = 10_000_000, message = "El precio es demasiado alto.")
        Integer precio,
    @Min(value = 1, message = "El precio de oferta debe ser mayor a 0.") Integer precioOferta,
    @NotNull(message = "El stock es obligatorio.")
        @Min(value = 0, message = "El stock no puede ser negativo.")
        Integer stock,
    @NotNull(message = "El stock crítico es obligatorio.")
        @Min(value = 0, message = "El stock crítico no puede ser negativo.")
        Integer stockCritico,
    @Size(max = 500, message = "La ruta de la imagen es demasiado larga.") String imagen,
    @Size(max = 1000, message = "La descripción admite hasta 1000 caracteres.") String descripcion,
    @Size(max = 30, message = "La unidad admite hasta 30 caracteres.") String unidad,
    @NotNull(message = "La categoría es obligatoria.") Long categoriaId) {}
