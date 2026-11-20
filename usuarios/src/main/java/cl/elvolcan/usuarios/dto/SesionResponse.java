package cl.elvolcan.usuarios.dto;

/**
 * Respuesta de login y refresh. El refresh token NO va aquí: viaja en una cookie
 * HttpOnly que JavaScript no puede leer.
 */
public record SesionResponse(String accessToken, long expiresIn, UsuarioResponse usuario) {}
