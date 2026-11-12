package cl.elvolcan.catalogo;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.elvolcan.catalogo.dto.VentaItemRequest;
import cl.elvolcan.catalogo.dto.VentaResponse;
import cl.elvolcan.catalogo.service.ProductosService;
import cl.elvolcan.catalogo.service.StockService;
import cl.elvolcan.common.exception.ApiException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de integración: levantan el servicio completo con H2 en memoria y llaman a la API
 * con MockMvc. @WithMockUser simula un usuario con sesión y rol (sin necesitar el servicio
 * usuarios). @Transactional deshace los cambios de cada prueba, así todas parten de los
 * datos de fábrica (14 productos, 4 categorías).
 */
@ActiveProfiles("h2")
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:catalogo-test")
@AutoConfigureMockMvc
@Transactional
class CatalogoIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired StockService stock;
  @Autowired ProductosService productos;

  static final String PRODUCTO_VALIDO =
      """
      {"codigo":"ac099","nombre":"Válvula de prueba","precio":5000,"precioOferta":null,
       "stock":10,"stockCritico":2,"imagen":"/img/x.png","descripcion":"Prueba",
       "unidad":"Unidad","categoriaId":4}
      """;

  // ---------- Lectura pública ----------

  @Test
  void catalogoEsPublicoYUsaLosNombresDelFrontend() throws Exception {
    mvc.perform(get("/api/v1/productos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(14)))
        .andExpect(jsonPath("$[1].codigo").value("CL002"))
        .andExpect(jsonPath("$[1].categoriaId").value(1))
        .andExpect(jsonPath("$[1].stockCritico").value(10))
        .andExpect(jsonPath("$[1].precioOferta").doesNotExist());
  }

  @Test
  void filtraPorCategoriaTextoYOfertas() throws Exception {
    mvc.perform(get("/api/v1/productos").param("categoriaId", "2"))
        .andExpect(jsonPath("$", hasSize(3)));
    mvc.perform(get("/api/v1/productos").param("texto", "rg0"))
        .andExpect(jsonPath("$", hasSize(3)));
    mvc.perform(get("/api/v1/productos").param("oferta", "true"))
        .andExpect(jsonPath("$", hasSize(3)));
  }

  @Test
  void productoInexistenteDevuelve404EnJson() throws Exception {
    mvc.perform(get("/api/v1/productos/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("Producto no encontrado."));
  }

  @Test
  void categoriaPorSlugEsPublica() throws Exception {
    mvc.perform(get("/api/v1/categorias/slug/reguladores"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Reguladores"));
  }

  // ---------- Roles ----------

  @Test
  void crearSinSesionDevuelve401() throws Exception {
    mvc.perform(post("/api/v1/productos").contentType("application/json").content(PRODUCTO_VALIDO))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "CLIENTE")
  void clienteNoPuedeCrearNiVerCriticos() throws Exception {
    mvc.perform(post("/api/v1/productos").contentType("application/json").content(PRODUCTO_VALIDO))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/productos/criticos")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "VENDEDOR")
  void vendedorVeCriticosPeroNoPuedeEditar() throws Exception {
    // Solo el detector (stock 4, crítico 5) está en estado crítico
    mvc.perform(get("/api/v1/productos/criticos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].codigo").value("AC003"));
    mvc.perform(delete("/api/v1/productos/1")).andExpect(status().isForbidden());
  }

  // ---------- CRUD de productos (ADMIN) ----------

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminCreaProductoYElCodigoQuedaEnMayusculas() throws Exception {
    mvc.perform(post("/api/v1/productos").contentType("application/json").content(PRODUCTO_VALIDO))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.codigo").value("AC099"))
        .andExpect(jsonPath("$.id").exists());
    // El mismo código otra vez es un conflicto
    mvc.perform(post("/api/v1/productos").contentType("application/json").content(PRODUCTO_VALIDO))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void datosInvalidosDevuelven400ConErrorPorCampo() throws Exception {
    mvc.perform(
            post("/api/v1/productos")
                .contentType("application/json")
                .content("{\"codigo\":\"\",\"nombre\":\"x\",\"precio\":-1,\"stock\":-1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.codigo").exists())
        .andExpect(jsonPath("$.campos.precio").exists())
        .andExpect(jsonPath("$.campos.categoriaId").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void ofertaMayorOIgualAlPrecioSeRechaza() throws Exception {
    mvc.perform(
            post("/api/v1/productos")
                .contentType("application/json")
                .content(PRODUCTO_VALIDO.replace("\"precioOferta\":null", "\"precioOferta\":5000")))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void editarActualizaElStock() throws Exception {
    mvc.perform(
            put("/api/v1/productos/14")
                .contentType("application/json")
                .content(PRODUCTO_VALIDO.replace("ac099", "AC003")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stock").value(10));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void eliminarOcultaElProductoYLiberaSuCodigo() throws Exception {
    mvc.perform(delete("/api/v1/productos/10")).andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/productos/10")).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/productos")).andExpect(jsonPath("$", hasSize(13)));
    mvc.perform(
            post("/api/v1/productos")
                .contentType("application/json")
                .content(PRODUCTO_VALIDO.replace("ac099", "MG003")))
        .andExpect(status().isCreated());
  }

  // ---------- Categorías (ADMIN) ----------

  @Test
  @WithMockUser(roles = "ADMIN")
  void noSeEliminaUnaCategoriaConProductos() throws Exception {
    mvc.perform(delete("/api/v1/categorias/1"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("No se puede eliminar una categoría que tiene productos."));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void creaCategoriaConSlugYNoPermiteNombreRepetido() throws Exception {
    mvc.perform(
            post("/api/v1/categorias")
                .contentType("application/json")
                .content("{\"nombre\":\"Calefacción Exterior\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.slug").value("calefaccion-exterior"));
    mvc.perform(
            post("/api/v1/categorias")
                .contentType("application/json")
                .content("{\"nombre\":\"calefaccion exterior\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void categoriaVaciaSePuedeEliminar() throws Exception {
    String json =
        mvc.perform(
                post("/api/v1/categorias")
                    .contentType("application/json")
                    .content("{\"nombre\":\"Temporal\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = json.replaceAll(".*\"id\":(\\d+).*", "$1");
    mvc.perform(delete("/api/v1/categorias/" + id)).andExpect(status().isNoContent());
  }

  // ---------- Stock (lo usa el servicio ventas) ----------

  @Test
  void ventaUsaPrecioDeOfertaDescuentaStockYEsIdempotente() {
    // Producto 4: Cilindro 45 kg, precio 45000, oferta 39990, stock 30
    List<VentaItemRequest> items = List.of(new VentaItemRequest(4L, 2));
    String boleta = UUID.randomUUID().toString();

    VentaResponse primera = stock.vender(boleta, items);
    VentaResponse repetida = stock.vender(boleta, items);

    assertEquals(39990, primera.lineas().get(0).precio());
    assertEquals(79980, primera.total());
    assertEquals(primera, repetida);
    assertEquals(28, productos.obtener(4L).stock());
  }

  @Test
  void stockInsuficienteNoDescuentaNada() {
    // Producto 14: detector con stock 4
    ApiException e =
        assertThrows(
            ApiException.class,
            () -> stock.vender("B-1", List.of(new VentaItemRequest(14L, 5))));
    assertEquals(409, e.status);
    assertEquals(4, productos.obtener(14L).stock());
  }

  @Test
  void endpointInternoExigeLaClave() throws Exception {
    mvc.perform(
            post("/internal/v1/ventas/B-2")
                .contentType("application/json")
                .content("[{\"id\":1,\"cantidad\":1}]"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/internal/v1/ventas/B-2")
                .header("X-Internal-Key", "local-cambiar-clave-interna-32-caracteres")
                .contentType("application/json")
                .content("[{\"id\":1,\"cantidad\":1}]"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(6500));
  }
}
