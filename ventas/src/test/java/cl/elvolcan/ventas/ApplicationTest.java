package cl.elvolcan.ventas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Prueba mínima: el servicio arranca, responde su estado de salud y cierra las rutas no declaradas. */
@ActiveProfiles("h2")
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:ventas-test")
@AutoConfigureMockMvc
class ApplicationTest {

  @Autowired MockMvc mvc;

  @Test
  void arrancaYRespondeSalud() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void lasRutasNoDeclaradasEstanCerradas() throws Exception {
    mvc.perform(get("/api/v1/algo")).andExpect(status().isUnauthorized());
  }
}
