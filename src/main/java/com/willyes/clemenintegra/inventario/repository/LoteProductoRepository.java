package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoteProductoRepository extends JpaRepository<LoteProducto, Long> {

    Optional<LoteProducto> findByCodigoLoteAndProductoId(String codigoLote, Long productoId);
    boolean existsByProducto(Producto producto);
    boolean existsByCodigoLote(String codigoLote);
    List<LoteProducto> findByEstado(EstadoLote estado);
    Optional<LoteProducto> findByCodigoLote(String codigoLote);
    List<LoteProducto> findByFechaVencimientoBetween(LocalDateTime inicio, LocalDateTime fin);
    Optional<LoteProducto> findByCodigoLoteAndProductoIdAndAlmacenId(String codigoLote, Integer productoId, Integer almacenId);
    List<LoteProducto> findByEstadoIn(List<EstadoLote> estados);
    List<LoteProducto> findByEstadoInAndProducto_TipoAnalisisIn(List<EstadoLote> estados, List<TipoAnalisisCalidad> tipos);
    Optional<LoteProducto> findFirstByProductoIdAndEstadoAndStockLoteGreaterThanOrderByFechaVencimientoAsc(
            Integer productoId,
            EstadoLote estado,
            BigDecimal stockLote
    );
}

