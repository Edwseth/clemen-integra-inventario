package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenCompraDetalleRepository extends JpaRepository<OrdenCompraDetalle, Long> {
    void deleteByOrdenCompra_Id(Long ordenCompraId);
}
