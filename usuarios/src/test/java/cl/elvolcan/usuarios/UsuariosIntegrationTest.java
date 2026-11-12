package cl.elvolcan.usuarios;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.elvolcan.usuarios.repository.UsuariosRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD de usuarios del panel admin. @WithMockUser o jwt() simulan la sesión y el rol;
 * @Transactional deshace los cambios, así cada prueba parte con los 3 usuarios del Seed
 * (1 = administrador, 2 = vendedor, 3 = cliente).
 */
@ActiveProfiles("h2")
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:usuarios-crud",
      "app.keys-directory=./target/test-keys"
    })
@AutoConfigureMockMvc
@Transactional
class UsuariosIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UsuariosRepository usuarios;

  static final String NUEVO =
      """
      {"run":"11.111.111-1","nombre":"Luis","apellidos":"Rojas Díaz","correo":"luis.rojas@duoc.cl",
       "password":"luis1234","telefono":"","fechaNacimiento":"1990-05-20","tipo":"vendedor",
       "region":"nuble","comuna":"pinto","direccion":"Pasaje 4 #10"}
      """;

  /** Admin con id 1 (Javiera), para las pruebas que necesitan saber quién hace la acción. */
  static JwtRequestPostProcessor admin(long id) {
    return jwt().jwt(j -> j.subject(Long.toString(id))).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  Long idDe(String correo) {
    return usuarios.findByCorreo(correo).orElseThrow().id;
  }

  // ---------- Permisos ----------

  @Test
  void sinSesionDevuelve401() throws Exception {
    mvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "VENDEDOR")
  void vendedorNoPuedeVerUsuarios() throws Exception {
    mvc.perform(get("/api/v1/usuarios")).andExpect(status().isForbidden());
  }

  // ---------- Lectura ----------

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminListaYFiltraPorTipoYTexto() throws Exception {
    mvc.perform(get("/api/v1/usuarios"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].tipo").value("administrador"))
        .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    mvc.perform(get("/api/v1/usuarios").param("tipo", "cliente")).andExpect(jsonPath("$", hasSize(1)));
    mvc.perform(get("/api/v1/usuarios").param("texto", "SALINAS"))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].nombre").value("Pedro"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void usuarioInexistenteDevuelve404EnJson() throws Exception {
    mvc.perform(get("/api/v1/usuarios/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("Usuario no encontrado."));
  }

  // ---------- Crear y editar ----------

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminCreaUnVendedor() throws Exception {
    mvc.perform(post("/api/v1/usuarios").contentType("application/json").content(NUEVO))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.tipo").value("vendedor"))
        .andExpect(jsonPath("$.run").value("111111111"))
        .andExpect(jsonPath("$.telefono").doesNotExist())
        .andExpect(jsonPath("$.fechaNacimiento").value("1990-05-20"));
    mvc.perform(post("/api/v1/usuarios").contentType("application/json").content(NUEVO))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void crearExigeContrasenaYTipoValido() throws Exception {
    mvc.perform(
            post("/api/v1/usuarios")
                .contentType("application/json")
                .content(NUEVO.replace("\"luis1234\"", "\"\"")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/usuarios")
                .contentType("application/json")
                .content(NUEVO.replace("\"vendedor\"", "\"jefe\"")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.tipo").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void editarConContrasenaVaciaMantieneLaActual() throws Exception {
    Long id = idDe("camila.toro@gmail.com");
    String hashAntes = usuarios.findById(id).orElseThrow().passwordHash;
    mvc.perform(
            put("/api/v1/usuarios/" + id)
                .contentType("application/json")
                .content(
                    """
                    {"run":"205671129","nombre":"Camila Andrea","apellidos":"Toro Pizarro",
                     "correo":"camila.toro@gmail.com","password":"","tipo":"cliente",
                     "region":"nuble","comuna":"chillan","direccion":"Nueva 123"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Camila Andrea"))
        .andExpect(jsonPath("$.comuna").value("chillan"));
    assertEquals(hashAntes, usuarios.findById(id).orElseThrow().passwordHash);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void editarNoPermiteUsarElCorreoDeOtro() throws Exception {
    Long id = idDe("camila.toro@gmail.com");
    mvc.perform(
            put("/api/v1/usuarios/" + id)
                .contentType("application/json")
                .content(NUEVO.replace("luis.rojas@duoc.cl", "pedro.salinas@duoc.cl")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("Ya existe un usuario con ese correo."));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void noSePuedeQuitarElRolAlUltimoAdministrador() throws Exception {
    Long id = idDe("javiera.munoz@gmail.com");
    mvc.perform(
            put("/api/v1/usuarios/" + id)
                .contentType("application/json")
                .content(NUEVO.replace("luis.rojas@duoc.cl", "javiera.munoz@gmail.com")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("Debe existir al menos un administrador."));
    assertEquals("administrador", usuarios.findById(id).orElseThrow().tipo);
  }

  // ---------- Eliminar ----------

  @Test
  void eliminarEsLogicoYLiberaElCorreo() throws Exception {
    Long admin = idDe("javiera.munoz@gmail.com");
    Long cliente = idDe("camila.toro@gmail.com");
    mvc.perform(delete("/api/v1/usuarios/" + cliente).with(admin(admin)))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/usuarios/" + cliente).with(admin(admin)))
        .andExpect(status().isNotFound());
    assertFalse(usuarios.findById(cliente).orElseThrow().activo);
    // El mismo correo y RUN se pueden volver a registrar
    mvc.perform(
            post("/api/v1/auth/registro")
                .contentType("application/json")
                .content(
                    """
                    {"run":"20.567.112-9","nombre":"Camila","apellidos":"Toro","correo":"camila.toro@gmail.com",
                     "password":"otra1234","region":"nuble","comuna":"chillan","direccion":"Calle 2"}
                    """))
        .andExpect(status().isCreated());
  }

  @Test
  void adminNoPuedeEliminarSuPropiaCuenta() throws Exception {
    Long admin = idDe("javiera.munoz@gmail.com");
    mvc.perform(delete("/api/v1/usuarios/" + admin).with(admin(admin)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("No puedes eliminar tu propia cuenta."));
  }

  @Test
  void usuarioEliminadoNoPuedeIniciarSesion() throws Exception {
    Long admin = idDe("javiera.munoz@gmail.com");
    mvc.perform(delete("/api/v1/usuarios/" + idDe("pedro.salinas@duoc.cl")).with(admin(admin)))
        .andExpect(status().isNoContent());
    mvc.perform(
            post("/api/v1/auth/login")
                .header("X-Requested-With", "ElVolcanMarket")
                .contentType("application/json")
                .content("{\"correo\":\"pedro.salinas@duoc.cl\",\"password\":\"vendedor1234\"}"))
        .andExpect(status().isUnauthorized());
  }
}
