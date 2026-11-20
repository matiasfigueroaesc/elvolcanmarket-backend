package cl.elvolcan.usuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Refresh token vigente. Solo se guarda su hash SHA-256: si alguien lee la base,
 * no obtiene tokens que pueda usar. Cada refresh borra la fila y crea otra (rotación).
 */
@Entity
@Table(name = "sesion_refresh")
public class SesionRefresh {
  @Id
  @Column(length = 64)
  public String tokenHash;

  @Column(nullable = false)
  public Long usuarioId;

  @Column(nullable = false)
  public Instant expira;

  public SesionRefresh() {}

  public SesionRefresh(String tokenHash, Long usuarioId, Instant expira) {
    this.tokenHash = tokenHash;
    this.usuarioId = usuarioId;
    this.expira = expira;
  }
}
