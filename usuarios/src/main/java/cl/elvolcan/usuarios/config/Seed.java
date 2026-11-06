package cl.elvolcan.usuarios.config;

import cl.elvolcan.usuarios.model.TipoUsuario;
import cl.elvolcan.usuarios.model.Usuario;
import cl.elvolcan.usuarios.repository.UsuariosRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Usuarios de prueba: los mismos de USUARIOS_SEED en src/data/usuarios.js del frontend.
 * Solo se cargan si la tabla está vacía. Las contraseñas se guardan con BCrypt y se pueden
 * cambiar con variables de entorno (ADMIN_PASSWORD, VENDEDOR_PASSWORD, CLIENTE_PASSWORD).
 */
@Configuration
public class Seed {

  @Bean
  CommandLineRunner cargarUsuarios(
      UsuariosRepository usuarios,
      PasswordEncoder passwords,
      @Value("${app.seed.admin-password}") String admin,
      @Value("${app.seed.vendedor-password}") String vendedor,
      @Value("${app.seed.cliente-password}") String cliente) {
    return args -> {
      if (usuarios.count() > 0) return;
      usuarios.save(usuario("191234561", "Javiera", "Muñoz Soto", "javiera.munoz@gmail.com",
          passwords.encode(admin), TipoUsuario.ADMINISTRADOR, "chillan", "Av. Libertad 100, Chillán"));
      usuarios.save(usuario("17894567K", "Pedro", "Salinas Rojas", "pedro.salinas@duoc.cl",
          passwords.encode(vendedor), TipoUsuario.VENDEDOR, "chillan", "Av. O'Higgins 456, Chillán"));
      usuarios.save(usuario("205671129", "Camila", "Toro Pizarro", "camila.toro@gmail.com",
          passwords.encode(cliente), TipoUsuario.CLIENTE, "chillan-viejo", "Camino a Chillán Viejo 789"));
    };
  }

  private static Usuario usuario(
      String run, String nombre, String apellidos, String correo, String hash, String tipo,
      String comuna, String direccion) {
    Usuario u = new Usuario();
    u.run = run;
    u.nombre = nombre;
    u.apellidos = apellidos;
    u.correo = correo;
    u.passwordHash = hash;
    u.tipo = tipo;
    u.region = "nuble";
    u.comuna = comuna;
    u.direccion = direccion;
    return u;
  }
}
