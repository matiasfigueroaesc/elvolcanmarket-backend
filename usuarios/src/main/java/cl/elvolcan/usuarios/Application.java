package cl.elvolcan.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio usuarios (puerto 8081) — ESQUELETO.
 * Mantiene la estructura de capas del catálogo: config, controller, dto, exception,
 * model, repository, service (y security para las claves del JWT).
 */
@SpringBootApplication(scanBasePackages = "cl.elvolcan")
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
