package cl.elvolcan.ventas.service;

import cl.elvolcan.common.exception.ApiException;
import cl.elvolcan.ventas.client.CatalogoClient;
import cl.elvolcan.ventas.client.DespachoClient;
import cl.elvolcan.ventas.dto.BoletaRequest;
import cl.elvolcan.ventas.dto.BoletaResponse;
import cl.elvolcan.ventas.dto.CatalogoItem;
import cl.elvolcan.ventas.dto.CatalogoLinea;
import cl.elvolcan.ventas.dto.CatalogoProducto;
import cl.elvolcan.ventas.dto.CatalogoVenta;
import cl.elvolcan.ventas.dto.ClienteDto;
import cl.elvolcan.ventas.dto.DespachoNuevo;
import cl.elvolcan.ventas.dto.DireccionDto;
import cl.elvolcan.ventas.dto.ItemRequest;
import cl.elvolcan.ventas.dto.ItemResponse;
import cl.elvolcan.ventas.model.Boleta;
import cl.elvolcan.ventas.model.DetalleBoleta;
import cl.elvolcan.ventas.repository.BoletasRepository;
import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Reglas de negocio de las boletas.
 *
 * Checkout (crear):
 *  1. Se guarda la boleta como "pendiente" para tener su id y su número (ORD-000001).
 *  2. Pago aprobado → el catálogo descuenta el stock y devuelve el precio real (todo o nada).
 *     Pago rechazado → NO se descuenta stock; solo se leen los productos para armar la boleta.
 *  3. Si el catálogo rechaza la compra (sin stock, producto eliminado...) se borra la boleta
 *     pendiente y se devuelve el error: no queda una boleta a medias.
 *  4. Con el pago aprobado se avisa a despacho, que crea el pedido en "recibido". Si despacho
 *     está caído la venta igual queda hecha y se reintenta después (DespachosPendientes).
 *
 * No hay @Transactional en crear(): no conviene tener una transacción de base de datos
 * abierta mientras se espera la respuesta HTTP de otro servicio.
 */
@Service
public class BoletasService {

  private static final Logger log = LoggerFactory.getLogger(BoletasService.class);

  private final BoletasRepository boletas;
  private final CatalogoClient catalogo;
  private final DespachoClient despacho;

  public BoletasService(
      BoletasRepository boletas, CatalogoClient catalogo, DespachoClient despacho) {
    this.boletas = boletas;
    this.catalogo = catalogo;
    this.despacho = despacho;
  }

  // ---------- Checkout ----------

  public BoletaResponse crear(BoletaRequest datos, Long usuarioId) {
    validarItems(datos.items());

    Boleta b = boletas.save(nueva(datos, usuarioId));
    b.numero = numero(b.id);
    try {
      if (datos.simularRechazo()) armarRechazada(b, datos.items());
      else armarPagada(b, datos.items());
      b = boletas.save(b);
    } catch (RuntimeException e) {
      boletas.deleteById(b.id);
      throw e;
    }

    if (!Boleta.PAGADO.equals(b.estadoPago)) return vista(b, null);
    return vista(b, registrarDespacho(b) ? "recibido" : null);
  }

  private void armarPagada(Boleta b, List<ItemRequest> items) {
    List<CatalogoItem> pedido =
        items.stream().map(i -> new CatalogoItem(i.id(), i.cantidad())).toList();
    CatalogoVenta venta = catalogo.vender(b.id, pedido);
    for (CatalogoLinea l : venta.lineas())
      b.agregar(new DetalleBoleta(l.id(), l.codigo(), l.nombre(), l.precio(), l.cantidad(), l.imagen()));
    b.total = venta.total();
    b.estadoPago = Boleta.PAGADO;
  }

  /** Pago rechazado: la boleta queda registrada (el admin la ve) pero el stock no se toca. */
  private void armarRechazada(Boleta b, List<ItemRequest> items) {
    int total = 0;
    for (ItemRequest i : items) {
      CatalogoProducto p = catalogo.producto(i.id());
      DetalleBoleta d =
          new DetalleBoleta(p.id(), p.codigo(), p.nombre(), p.precioFinal(), i.cantidad(), p.imagen());
      b.agregar(d);
      total += d.subtotal;
    }
    b.total = total;
    b.estadoPago = Boleta.RECHAZADO;
  }

  /** Devuelve true si despacho confirmó. Nunca lanza excepción: la venta ya está hecha. */
  private boolean registrarDespacho(Boleta b) {
    try {
      despacho.registrar(
          new DespachoNuevo(b.id, b.numero, b.usuarioId, b.region, b.comuna, b.entrega));
      b.despachoRegistrado = true;
      boletas.save(b);
      return true;
    } catch (RuntimeException e) {
      log.warn("Despacho no registró la boleta {}; se reintentará: {}", b.numero, e.getMessage());
      return false;
    }
  }

