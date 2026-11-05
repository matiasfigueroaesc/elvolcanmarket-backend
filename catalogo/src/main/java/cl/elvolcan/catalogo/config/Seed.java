package cl.elvolcan.catalogo.config;

import cl.elvolcan.catalogo.model.Categoria;
import cl.elvolcan.catalogo.model.Producto;
import cl.elvolcan.catalogo.repository.CategoriasRepository;
import cl.elvolcan.catalogo.repository.ProductosRepository;
import cl.elvolcan.catalogo.service.Textos;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Datos de fábrica: los mismos de src/data/categorias.js y src/data/productos.js del frontend.
 * Solo se cargan si la base está vacía (primer arranque), así no pisan los cambios del admin.
 */
@Configuration
public class Seed {

  @Bean
  CommandLineRunner cargarCatalogo(CategoriasRepository categorias, ProductosRepository productos) {
    return args -> {
      if (categorias.count() > 0) return;

      Categoria cilindros = categoria(categorias, "Cilindros de Gas", "cilindros",
          "Cilindros de gas licuado de 5, 11, 15 y 45 kg.", "/img/cilindro-11kg.png");
      Categoria reguladores = categoria(categorias, "Reguladores", "reguladores",
          "Reguladores de presión para uso doméstico e industrial.", "/img/regulador-estandar.png");
      Categoria mangueras = categoria(categorias, "Mangueras y Conexiones", "mangueras-conexiones",
          "Mangueras homologadas, abrazaderas y kits de conexión.", "/img/kit-conexion.png");
      Categoria accesorios = categoria(categorias, "Accesorios", "accesorios",
          "Carros, tapas protectoras y detectores de gas.", "/img/detector-gas.png");

      List<Producto> lista = new ArrayList<>();
      lista.add(producto("CL001", "Cilindro GLP 5 kg", 6500, null, 80, 10, "/img/cilindro-5kg.png",
          "Cilindro de gas licuado de petróleo 5 kg. Para uso residencial (cocina, calefacción pequeña).",
          "Unidad", cilindros));
      lista.add(producto("CL002", "Cilindro GLP 11 kg", 12000, null, 200, 10, "/img/cilindro-11kg.png",
          "Cilindro estándar doméstico. El más utilizado en hogares chilenos. Compatible con reguladores estándar.",
          "Unidad", cilindros));
      lista.add(producto("CL003", "Cilindro GLP 15 kg", 16000, null, 90, 10, "/img/cilindro-15kg.png",
          "Cilindro de mayor capacidad para hogares de alto consumo o locales pequeños.",
          "Unidad", cilindros));
      lista.add(producto("CL004", "Cilindro GLP 45 kg", 45000, 39990, 30, 10, "/img/cilindro-45kg.png",
          "Cilindro industrial. Uso comercial: restaurantes, talleres, calefacción de locales.",
          "Unidad", cilindros));
      lista.add(producto("RG001", "Regulador doméstico estándar", 8990, null, 45, 10, "/img/regulador-estandar.png",
          "Regulador de 1 etapa para cilindros 5, 11 y 15 kg. Presión de salida 28 mbar.",
          "Unidad", reguladores));
      lista.add(producto("RG002", "Regulador de alta presión", 18990, null, 12, 5, "/img/regulador-alta-presion.png",
          "Regulador para cocinas industriales o equipos de mayor consumo. Presión regulable.",
          "Unidad", reguladores));
      lista.add(producto("RG003", "Regulador dual (2 salidas)", 14990, null, 18, 5, "/img/regulador-dual.png",
          "Permite conectar dos artefactos simultáneamente al mismo cilindro.",
          "Unidad", reguladores));
      lista.add(producto("MG001", "Manguera gas 1.5 m", 3990, null, 80, 10, "/img/manguera-1.5m.png",
          "Manguera flexible homologada. Diámetro interior 9mm. Compatible con reguladores estándar.",
          "Unidad", mangueras));
      lista.add(producto("MG002", "Manguera gas 3 m", 6990, null, 50, 10, "/img/manguera-3m.png",
          "Manguera larga para instalaciones donde el artefacto está alejado del cilindro.",
          "Unidad", mangueras));
      lista.add(producto("MG003", "Abrazadera metálica", 990, null, 200, 10, "/img/abrazadera.png",
          "Abrazadera de acero para asegurar la conexión manguera-regulador y manguera-artefacto.",
          "Unidad", mangueras));
      lista.add(producto("MG004", "Kit conexión completo (regulador + manguera 1.5m + abrazaderas)", 12990, 10990, 25, 10,
          "/img/kit-conexion.png", "Todo lo necesario para instalar un cilindro nuevo.",
          "Kit", mangueras));
      lista.add(producto("AC001", "Carro porta cilindro 11/15 kg", 12990, null, 20, 5, "/img/carro-porta-cilindro.png",
          "Carro metálico con ruedas para transportar cilindros dentro del hogar con seguridad.",
          "Unidad", accesorios));
      lista.add(producto("AC002", "Tapa protectora para válvula", 1490, null, 60, 10, "/img/tapa-protectora.png",
          "Tapa de plástico ABS para proteger la válvula del cilindro durante el transporte.",
          "Unidad", accesorios));
      lista.add(producto("AC003", "Detector de gas a batería", 19990, 16990, 4, 5, "/img/detector-gas.png",
          "Sensor electroquímico. Alarma sonora y visual ante fuga de gas GLP o metano.",
          "Unidad", accesorios));
      productos.saveAll(lista);
    };
  }

  private static Categoria categoria(
      CategoriasRepository repo, String nombre, String slug, String descripcion, String imagen) {
    Categoria c = new Categoria();
    c.nombre = nombre;
    c.nombreClave = Textos.normalizar(nombre);
    // Se respeta el slug del frontend (ej. "mangueras-conexiones") para no romper sus enlaces.
    c.slug = slug;
    c.descripcion = descripcion;
    c.imagen = imagen;
    return repo.save(c);
  }

  private static Producto producto(
      String codigo, String nombre, int precio, Integer precioOferta, int stock, int stockCritico,
      String imagen, String descripcion, String unidad, Categoria categoria) {
    Producto p = new Producto();
    p.codigo = codigo;
    p.nombre = nombre;
    p.precio = precio;
    p.precioOferta = precioOferta;
    p.stock = stock;
    p.stockCritico = stockCritico;
    p.imagen = imagen;
    p.descripcion = descripcion;
    p.unidad = unidad;
    p.categoria = categoria;
    return p;
  }
}
