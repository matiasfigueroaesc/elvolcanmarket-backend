package cl.elvolcan.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio usuarios (puerto 8081): registro, login con JWT RS256, refresh en cookie
 * y CRUD de usuarios para el administrador.
 * scanBasePackages = "cl.elvolcan" para que también encuentre lo de common (Swagger).
 */
@SpringBootApplication(scanBasePackages = "cl.elvolcan")
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