  /** Lo llama DespachosPendientes cada cierto tiempo. Devuelve cuántas quedaron registradas. */
  public int reintentarDespachosPendientes() {
    int registradas = 0;
    for (Boleta b : boletas.findByEstadoPagoAndDespachoRegistradoFalse(Boleta.PAGADO))
      if (registrarDespacho(b)) registradas++;
    return registradas;
  }

  // ---------- Consultas ----------

  /** Panel admin/vendedor. Filtros opcionales: texto (número, nombre o correo), estadoPago, usuarioId. */
  public List<BoletaResponse> listar(String texto, String estadoPago, Long usuarioId) {
    String buscado = normalizar(texto);
    Stream<Boleta> lista = boletas.findByEstadoPagoNotOrderByIdDesc(Boleta.PENDIENTE).stream();
    if (!buscado.isEmpty())
      lista =
          lista.filter(
              b ->
                  normalizar(
                          b.numero + " " + b.clienteNombre + " " + b.clienteApellidos + " "
                              + b.clienteCorreo)
                      .contains(buscado));
    if (estadoPago != null && !estadoPago.isBlank())
      lista = lista.filter(b -> b.estadoPago.equals(estadoPago.trim()));
    if (usuarioId != null) lista = lista.filter(b -> usuarioId.equals(b.usuarioId));
    return conEstados(lista.toList());
  }

  /** "Mis compras" del usuario con sesión. */
  public List<BoletaResponse> listarDeUsuario(Long usuarioId) {
    return conEstados(
        boletas.findByUsuarioIdAndEstadoPagoNotOrderByIdDesc(usuarioId, Boleta.PENDIENTE));
  }

  /**
   * Detalle. El personal (ADMIN/VENDEDOR) ve cualquiera; un cliente solo las suyas.
   * Si no es suya se responde 404 (no se revela que la boleta existe).
   */
  public BoletaResponse obtener(Long id, Long usuarioId, boolean esPersonal) {
    Boleta b =
        boletas
            .findById(id)
            .filter(x -> !Boleta.PENDIENTE.equals(x.estadoPago))
            .filter(x -> esPersonal || (usuarioId != null && usuarioId.equals(x.usuarioId)))
            .orElseThrow(() -> new ApiException(404, "Boleta no encontrada."));
    return conEstados(List.of(b)).get(0);
  }

  // ---------- Apoyo ----------

  private List<BoletaResponse> conEstados(List<Boleta> lista) {
    List<Long> pagadas =
        lista.stream().filter(b -> Boleta.PAGADO.equals(b.estadoPago)).map(b -> b.id).toList();
    Map<Long, String> estados = despacho.estados(pagadas);
    return lista.stream().map(b -> vista(b, estados.get(b.id))).toList();
  }

  private static void validarItems(List<ItemRequest> items) {
    if (items.stream().map(ItemRequest::id).distinct().count() != items.size())
      throw new ApiException(400, "El carrito tiene productos repetidos.");
  }

  private static Boleta nueva(BoletaRequest d, Long usuarioId) {
    Boleta b = new Boleta();
    b.fecha = Instant.now();
    b.usuarioId = usuarioId;
    b.clienteNombre = d.cliente().nombre().trim();
    b.clienteApellidos = d.cliente().apellidos().trim();
    b.clienteCorreo = d.cliente().correo().trim().toLowerCase(Locale.ROOT);
    b.calle = d.direccion().calle().trim();
    b.departamento = limpiar(d.direccion().departamento());
    b.region = d.direccion().region().trim();
    b.comuna = d.direccion().comuna().trim();
    b.indicaciones = limpiar(d.direccion().indicaciones());
    b.entrega = d.entrega();
    b.estadoPago = Boleta.PENDIENTE;
    return b;
  }

  /** 1 → "ORD-000001", igual que el frontend. */
  static String numero(Long id) {
    return "ORD-" + String.format("%06d", id);
  }

  public static BoletaResponse vista(Boleta b, String estado) {
    return new BoletaResponse(
        b.id,
        b.numero,
        b.fecha,
        b.usuarioId,
        new ClienteDto(b.clienteNombre, b.clienteApellidos, b.clienteCorreo),
        new DireccionDto(
            b.calle,
            b.departamento != null ? b.departamento : "",
            b.region,
            b.comuna,
            b.indicaciones != null ? b.indicaciones : ""),
        b.entrega,
        b.detalles.stream()
            .map(
                d ->
                    new ItemResponse(
                        d.productoId, d.codigo, d.nombre, d.precio, d.cantidad, d.subtotal, d.imagen))
            .toList(),
        b.total,
        b.estadoPago,
        estado);
  }

  private static String limpiar(String texto) {
    return texto == null || texto.isBlank() ? null : texto.trim();
  }

  private static String normalizar(String texto) {
    if (texto == null) return "";
    return Normalizer.normalize(texto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
  }
}
