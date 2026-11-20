package cl.elvolcan.ventas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Lo que envía el checkout (CheckoutForm.jsx + carrito). El usuario NO viene en el cuerpo:
 * se toma del token si hay sesión; sin sesión la compra queda como invitado.
 * simularRechazo es el check "Simular pago rechazado" del formulario.
 */
public record BoletaRequest(
    @NotNull(message = "Faltan los datos del cliente.") @Valid ClienteDto cliente,
    @NotNull(message = "Falta la dirección.") @Valid DireccionDto direccion,
    @NotBlank(message = "Selecciona una opción de entrega.")
        @Pattern(regexp = "normal|express", message = "La opción de entrega no es válida.")
        String entrega,
    @NotEmpty(message = "El carrito está vacío.")
        @Size(max = 50, message = "El carrito admite hasta 50 productos distintos.")
        List<@NotNull(message = "Producto inválido.") @Valid ItemRequest> items,
    boolean simularRechazo) {}
