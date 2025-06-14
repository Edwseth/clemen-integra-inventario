package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoteProductoRepository extends JpaRepository<LoteProducto, Long> {

    boolean existsByProducto(Producto producto);
    boolean existsByCodigoLote(String codigoLote);
    List<LoteProducto> findByEstado(EstadoLote estado);
    Optional<LoteProducto> findByCodigoLote(String codigoLote);
    List<LoteProducto> findByFechaVencimientoBetween(LocalDate inicio, LocalDate fin);
}

