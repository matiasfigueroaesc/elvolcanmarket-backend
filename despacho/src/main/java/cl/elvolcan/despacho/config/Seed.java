package cl.elvolcan.despacho.config;

import cl.elvolcan.despacho.model.Cobertura;
import cl.elvolcan.despacho.model.Despacho;
import cl.elvolcan.despacho.model.Estados;
import cl.elvolcan.despacho.model.Zona;
import cl.elvolcan.despacho.repository.DespachosRepository;
import cl.elvolcan.despacho.repository.ZonasRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Datos de fábrica (solo si la base está vacía):
 *  - las 5 zonas de src/utils/zonasDespacho.js del frontend
 *  - los despachos de las 2 boletas del Seed de ventas (ORD-000001 entregado, ORD-000002 en camino)
 */
@Configuration
public class Seed {

  @Bean
  CommandLineRunner cargarDespacho(ZonasRepository zonas, DespachosRepository despachos) {
    return args -> {
      if (zonas.count() == 0) {
        zonas.save(zona(1, "centro", "Zona Centro", null, "Lunes a Sábado", "08:00 – 20:00",
            "1 – 3 horas", new Cobertura("chillan", "sectores centro, norte y sur")));
        zonas.save(zona(2, "oriente", "Zona Oriente", null, "Lunes a Viernes", "08:00 – 18:00",
            "2 – 4 horas", new Cobertura("chillan", "sector oriente"),
            new Cobertura("chillan-viejo", null)));
        zonas.save(zona(3, "rural", "Zona Rural", null, "Martes y Jueves", "08:00 – 16:00",
            "3 – 6 horas", new Cobertura("el-carmen", null), new Cobertura("pinto", null),
            new Cobertura("san-ignacio", null)));
        zonas.save(zona(4, "sur", "Zona Sur", null, "Miércoles", "08:00 – 16:00", "4 – 6 horas",
            new Cobertura("bulnes", null), new Cobertura("quillon", null)));
        zonas.save(zona(5, "comercial", "Zona Comercial",
            "Parques industriales y locales comerciales", "Lunes a Viernes", "07:00 – 17:00",
            "Según agenda"));
      }

      if (despachos.count() == 0) {
        despachos.save(despachoCamila(1L, "ORD-000001", "2026-09-02T14:10:00Z", Estados.ORDEN));
        despachos.save(despachoCamila(2L, "ORD-000002", "2026-09-15T19:45:00Z",
            List.of(Estados.RECIBIDO, Estados.PREPARANDO, Estados.EN_CAMINO)));
      }
    };
  }

  private static Zona zona(int posicion, String id, String nombre, String detalle, String dias,
      String horario, String tiempo, Cobertura... cobertura) {
    Zona z = new Zona();
    z.posicion = posicion;
    z.id = id;
    z.nombre = nombre;
    z.detalle = detalle;
    z.dias = dias;
    z.horario = horario;
    z.tiempo = tiempo;
    z.cobertura.addAll(List.of(cobertura));
    return z;
  }

  /** Recorre los estados indicados, con una hora de diferencia entre cada uno. */
  private static Despacho despachoCamila(Long boletaId, String numero, String fecha, List<String> etapas) {
    Despacho d = new Despacho();
    d.boletaId = boletaId;
    d.numero = numero;
    d.usuarioId = 3L;
    d.region = "nuble";
    d.comuna = "chillan-viejo";
    d.entrega = "normal";
    d.zonaId = "oriente";
    Instant t = Instant.parse(fecha);
    for (String etapa : etapas) {
      d.cambiarEstado(etapa, t);
      t = t.plusSeconds(3600);
    }
    return d;
  }
}
