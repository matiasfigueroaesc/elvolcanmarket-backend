package cl.elvolcan.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El slug no se envía: lo genera el servidor a partir del nombre. */
public record CategoriaRequest(
    @NotBlank(message = "El nombre es obligatorio.")
        @Size(min = 3, max = 80, message = "El nombre debe tener entre 3 y 80 caracteres.")
        String nombre,
    @Size(max = 500, message = "La descripción admite hasta 500 caracteres.") String descripcion,
    @Size(max = 500, message = "La ruta de la imagen es demasiado larga.") String imagen) {}
