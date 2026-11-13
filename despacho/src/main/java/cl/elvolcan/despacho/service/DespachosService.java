package cl.elvolcan.despacho.service;

import cl.elvolcan.common.exception.ApiException;
import cl.elvolcan.despacho.dto.CambioEstadoResponse;
import cl.elvolcan.despacho.dto.DespachoNuevoRequest;
import cl.elvolcan.despacho.dto.DespachoResponse;
import cl.elvolcan.despacho.dto.SeguimientoResponse;
import cl.elvolcan.despacho.model.Despacho;
import cl.elvolcan.despacho.model.Estados;
import cl.elvolcan.despacho.model.Zona;
import cl.elvolcan.despacho.repository.DespachosRepository;
import cl.elvolcan.despacho.repository.ZonasRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de los despachos.
 *  - Se crean solo desde ventas (boleta pagada), siempre en "recibido".
 *  - Crear es idempotente: si ventas reintenta la misma boleta, se devuelve el que ya existe.
 *  - El estado solo avanza: recibido → preparando → en camino → entregado.
 *  - Cada cambio queda en el historial con su fecha.
 */
@Service
public class DespachosService {

  private final DespachosRepository despachos;
  private final ZonasRepository zonas;

  public DespachosService(DespachosRepository despachos, ZonasRepository zonas) {
    this.despachos = despachos;
    this.zonas = zonas;
  }

  @Transactional
  public DespachoResponse registrar(DespachoNuevoRequest d) {
    return despachos
        .findByBoletaId(d.boletaId())
        .map(DespachosService::vista)
        .orElseGet(
            () -> {
              Despacho nuevo = new Despacho();
              nuevo.boletaId = d.boletaId();
              nuevo.numero = d.numero().trim().toUpperCase(Locale.ROOT);
              nuevo.usuarioId = d.usuarioId();
              nuevo.region = d.region();
              nuevo.comuna = d.comuna();
              nuevo.entrega = d.entrega();
              nuevo.zonaId = zonaDe(d.comuna());
              nuevo.cambiarEstado(Estados.RECIBIDO, Instant.now());
              return vista(despachos.save(nuevo));
            });
  }

  /** Estado actual de varias boletas: { boletaId: estado }. Lo usa ventas para sus listados. */
  @Transactional(readOnly = true)
  public Map<Long, String> estados(List<Long> boletaIds) {
    if (boletaIds == null || boletaIds.isEmpty()) return Map.of();
    if (boletaIds.size() > 1000) throw new ApiException(400, "Máximo 1000 boletas por consulta.");
    return despachos.findByBoletaIdIn(boletaIds).stream()
        .collect(Collectors.toMap(x -> x.boletaId, x -> x.estado));
  }

  @Transactional(readOnly = true)
  public List<DespachoResponse> listar(String estado) {
    List<Despacho> lista =
        estado == null || estado.isBlank()
            ? despachos.findAllByOrderByBoletaIdDesc()
            : despachos.findByEstadoOrderByBoletaIdDesc(estado.trim());
    return lista.stream().map(DespachosService::vista).toList();
  }

  /** El personal ve cualquiera; un cliente solo el de su compra (si no, 404). */
  @Transactional(readOnly = true)
  public DespachoResponse obtener(Long boletaId, Long usuarioId, boolean esPersonal) {
    return despachos
        .findByBoletaId(boletaId)
        .filter(x -> esPersonal || (usuarioId != null && usuarioId.equals(x.usuarioId)))
        .map(DespachosService::vista)
        .orElseThrow(() -> new ApiException(404, "Despacho no encontrado."));
  }

  @Transactional
  public DespachoResponse cambiarEstado(Long boletaId, String nuevo) {
    Despacho d =
        despachos
            .findByBoletaId(boletaId)
            .orElseThrow(() -> new ApiException(404, "Despacho no encontrado."));
    if (Estados.ENTREGADO.equals(d.estado))
      throw new ApiException(409, "El pedido ya fue entregado.");
    if (!Estados.puedeAvanzar(d.estado, nuevo))
      throw new ApiException(
          409, "El pedido está \"" + d.estado + "\": solo puede avanzar a una etapa siguiente.");
    d.cambiarEstado(nuevo, Instant.now());
    return vista(d);
  }

  /** Búsqueda pública por número. Acepta " ord-000001 " igual que el frontend. */
  @Transactional(readOnly = true)
  public SeguimientoResponse seguimiento(String numero) {
    String buscado = numero == null ? "" : numero.trim().toUpperCase(Locale.ROOT);
    Despacho d =
        despachos
            .findByNumero(buscado)
            .orElseThrow(
                () ->
                    new ApiException(
                        404, "Pedido no encontrado. Verifica el número (ej: ORD-000001)."));
    Zona zona = d.zonaId == null ? null : zonas.findById(d.zonaId).orElse(null);
    return new SeguimientoResponse(
        d.numero,
        d.estado,
        d.entrega,
        zona != null ? zona.nombre : null,
        zona != null ? zona.tiempo : null,
        Estados.ORDEN,
        historial(d));
  }

  // ---------- Apoyo ----------

  /** Primera zona (en el orden de la tabla) que cubre la comuna; null si ninguna. */
  private String zonaDe(String comuna) {
    return zonas.findAllByOrderByPosicionAsc().stream()
        .filter(z -> z.cubre(comuna))
        .map(z -> z.id)
        .findFirst()
        .orElse(null);
  }

  private static List<CambioEstadoResponse> historial(Despacho d) {
    return d.historial.stream().map(c -> new CambioEstadoResponse(c.estado, c.fecha)).toList();
  }

  static DespachoResponse vista(Despacho d) {
    return new DespachoResponse(
        d.boletaId,
        d.numero,
        d.usuarioId,
        d.region,
        d.comuna,
        d.entrega,
        d.zonaId,
        d.estado,
        d.actualizado,
        historial(d));
  }
}
