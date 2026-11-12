package cl.elvolcan.gateway;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

@SpringBootTest
class GatewayApplicationTest {

  @Autowired RouteLocator rutas;

  @Test
  void registraLasRutasDeLosCuatroServiciosYSusDocumentos() {
    List<String> ids = rutas.getRoutes().map(Route::getId).collectList().block();
    assertTrue(
        ids.containsAll(
            List.of(
                "usuarios",
                "catalogo",
                "ventas",
                "despacho",
                "docs-usuarios",
                "docs-catalogo",
                "docs-ventas",
                "docs-despacho")),
        "Rutas registradas: " + ids);
  }
}
