package cl.elvolcan.usuarios;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.elvolcan.usuarios.repository.UsuariosRepository;
import cl.elvolcan.usuarios.validation.Run;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas del login y del JWT. Levantan el servicio completo con H2 en memoria.
 * Usan los usuarios de prueba del Seed (los mismos del frontend).
 */
@ActiveProfiles("h2")
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:usuarios-auth",
      "app.keys-directory=./target/test-keys"
    })
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UsuariosRepository usuarios;

  static final String ENCABEZADO = "X-Requested-With";
  static final String VALOR = "ElVolcanMarket";

  MvcResult login(String correo, String password) throws Exception {
    return mvc.perform(
            post("/api/v1/auth/login")
                .header(ENCABEZADO, VALOR)
                .contentType("application/json")
                .content("{\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}"))
        .andReturn();
  }

  // ---------- Login y JWT ----------

  @Test
  void loginEntregaJwtYCookieHttpOnly() throws Exception {
    mvc.perform(
            post("/api/v1/auth/login")
                .header(ENCABEZADO, VALOR)
                .contentType("application/json")
                .content("{\"correo\":\"Javiera.Munoz@gmail.com \",\"password\":\"admin1234\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.expiresIn").value(900))
        .andExpect(jsonPath("$.usuario.tipo").value("administrador"))
        .andExpect(jsonPath("$.usuario.correo").value("javiera.munoz@gmail.com"))
        .andExpect(jsonPath("$.usuario.password").doesNotExist())
        .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist())
        .andExpect(cookie().httpOnly("ev_refresh", true))
        .andExpect(cookie().path("ev_refresh", "/api/v1/auth"))
        .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")));
  }

  @Test
  void elJwtCumpleElContratoDelReadme() throws Exception {
    String token =
        json.readTree(login("pedro.salinas@duoc.cl", "vendedor1234").getResponse().getContentAsString())
            .get("accessToken")
            .asText();
    SignedJWT jwt = SignedJWT.parse(token);
    assertEquals("RS256", jwt.getHeader().getAlgorithm().getName());
    assertEquals("elvolcan-usuarios", jwt.getJWTClaimsSet().getIssuer());
    assertEquals(List.of("elvolcan-api"), jwt.getJWTClaimsSet().getAudience());
    assertEquals(List.of("VENDEDOR"), jwt.getJWTClaimsSet().getStringListClaim("roles"));
    assertNotEquals(null, jwt.getHeader().getKeyID());
  }

  @Test
  void loginIncorrectoDevuelve401ConElMismoMensaje() throws Exception {
    mvc.perform(
            post("/api/v1/auth/login")
                .header(ENCABEZADO, VALOR)
                .contentType("application/json")
                .content("{\"correo\":\"javiera.munoz@gmail.com\",\"password\":\"mala\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos."));
    mvc.perform(
            post("/api/v1/auth/login")
                .header(ENCABEZADO, VALOR)
                .contentType("application/json")
                .content("{\"correo\":\"nadie@gmail.com\",\"password\":\"admin1234\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos."));
  }

  @Test
  void loginSinEncabezadoOConOrigenAjenoDevuelve403() throws Exception {
    String cuerpo = "{\"correo\":\"javiera.munoz@gmail.com\",\"password\":\"admin1234\"}";
    mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(cuerpo))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/auth/login")
                .header(ENCABEZADO, VALOR)
                .header("Origin", "https://otro-sitio.example")
                .contentType("application/json")
                .content(cuerpo))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/auth/login")
                .header(ENCABEZADO, VALOR)
                .header("Origin", "http://localhost:5173")
                .contentType("application/json")
                .content(cuerpo))
        .andExpect(status().isOk());
  }

  @Test
  void jwksPublicaSoloLaClavePublica() throws Exception {
    mvc.perform(get("/api/v1/auth/jwks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
        .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
        .andExpect(jsonPath("$.keys[0].kid").isNotEmpty())
        .andExpect(jsonPath("$.keys[0].n").isNotEmpty())
        .andExpect(jsonPath("$.keys[0].d").doesNotExist());
  }

  @Test
  void meConTokenRealDevuelveElUsuario() throws Exception {
    String token =
        json.readTree(login("camila.toro@gmail.com", "cliente1234").getResponse().getContentAsString())
            .get("accessToken")
            .asText();
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correo").value("camila.toro@gmail.com"))
        .andExpect(jsonPath("$.run").value("205671129"))
        .andExpect(jsonPath("$.comuna").value("chillan-viejo"));
  }

  @Test
  void meSinTokenOConTokenAlteradoDevuelve401() throws Exception {
    mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    String token =
        json.readTree(login("camila.toro@gmail.com", "cliente1234").getResponse().getContentAsString())
            .get("accessToken")
            .asText();
    // Cambiar un carácter de la firma la invalida
    String alterado = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + alterado))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void clienteConTokenRealNoEntraAlCrudDeUsuarios() throws Exception {
    String token =
        json.readTree(login("camila.toro@gmail.com", "cliente1234").getResponse().getContentAsString())
            .get("accessToken")
            .asText();
    mvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  // ---------- Refresh y logout ----------

  @Test
  void refreshRotaElTokenYElAnteriorDejaDeServir() throws Exception {
    Cookie primera = login("javiera.munoz@gmail.com", "admin1234").getResponse().getCookie("ev_refresh");
    MvcResult r =
        mvc.perform(post("/api/v1/auth/refresh").header(ENCABEZADO, VALOR).cookie(primera))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.usuario.tipo").value("administrador"))
            .andReturn();
    Cookie segunda = r.getResponse().getCookie("ev_refresh");
    assertNotEquals(primera.getValue(), segunda.getValue());
    // El refresh usado ya no sirve
    mvc.perform(post("/api/v1/auth/refresh").header(ENCABEZADO, VALOR).cookie(primera))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refreshSinCookieDevuelve401() throws Exception {
    mvc.perform(post("/api/v1/auth/refresh").header(ENCABEZADO, VALOR))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logoutInvalidaElRefreshYBorraLaCookie() throws Exception {
    Cookie refresh = login("javiera.munoz@gmail.com", "admin1234").getResponse().getCookie("ev_refresh");
    mvc.perform(post("/api/v1/auth/logout").header(ENCABEZADO, VALOR).cookie(refresh))
        .andExpect(status().isNoContent())
        .andExpect(cookie().maxAge("ev_refresh", 0));
    mvc.perform(post("/api/v1/auth/refresh").header(ENCABEZADO, VALOR).cookie(refresh))
        .andExpect(status().isUnauthorized());
  }

  // ---------- Registro ----------

  static final String REGISTRO =
      """
      {"run":"12.345.678-5","nombre":"Ana","apellidos":"Pérez Soto","correo":"Ana.Perez@duoc.cl",
       "password":"ana1234","telefono":"912345678","region":"nuble","comuna":"bulnes",
       "direccion":"Calle 1 #23","tipo":"administrador"}
      """;

  @Test
  void registroCreaSiempreUnClienteYPuedeIniciarSesion() throws Exception {
    mvc.perform(post("/api/v1/auth/registro").contentType("application/json").content(REGISTRO))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.tipo").value("cliente"))
        .andExpect(jsonPath("$.run").value("123456785"))
        .andExpect(jsonPath("$.correo").value("ana.perez@duoc.cl"))
        .andExpect(jsonPath("$.password").doesNotExist());
    MvcResult r = login("ana.perez@duoc.cl", "ana1234");
    assertEquals(200, r.getResponse().getStatus());
    JsonNode cuerpo = json.readTree(r.getResponse().getContentAsString());
    assertEquals(
        List.of("CLIENTE"),
        SignedJWT.parse(cuerpo.get("accessToken").asText()).getJWTClaimsSet().getStringListClaim("roles"));
  }

  @Test
  void registroRechazaCorreoORunRepetidos() throws Exception {
    mvc.perform(
            post("/api/v1/auth/registro")
                .contentType("application/json")
                .content(REGISTRO.replace("Ana.Perez@duoc.cl", "CAMILA.TORO@gmail.com")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("Ya existe un usuario con ese correo."));
    mvc.perform(
            post("/api/v1/auth/registro")
                .contentType("application/json")
                .content(REGISTRO.replace("12.345.678-5", "20.567.112-9")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("Ya existe un usuario con ese RUN."));
  }

  @Test
  void registroInvalidoDevuelveUnErrorPorCampo() throws Exception {
    mvc.perform(
            post("/api/v1/auth/registro")
                .contentType("application/json")
                .content(
                    "{\"run\":\"12345678-0\",\"nombre\":\"\",\"correo\":\"ana@hotmail.com\","
                        + "\"password\":\"123\",\"telefono\":\"12\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.run").value("El RUN ingresado no es válido."))
        .andExpect(jsonPath("$.campos.nombre").exists())
        .andExpect(jsonPath("$.campos.apellidos").exists())
        .andExpect(jsonPath("$.campos.correo").value(containsString("dominio válido")))
        .andExpect(jsonPath("$.campos.password").exists())
        .andExpect(jsonPath("$.campos.telefono").exists())
        .andExpect(jsonPath("$.campos.region").exists())
        .andExpect(jsonPath("$.campos.direccion").exists());
  }

  // ---------- Datos guardados ----------

  @Test
  void laContrasenaSeGuardaConBcrypt() {
    String hash = usuarios.findByCorreo("javiera.munoz@gmail.com").orElseThrow().passwordHash;
    assertTrue(hash.startsWith("$2"));
    assertNotEquals("admin1234", hash);
  }

  @Test
  void runSeValidaConModulo11() {
    assertTrue(Run.esValido("19.123.456-1"));
    assertTrue(Run.esValido("191234561"));
    assertTrue(Run.esValido("17.894.567-K"));
    assertTrue(Run.esValido("17894567k"));
    assertTrue(!Run.esValido("17894567-1"));
    assertTrue(!Run.esValido("abc"));
  }

  @Test
  void seedTieneLosTresTiposDelFrontend() throws Exception {
    assertEquals(3, usuarios.count());
    mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status", not("DOWN")));
  }
}
