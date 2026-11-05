package cl.elvolcan.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger de cada microservicio. El servidor apunta al Gateway para que
 * "Try it out" pase por la misma entrada que usa el frontend.
 * El botón "Authorize" acepta el accessToken (Bearer).
 */
@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI api(
      @Value("${spring.application.name}") String nombre,
      @Value("${app.gateway-url:http://localhost:8080}") String gateway) {
    return new OpenAPI()
        .info(new Info().title("El Volcán Market - " + nombre).version("v1"))
        .addServersItem(new Server().url(gateway))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }
}
