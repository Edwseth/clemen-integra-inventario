package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.OrdenCompraDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenCompraDocumentoRepository extends JpaRepository<OrdenCompraDocumento, Long> {
    List<OrdenCompraDocumento> findByOrdenCompra_Id(Long ordenCompraId);
}
