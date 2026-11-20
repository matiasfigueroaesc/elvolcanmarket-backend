package cl.elvolcan.despacho;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio despacho (puerto 8084): estado de los pedidos y zonas de despacho.
 * Los despachos los crea ventas al confirmar el pago (endpoint interno).
 * scanBasePackages = "cl.elvolcan" para que también encuentre lo de common (OpenApiConfig).
 */
@SpringBootApplication(scanBasePackages = "cl.elvolcan")
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
