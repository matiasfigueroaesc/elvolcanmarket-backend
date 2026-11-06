package cl.elvolcan.usuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Tabla USUARIO del modelo de la pauta. Los campos son los mismos de
 * src/data/usuarios.js del frontend, salvo la contraseña, que aquí se guarda
 * como hash BCrypt (nunca en texto plano).
 */
@Entity
@Table(name = "usuario")
public class Usuario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  /** RUN sin puntos ni guion y con K mayúscula, ej. 191234561. Único. */
  @Column(nullable = false, unique = true, length = 12)
  public String run;

  @Column(nullable = false, length = 50)
  public String nombre;

  @Column(nullable = false, length = 100)
  public String apellidos;

  /** Correo en minúsculas. Es el "usuario" con el que se inicia sesión. Único. */
  @Column(nullable = false, unique = true, length = 150)
  public String correo;

  /** Hash BCrypt de la contraseña (empieza con $2a$). */
  @Column(nullable = false, length = 72)
  public String passwordHash;

  @Column(length = 9)
  public String telefono;

  public LocalDate fechaNacimiento;

  /** "administrador", "vendedor" o "cliente" (los mismos valores del frontend). */
  @Column(nullable = false, length = 20)
  public String tipo = TipoUsuario.CLIENTE;

  @Column(length = 50)
  public String region;

  @Column(length = 50)
  public String comuna;

  @Column(length = 300)
  public String direccion;

  /** Borrado lógico: un usuario eliminado no puede iniciar sesión, pero sus boletas siguen válidas. */
  @Column(nullable = false)
  public boolean activo = true;

  public Usuario() {}
}
