package cl.elvolcan.ventas;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.elvolcan.common.exception.ApiException;
import cl.elvolcan.ventas.client.CatalogoClient;
import cl.elvolcan.ventas.client.DespachoClient;
import cl.elvolcan.ventas.dto.CatalogoItem;
import cl.elvolcan.ventas.dto.CatalogoLinea;
import cl.elvolcan.ventas.dto.CatalogoProducto;
import cl.elvolcan.ventas.dto.CatalogoVenta;
import cl.elvolcan.ventas.dto.DespachoNuevo;
import cl.elvolcan.ventas.service.BoletasService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de integración de ventas: H2 en memoria + MockMvc.
 * El catálogo y despacho son otros servicios, así que aquí se reemplazan por mocks
 * (@MockitoBean): así se prueba qué les pide ventas y qué hace con cada respuesta.
 * jwt() simula la sesión: subject = id del usuario, authorities = rol.
 * Datos de fábrica: 2 boletas pagadas de Camila (usuario 3).
 */
@ActiveProfiles("h2")
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:ventas-it")
@AutoConfigureMockMvc
@Transactional
class BoletasIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired BoletasService service;
  @MockitoBean CatalogoClient catalogo;
  @MockitoBean DespachoClient despacho;

  static final String CHECKOUT =
      """
      {"cliente":{"nombre":"Ana","apellidos":"Rojas Díaz","correo":"Ana.Rojas@gmail.com"},
       "direccion":{"calle":"Av. Libertad 123","departamento":"","region":"nuble",
                    "comuna":"chillan","indicaciones":"Portón verde"},
       "entrega":"express",
       "items":[{"id":4,"cantidad":2,"precio":1,"nombre":"precio falso del navegador"}],
       "simularRechazo":false}
      """;

  static JwtRequestPostProcessor usuario(long id, String rol) {
    return jwt().jwt(j -> j.subject(Long.toString(id)))
        .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
  }

  /** El catálogo responde con el precio de oferta del cilindro de 45 kg. */
  void catalogoVende() {
    when(catalogo.vender(anyLong(), anyList()))
        .thenReturn(
            new CatalogoVenta(
                List.of(new CatalogoLinea(4L, "CL004", "Cilindro GLP 45 kg", 39990, 2,
                    "/img/cilindro-45kg.png")),
                79980));
  }

  // ---------- Checkout ----------

  @Test
  void checkoutComoInvitadoUsaElPrecioDelCatalogoYCreaElDespacho() throws Exception {
    catalogoVende();
    mvc.perform(post("/api/v1/boletas").contentType("application/json").content(CHECKOUT))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.numero").value(matchesPattern("ORD-\\d{6}")))
        .andExpect(jsonPath("$.usuarioId").doesNotExist())
        .andExpect(jsonPath("$.cliente.correo").value("ana.rojas@gmail.com"))
        .andExpect(jsonPath("$.direccion.comuna").value("chillan"))
        .andExpect(jsonPath("$.entrega").value("express"))
        .andExpect(jsonPath("$.items[0].precio").value(39990))
        .andExpect(jsonPath("$.items[0].subtotal").value(79980))
        .andExpect(jsonPath("$.total").value(79980))
        .andExpect(jsonPath("$.estadoPago").value("pagado"))
        .andExpect(jsonPath("$.estado").value("recibido"));

    verify(catalogo).vender(anyLong(), org.mockito.ArgumentMatchers.eq(List.of(new CatalogoItem(4L, 2))));
    verify(despacho).registrar(any(DespachoNuevo.class));
  }

  @Test
  void conSesionLaBoletaQuedaAnombreDelUsuarioDelToken() throws Exception {
    catalogoVende();
    // Aunque el cuerpo traiga otro usuarioId, se usa el del token.
    String cuerpo = CHECKOUT.replace("\"entrega\"", "\"usuarioId\":99,\"entrega\"");
    mvc.perform(
            post("/api/v1/boletas")
                .with(usuario(3, "CLIENTE"))
                .contentType("application/json")
                .content(cuerpo))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.usuarioId").value(3));
    mvc.perform(get("/api/v1/boletas/mias").with(usuario(3, "CLIENTE")))
        .andExpect(jsonPath("$", hasSize(3)));
  }

  @Test
  void pagoRechazadoNoDescuentaStockNiSeDespacha() throws Exception {
    when(catalogo.producto(4L))
        .thenReturn(new CatalogoProducto(4L, "CL004", "Cilindro GLP 45 kg", 45000, 39990, "/img/x.png"));
    mvc.perform(
            post("/api/v1/boletas")
                .contentType("application/json")
                .content(CHECKOUT.replace("\"simularRechazo\":false", "\"simularRechazo\":true")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estadoPago").value("rechazado"))
        .andExpect(jsonPath("$.total").value(79980))
        .andExpect(jsonPath("$.estado").doesNotExist());

    verify(catalogo, never()).vender(anyLong(), anyList());
    verify(despacho, never()).registrar(any());
  }

  @Test
  void sinStockDevuelve409YNoQuedaBoletaAMedias() throws Exception {
    when(catalogo.vender(anyLong(), anyList()))
        .thenThrow(new ApiException(409, "Stock insuficiente de Cilindro GLP 45 kg (disponible: 1)."));
    mvc.perform(post("/api/v1/boletas").contentType("application/json").content(CHECKOUT))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("Stock insuficiente de Cilindro GLP 45 kg (disponible: 1)."));

    mvc.perform(get("/api/v1/boletas").with(usuario(1, "ADMIN")))
        .andExpect(jsonPath("$", hasSize(2)));
    verify(despacho, never()).registrar(any());
  }

  @Test
  void datosInvalidosDevuelven400ConErrorPorCampo() throws Exception {
    mvc.perform(
            post("/api/v1/boletas")
                .contentType("application/json")
                .content(
                    """
                    {"cliente":{"nombre":"","apellidos":"Rojas","correo":"no-es-correo"},
                     "direccion":{"calle":"","region":"nuble","comuna":""},
                     "entrega":"dron","items":[]}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos['cliente.nombre']").exists())
        .andExpect(jsonPath("$.campos['cliente.correo']").exists())
        .andExpect(jsonPath("$.campos['direccion.calle']").exists())
        .andExpect(jsonPath("$.campos['direccion.comuna']").exists())
        .andExpect(jsonPath("$.campos.entrega").exists())
        .andExpect(jsonPath("$.campos.items").exists());
  }

  @Test
  void productosRepetidosOCantidadCeroSeRechazan() throws Exception {
    mvc.perform(
            post("/api/v1/boletas")
                .contentType("application/json")
                .content(CHECKOUT.replace("{\"id\":4,\"cantidad\":2", "{\"id\":4,\"cantidad\":1},{\"id\":4,\"cantidad\":1")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("El carrito tiene productos repetidos."));
    mvc.perform(
            post("/api/v1/boletas")
                .contentType("application/json")
                .content(CHECKOUT.replace("\"cantidad\":2", "\"cantidad\":0")))
        .andExpect(status().isBadRequest());
    verify(catalogo, never()).vender(anyLong(), anyList());
  }

  @Test
  void siDespachoEstaCaidoLaVentaIgualSeHaceYDespuesSeReintenta() throws Exception {
    catalogoVende();
    doThrow(new IllegalStateException("conexión rechazada")).when(despacho).registrar(any());
    mvc.perform(post("/api/v1/boletas").contentType("application/json").content(CHECKOUT))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estadoPago").value("pagado"))
        .andExpect(jsonPath("$.estado").doesNotExist());

    // Despacho vuelve: el reintento registra la boleta pendiente (y solo esa).
    org.mockito.Mockito.reset(despacho);
    assertEquals(1, service.reintentarDespachosPendientes());
    assertEquals(0, service.reintentarDespachosPendientes());
    verify(despacho, times(1)).registrar(any());
  }

  // ---------- Consultas y roles ----------

  @Test
  void listarEsParaAdminYVendedorYTraeElEstadoDeDespacho() throws Exception {
    when(despacho.estados(anyList())).thenReturn(Map.of(1L, "entregado", 2L, "en camino"));

    mvc.perform(get("/api/v1/boletas")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/boletas").with(usuario(3, "CLIENTE"))).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/boletas").with(usuario(2, "VENDEDOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        // Más nuevas primero
        .andExpect(jsonPath("$[0].numero").value("ORD-000002"))
        .andExpect(jsonPath("$[0].estado").value("en camino"))
        .andExpect(jsonPath("$[1].estado").value("entregado"))
        .andExpect(jsonPath("$[0].items", hasSize(2)));
  }

  @Test
  void filtrosDelListado() throws Exception {
    mvc.perform(get("/api/v1/boletas").param("texto", "CAMILA").with(usuario(1, "ADMIN")))
        .andExpect(jsonPath("$", hasSize(2)));
    mvc.perform(get("/api/v1/boletas").param("texto", "ord-000001").with(usuario(1, "ADMIN")))
        .andExpect(jsonPath("$", hasSize(1)));
    mvc.perform(get("/api/v1/boletas").param("usuarioId", "7").with(usuario(1, "ADMIN")))
        .andExpect(jsonPath("$", hasSize(0)));
    mvc.perform(get("/api/v1/boletas").param("estadoPago", "rechazado").with(usuario(1, "ADMIN")))
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void unClienteSoloVeSusBoletas() throws Exception {
    mvc.perform(get("/api/v1/boletas/mias")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/boletas/mias").with(usuario(3, "CLIENTE")))
        .andExpect(jsonPath("$", hasSize(2)));
    mvc.perform(get("/api/v1/boletas/mias").with(usuario(7, "CLIENTE")))
        .andExpect(jsonPath("$", hasSize(0)));

    mvc.perform(get("/api/v1/boletas/1").with(usuario(3, "CLIENTE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].nombre").value("Cilindro GLP 11 kg"));
    // Boleta ajena: 404, no se revela que existe
    mvc.perform(get("/api/v1/boletas/1").with(usuario(7, "CLIENTE")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("Boleta no encontrada."));
    mvc.perform(get("/api/v1/boletas/1").with(usuario(1, "ADMIN"))).andExpect(status().isOk());
    mvc.perform(get("/api/v1/boletas/999").with(usuario(1, "ADMIN"))).andExpect(status().isNotFound());
  }
}
