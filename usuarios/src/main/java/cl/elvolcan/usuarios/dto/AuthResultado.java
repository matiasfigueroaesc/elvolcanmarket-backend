package cl.elvolcan.usuarios.dto;

/** Uso interno (servicio → controlador): la sesión para el JSON y el refresh para la cookie. */
public record AuthResultado(SesionResponse sesion, String refresh) {}
