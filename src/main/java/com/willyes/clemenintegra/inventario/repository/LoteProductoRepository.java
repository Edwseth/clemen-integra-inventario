package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT lp.estado, COALESCE(SUM(lp.stockLote), 0) " +
            "FROM LoteProducto lp " +
            "WHERE lp.producto.id = :productoId AND lp.stockLote > 0 " +
            "GROUP BY lp.estado")
    List<Object[]> sumarStockPorEstado(@Param("productoId") Long productoId);

    @Query("SELECT lp FROM LoteProducto lp " +
            "JOIN FETCH lp.almacen a " +
            "WHERE lp.producto.id = :productoId " +
            "AND lp.estado = 'DISPONIBLE' " +
            "AND lp.stockLote > 0 " +
            "ORDER BY lp.fechaVencimiento ASC")
    List<LoteProducto> findDisponiblesFifo(@Param("productoId") Long productoId);
}

