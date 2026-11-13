package cl.elvolcan.despacho;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de integración de despacho: H2 en memoria + MockMvc. @Transactional deshace los
 * cambios de cada prueba. Datos de fábrica: 5 zonas y 2 despachos de Camila (usuario 3):
 * boleta 1 "entregado" y boleta 2 "en camino".
 */
@ActiveProfiles("h2")
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:despacho-it")
@AutoConfigureMockMvc
@Transactional
class DespachoIntegrationTest {

  @Autowired MockMvc mvc;

  static final String CLAVE = "local-cambiar-clave-interna-32-caracteres";

  static final String NUEVO =
      """
      {"boletaId":10,"numero":"ORD-000010","usuarioId":null,"region":"nuble",
       "comuna":"chillan","entrega":"express"}
      """;

  static final String ZONA =
      """
      {"id":"costa","nombre":"Zona Costa","cobertura":[{"comuna":"cobquecura","sector":null}],
       "detalle":null,"dias":"Sábado","horario":"09:00 – 14:00","tiempo":"6 – 8 horas"}
      """;

  static JwtRequestPostProcessor usuario(long id, String rol) {
    return jwt().jwt(j -> j.subject(Long.toString(id)))
        .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
  }

  // ---------- Zonas ----------

  @Test
  void zonasSonPublicasYUsanLaFormaDelFrontend() throws Exception {
    mvc.perform(get("/api/v1/zonas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(5)))
        .andExpect(jsonPath("$[0].id").value("centro"))
        .andExpect(jsonPath("$[1].cobertura[0].sector").value("sector oriente"))
        .andExpect(jsonPath("$[1].cobertura[1].comuna").value("chillan-viejo"))
        .andExpect(jsonPath("$[4].detalle").value("Parques industriales y locales comerciales"));
    mvc.perform(get("/api/v1/zonas/rural"))
        .andExpect(jsonPath("$.dias").value("Martes y Jueves"));
    mvc.perform(get("/api/v1/zonas/marte")).andExpect(status().isNotFound());
  }

  @Test
  void zonasDeUnaComuna() throws Exception {
    // Chillán está en la Zona Centro y en la Zona Oriente
    mvc.perform(get("/api/v1/zonas/comuna/chillan")).andExpect(jsonPath("$", hasSize(2)));
    mvc.perform(get("/api/v1/zonas/comuna/quillon"))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value("sur"));
    mvc.perform(get("/api/v1/zonas/comuna/talca")).andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void soloAdminAdministraZonas() throws Exception {
    mvc.perform(post("/api/v1/zonas").contentType("application/json").content(ZONA))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/v1/zonas").with(usuario(2, "VENDEDOR")).contentType("application/json").content(ZONA))
        .andExpect(status().isForbidden());

    mvc.perform(post("/api/v1/zonas").with(usuario(1, "ADMIN")).contentType("application/json").content(ZONA))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("costa"));
    mvc.perform(post("/api/v1/zonas").with(usuario(1, "ADMIN")).contentType("application/json").content(ZONA))
        .andExpect(status().isConflict());
    // Las nuevas quedan al final
    mvc.perform(get("/api/v1/zonas")).andExpect(jsonPath("$[5].id").value("costa"));

    mvc.perform(
            put("/api/v1/zonas/costa")
                .with(usuario(1, "ADMIN"))
                .contentType("application/json")
                .content(ZONA.replace("Sábado", "Viernes y Sábado")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dias").value("Viernes y Sábado"));

    mvc.perform(delete("/api/v1/zonas/costa").with(usuario(1, "ADMIN"))).andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/zonas/costa")).andExpect(status().isNotFound());
  }

  @Test
  void zonaInvalidaDevuelve400() throws Exception {
    mvc.perform(
            post("/api/v1/zonas")
                .with(usuario(1, "ADMIN"))
                .contentType("application/json")
                .content("{\"id\":\"Con Espacios\",\"nombre\":\"\",\"cobertura\":[],\"dias\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.id").exists())
        .andExpect(jsonPath("$.campos.nombre").exists())
        .andExpect(jsonPath("$.campos.dias").exists());
    // Sin comunas ni detalle
    mvc.perform(
            post("/api/v1/zonas")
                .with(usuario(1, "ADMIN"))
                .contentType("application/json")
                .content(ZONA.replace("[{\"comuna\":\"cobquecura\",\"sector\":null}]", "[]")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("La zona debe tener comunas cubiertas o un detalle."));
  }

  // ---------- Endpoint interno (lo usa ventas) ----------

  @Test
  void ventasCreaElDespachoEnRecibidoYEsIdempotente() throws Exception {
    mvc.perform(post("/internal/v1/despachos").contentType("application/json").content(NUEVO))
        .andExpect(status().isForbidden());

    mvc.perform(
            post("/internal/v1/despachos")
                .header("X-Internal-Key", CLAVE)
                .contentType("application/json")
                .content(NUEVO))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("recibido"))
        .andExpect(jsonPath("$.zonaId").value("centro"))
        .andExpect(jsonPath("$.historial", hasSize(1)));

    // Reintento de ventas: no crea otro ni agrega historial
    mvc.perform(
            post("/internal/v1/despachos")
                .header("X-Internal-Key", CLAVE)
                .contentType("application/json")
                .content(NUEVO))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.historial", hasSize(1)));
    mvc.perform(get("/api/v1/despachos").with(usuario(1, "ADMIN"))).andExpect(jsonPath("$", hasSize(3)));
  }

  @Test
  void comunaSinCoberturaQuedaSinZona() throws Exception {
    mvc.perform(
            post("/internal/v1/despachos")
                .header("X-Internal-Key", CLAVE)
                .contentType("application/json")
                .content(NUEVO.replace("\"chillan\"", "\"talca\"").replace("\"region\":\"nuble\"", "\"region\":\"maule\"")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.zonaId").doesNotExist());
  }

  @Test
  void estadosDeVariasBoletas() throws Exception {
    mvc.perform(
            post("/internal/v1/despachos/estados")
                .header("X-Internal-Key", CLAVE)
                .contentType("application/json")
                .content("[1, 2, 99]"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$['1']").value("entregado"))
        .andExpect(jsonPath("$['2']").value("en camino"))
        .andExpect(jsonPath("$['99']").doesNotExist());
  }

  // ---------- Seguimiento público ----------

  @Test
  void seguimientoPublicoSinDatosPersonales() throws Exception {
    mvc.perform(get("/api/v1/despachos/seguimiento/ord-000002"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.numero").value("ORD-000002"))
        .andExpect(jsonPath("$.estado").value("en camino"))
        .andExpect(jsonPath("$.zona").value("Zona Oriente"))
        .andExpect(jsonPath("$.etapas", hasSize(4)))
        .andExpect(jsonPath("$.historial", hasSize(3)))
        .andExpect(jsonPath("$.usuarioId").doesNotExist())
        .andExpect(jsonPath("$.comuna").doesNotExist());
    mvc.perform(get("/api/v1/despachos/seguimiento/ORD-999999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("Pedido no encontrado. Verifica el número (ej: ORD-000001)."));
  }

  // ---------- Estados ----------

  @Test
  void soloAdminCambiaElEstadoYSoloHaciaAdelante() throws Exception {
    String aEntregado = "{\"estado\":\"entregado\"}";
    mvc.perform(patch("/api/v1/despachos/2/estado").contentType("application/json").content(aEntregado))
        .andExpect(status().isUnauthorized());
    mvc.perform(patch("/api/v1/despachos/2/estado").with(usuario(2, "VENDEDOR"))
            .contentType("application/json").content(aEntregado))
        .andExpect(status().isForbidden());

    mvc.perform(patch("/api/v1/despachos/2/estado").with(usuario(1, "ADMIN"))
            .contentType("application/json").content("{\"estado\":\"recibido\"}"))
        .andExpect(status().isConflict());
    mvc.perform(patch("/api/v1/despachos/2/estado").with(usuario(1, "ADMIN"))
            .contentType("application/json").content(aEntregado))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("entregado"))
        .andExpect(jsonPath("$.historial", hasSize(4)));
    mvc.perform(patch("/api/v1/despachos/2/estado").with(usuario(1, "ADMIN"))
            .contentType("application/json").content(aEntregado))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("El pedido ya fue entregado."));
  }

  @Test
  void estadoInexistenteODespachoInexistente() throws Exception {
    mvc.perform(patch("/api/v1/despachos/2/estado").with(usuario(1, "ADMIN"))
            .contentType("application/json").content("{\"estado\":\"perdido\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.estado").exists());
    mvc.perform(patch("/api/v1/despachos/999/estado").with(usuario(1, "ADMIN"))
            .contentType("application/json").content("{\"estado\":\"preparando\"}"))
        .andExpect(status().isNotFound());
  }

  // ---------- Consultas ----------

  @Test
  void listarEsParaAdminYVendedor() throws Exception {
    mvc.perform(get("/api/v1/despachos").with(usuario(3, "CLIENTE"))).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/despachos").with(usuario(2, "VENDEDOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].boletaId").value(2));
    mvc.perform(get("/api/v1/despachos").param("estado", "entregado").with(usuario(2, "VENDEDOR")))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].numero").value("ORD-000001"));
  }

  @Test
  void unClienteSoloVeSuDespacho() throws Exception {
    mvc.perform(get("/api/v1/despachos/1")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/despachos/1").with(usuario(3, "CLIENTE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("entregado"))
        .andExpect(jsonPath("$.historial", hasSize(4)));
    mvc.perform(get("/api/v1/despachos/1").with(usuario(7, "CLIENTE"))).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/despachos/1").with(usuario(1, "ADMIN"))).andExpect(status().isOk());
  }
}
