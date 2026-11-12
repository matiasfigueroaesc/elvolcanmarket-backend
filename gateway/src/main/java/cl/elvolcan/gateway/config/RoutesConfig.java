package cl.elvolcan.gateway.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Tabla de rutas: según el inicio de la URL, el Gateway reenvía la petición al
 * microservicio que corresponde. El Gateway NO valida el token: lo hace cada servicio.
 * Las rutas /internal/** no se publican aquí (solo se usan entre servicios).
 */
@Configuration
public class RoutesConfig {

  @Bean
  RouteLocator rutas(
      RouteLocatorBuilder b,
      @Value("${app.usuarios-url}") String usuarios,
      @Value("${app.catalogo-url}") String catalogo,
      @Value("${app.ventas-url}") String ventas,
      @Value("${app.despacho-url}") String despacho) {
    return b.routes()
        // API REST versionada
        .route("usuarios", r -> r.path("/api/v1/auth/**", "/api/v1/usuarios/**").uri(usuarios))
        .route("catalogo", r -> r.path("/api/v1/productos/**", "/api/v1/categorias/**").uri(catalogo))
        .route("ventas", r -> r.path("/api/v1/boletas/**").uri(ventas))
        .route("despacho", r -> r.path("/api/v1/despachos/**", "/api/v1/zonas/**").uri(despacho))
        // Documentos OpenAPI de cada servicio, para el Swagger agregado del Gateway
        .route("docs-usuarios", r -> r.path("/openapi/usuarios").filters(f -> f.setPath("/v3/api-docs")).uri(usuarios))
        .route("docs-catalogo", r -> r.path("/openapi/catalogo").filters(f -> f.setPath("/v3/api-docs")).uri(catalogo))
        .route("docs-ventas", r -> r.path("/openapi/ventas").filters(f -> f.setPath("/v3/api-docs")).uri(ventas))
        .route("docs-despacho", r -> r.path("/openapi/despacho").filters(f -> f.setPath("/v3/api-docs")).uri(despacho))
        .build();
  }

  /** CORS en un solo lugar: el navegador solo habla con el Gateway. */
  @Bean
  CorsWebFilter cors(@Value("${app.allowed-origins}") String origenes) {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(Arrays.asList(origenes.split(",")));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-Requested-With"));
    // Necesario para la cookie del refresh token
    c.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return new CorsWebFilter(source);
  }
}
