package cl.elvolcan.usuarios.dto;

import java.time.LocalDate;

/**
 * Usuario tal como lo usa el frontend (mismos nombres que src/data/usuarios.js).
 * Nunca incluye la contraseña ni su hash (equivale a datosPublicos() del frontend).
 */
public record UsuarioResponse(
    Long id,
    String run,
    String nombre,
    String apellidos,
    String correo,
    String telefono,
    LocalDate fechaNacimiento,
    String tipo,
    String region,
    String comuna,
    String direccion) {}
