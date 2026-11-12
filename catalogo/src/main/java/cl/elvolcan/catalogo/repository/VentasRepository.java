package cl.elvolcan.catalogo.repository;

import cl.elvolcan.catalogo.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VentasRepository extends JpaRepository<Venta, String> {}
