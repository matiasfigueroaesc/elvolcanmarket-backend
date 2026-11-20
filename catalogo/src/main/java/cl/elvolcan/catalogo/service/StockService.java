package cl.elvolcan.catalogo.service;

import cl.elvolcan.catalogo.dto.VentaItemRequest;
import cl.elvolcan.catalogo.dto.VentaLineaResponse;
import cl.elvolcan.catalogo.dto.VentaResponse;
import cl.elvolcan.catalogo.model.Producto;
import cl.elvolcan.catalogo.model.Venta;
import cl.elvolcan.catalogo.repository.ProductosRepository;
import cl.elvolcan.catalogo.repository.VentasRepository;
import cl.elvolcan.common.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Descuento de stock cuando se confirma una compra. Lo llama el servicio ventas
 * (nunca el navegador) por el endpoint interno.
 *
 * Reglas:
 *  - todo o nada: si un producto no tiene stock, no se descuenta ninguno (@Transactional)
 *  - el precio lo pone el catálogo (con oferta), no el carrito del navegador
 *  - idempotente: repetir la misma boleta no descuenta dos veces
 */
@Service
public class StockService {

  private final ProductosRepository productos;
  private final VentasRepository ventas;
  private final ObjectMapper json;

  public StockService(ProductosRepository productos, VentasRepository ventas, ObjectMapper json) {
    this.productos = productos;
    this.ventas = ventas;
    this.json = json;
  }

  @Transactional
  public VentaResponse vender(String boletaId, List<VentaItemRequest> items) {
    validarItems(items);

    // Orden fijo por id: evita bloqueos cruzados entre dos compras simultáneas.
    List<VentaItemRequest> ordenados =
        items.stream().sorted(Comparator.comparing(VentaItemRequest::id)).toList();
    String huella =
        ordenados.stream().map(i -> i.id() + "x" + i.cantidad()).collect(Collectors.joining(","));

    Optional<Venta> previa = ventas.findById(boletaId);
    if (previa.isPresent()) {
      if (!previa.get().huella.equals(huella))
        throw new ApiException(409, "La boleta ya fue registrada con otros productos.");
      return leer(previa.get().comprobante);
    }

    List<VentaLineaResponse> lineas = new ArrayList<>();
    int total = 0;
    for (VentaItemRequest item : ordenados) {
      Producto p =
          productos
              .bloquear(item.id())
              .filter(x -> x.activo)
              .orElseThrow(() -> new ApiException(409, "Un producto ya no está disponible."));
      if (p.stock < item.cantidad())
        throw new ApiException(
            409, "Stock insuficiente de " + p.nombre + " (disponible: " + p.stock + ").");
      p.stock -= item.cantidad();
      int precio = p.precioFinal();
      lineas.add(new VentaLineaResponse(p.id, p.codigo, p.nombre, precio, item.cantidad(), p.imagen));
      total += precio * item.cantidad();
    }

    VentaResponse respuesta = new VentaResponse(lineas, total);
    ventas.saveAndFlush(new Venta(boletaId, huella, escribir(respuesta)));
    return respuesta;
  }

  private void validarItems(List<VentaItemRequest> items) {
    boolean invalido =
        items == null
            || items.isEmpty()
            || items.size() > 50
            || items.stream().anyMatch(i -> i == null || i.id() == null || i.cantidad() < 1)
            || items.stream().map(VentaItemRequest::id).distinct().count() != items.size();
    if (invalido)
      throw new ApiException(
          400, "La compra requiere productos sin repetir y cantidades enteras positivas.");
  }

  private String escribir(VentaResponse respuesta) {
    try {
      return json.writeValueAsString(respuesta);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private VentaResponse leer(String comprobante) {
    try {
      return json.readValue(comprobante, VentaResponse.class);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
