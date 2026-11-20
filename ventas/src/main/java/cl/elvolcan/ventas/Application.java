package cl.elvolcan.ventas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio ventas (puerto 8083): boletas y detalle de boleta.
 * Llama al catálogo para descontar stock y a despacho para crear el pedido.
 * scanBasePackages = "cl.elvolcan" para que también encuentre lo de common (OpenApiConfig).
 */
@SpringBootApplication(scanBasePackages = "cl.elvolcan")
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
