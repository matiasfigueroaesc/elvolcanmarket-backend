package cl.elvolcan.ventas.config;

import cl.elvolcan.ventas.model.Boleta;
import cl.elvolcan.ventas.model.DetalleBoleta;
import cl.elvolcan.ventas.repository.BoletasRepository;
import java.time.Instant;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Datos de fábrica: las 2 órdenes de ORDENES_SEED (src/data/ordenes.js del frontend),
 * de Camila (usuario 3 del seed de usuarios). Sus despachos los crea el Seed de despacho
 * con los mismos ids (1 entregado, 2 en camino). Solo se cargan si la base está vacía.
 */
@Configuration
public class Seed {

  @Bean
  CommandLineRunner cargarBoletas(BoletasRepository boletas) {
    return args -> {
      if (boletas.count() > 0) return;

      Boleta b1 = boletaCamila("ORD-000001", "2026-09-02T14:10:00Z");
      b1.agregar(new DetalleBoleta(2L, "CL002", "Cilindro GLP 11 kg", 12000, 2, "/img/cilindro-11kg.png"));
      b1.total = 24000;
      boletas.save(b1);

      Boleta b2 = boletaCamila("ORD-000002", "2026-09-15T19:45:00Z");
      b2.agregar(new DetalleBoleta(3L, "CL003", "Cilindro GLP 15 kg", 16000, 1, "/img/cilindro-15kg.png"));
      b2.agregar(new DetalleBoleta(5L, "RG001", "Regulador doméstico estándar", 8990, 1,
          "/img/regulador-estandar.png"));
      b2.total = 24990;
      boletas.save(b2);
    };
  }

  private static Boleta boletaCamila(String numero, String fecha) {
    Boleta b = new Boleta();
    b.numero = numero;
    b.fecha = Instant.parse(fecha);
    b.usuarioId = 3L;
    b.clienteNombre = "Camila";
    b.clienteApellidos = "Toro Pizarro";
    b.clienteCorreo = "camila.toro@gmail.com";
    b.calle = "Camino a Chillán Viejo 789";
    b.region = "nuble";
    b.comuna = "chillan-viejo";
    b.entrega = "normal";
    b.estadoPago = Boleta.PAGADO;
    b.despachoRegistrado = true;
    return b;
  }
}
