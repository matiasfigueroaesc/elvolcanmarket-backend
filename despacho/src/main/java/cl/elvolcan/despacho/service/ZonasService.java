package cl.elvolcan.despacho.service;

import cl.elvolcan.common.exception.ApiException;
import cl.elvolcan.despacho.dto.CoberturaDto;
import cl.elvolcan.despacho.dto.ZonaRequest;
import cl.elvolcan.despacho.dto.ZonaResponse;
import cl.elvolcan.despacho.model.Cobertura;
import cl.elvolcan.despacho.model.Zona;
import cl.elvolcan.despacho.repository.ZonasRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas de negocio de las zonas de despacho. */
@Service
public class ZonasService {

  private final ZonasRepository zonas;

  public ZonasService(ZonasRepository zonas) {
    this.zonas = zonas;
  }

  @Transactional(readOnly = true)
  public List<ZonaResponse> listar() {
    return zonas.findAllByOrderByPosicionAsc().stream().map(ZonasService::vista).toList();
  }

  @Transactional(readOnly = true)
  public ZonaResponse obtener(String id) {
    return vista(buscar(id));
  }

  /** Zonas que despachan a una comuna (puede ser más de una, o ninguna). Como zonasDeComuna(). */
  @Transactional(readOnly = true)
  public List<ZonaResponse> deComuna(String comuna) {
    return zonas.findAllByOrderByPosicionAsc().stream()
        .filter(z -> z.cubre(comuna))
        .map(ZonasService::vista)
        .toList();
  }

  @Transactional
  public ZonaResponse crear(ZonaRequest datos) {
    if (datos.id() == null) throw new ApiException(400, "El id de la zona es obligatorio.");
    if (zonas.existsById(datos.id())) throw new ApiException(409, "Ya existe una zona con ese id.");
    Zona z = new Zona();
    z.id = datos.id();
    z.posicion =
        zonas.findAllByOrderByPosicionAsc().stream().mapToInt(x -> x.posicion).max().orElse(0) + 1;
    copiar(datos, z);
    return vista(zonas.save(z));
  }

  @Transactional
  public ZonaResponse actualizar(String id, ZonaRequest datos) {
    Zona z = buscar(id);
    copiar(datos, z);
    return vista(z);
  }

  /**
   * Los despachos ya creados guardan el id de su zona como texto (sin llave foránea),
   * así que eliminar una zona no rompe el historial.
   */
  @Transactional
  public void eliminar(String id) {
    zonas.delete(buscar(id));
  }

  // ---------- Apoyo ----------

  private Zona buscar(String id) {
    return zonas.findById(id).orElseThrow(() -> new ApiException(404, "Zona no encontrada."));
  }

  private static void copiar(ZonaRequest d, Zona z) {
    String detalle = limpiar(d.detalle());
    if (d.cobertura().isEmpty() && detalle == null)
      throw new ApiException(400, "La zona debe tener comunas cubiertas o un detalle.");
    if (d.cobertura().stream().map(c -> c.comuna().trim() + "|" + limpiar(c.sector())).distinct().count()
        != d.cobertura().size())
      throw new ApiException(400, "La zona tiene comunas repetidas.");
    z.nombre = d.nombre().trim();
    z.cobertura.clear();
    for (CoberturaDto c : d.cobertura())
      z.cobertura.add(new Cobertura(c.comuna().trim(), limpiar(c.sector())));
    z.detalle = detalle;
    z.dias = d.dias().trim();
    z.horario = d.horario().trim();
    z.tiempo = d.tiempo().trim();
  }

  static ZonaResponse vista(Zona z) {
    return new ZonaResponse(
        z.id,
        z.nombre,
        z.cobertura.stream().map(c -> new CoberturaDto(c.comuna, c.sector)).toList(),
        z.detalle,
        z.dias,
        z.horario,
        z.tiempo);
  }

  private static String limpiar(String texto) {
    return texto == null || texto.isBlank() ? null : texto.trim();
  }
}
