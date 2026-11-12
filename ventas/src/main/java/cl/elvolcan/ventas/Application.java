package cl.elvolcan.ventas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio ventas (puerto 8083) — ESQUELETO.
 * Mantiene la estructura de capas del catálogo: config, controller, dto, exception,
 * model, repository y service.
 */
@SpringBootApplication(scanBasePackages = "cl.elvolcan")
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
