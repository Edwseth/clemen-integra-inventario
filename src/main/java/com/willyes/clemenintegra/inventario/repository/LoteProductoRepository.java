package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.math.BigDecimal;

@Repository
public interface LoteProductoRepository extends JpaRepository<LoteProducto, Long>, JpaSpecificationExecutor<LoteProducto> {

    Optional<LoteProducto> findByCodigoLoteAndProductoId(String codigoLote, Long productoId);
    boolean existsByProducto(Producto producto);
    boolean existsByCodigoLote(String codigoLote);
    List<LoteProducto> findByEstado(EstadoLote estado);
    Optional<LoteProducto> findByCodigoLote(String codigoLote);
    @Query("SELECT lp FROM LoteProducto lp JOIN FETCH lp.almacen a WHERE lp.fechaVencimiento BETWEEN :inicio AND :fin")
    List<LoteProducto> findByFechaVencimientoBetween(@Param("inicio") LocalDateTime inicio,
                                                     @Param("fin") LocalDateTime fin);
    Optional<LoteProducto> findByCodigoLoteAndProductoIdAndAlmacenId(String codigoLote, Integer productoId, Integer almacenId);
    List<LoteProducto> findByEstadoIn(List<EstadoLote> estados);
    List<LoteProducto> findByEstadoInAndProducto_TipoAnalisisIn(List<EstadoLote> estados, List<TipoAnalisisCalidad> tipos);
    List<LoteProducto> findByFechaVencimientoBeforeAndEstadoNotIn(LocalDateTime fecha,
                                                                  List<EstadoLote> estados);
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

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT l FROM LoteProducto l
    WHERE l.producto.id = :productoId
      AND l.estado IN :estadosValidos
      AND l.agotado = false
      AND (l.stockLote - l.stockReservado) > 0
      AND (:almacenesIds IS NULL OR l.almacen.id IN :almacenesIds)
    ORDER BY
      CASE WHEN l.fechaVencimiento IS NULL THEN 1 ELSE 0 END,
      l.fechaVencimiento ASC,
      l.fechaFabricacion ASC,
      l.id ASC
    """)
    List<LoteProducto> findDisponiblesFefo(@Param("productoId") Long productoId,
                                           @Param("estadosValidos") Collection<EstadoLote> estadosValidos,
                                           @Param("almacenesIds") Collection<Long> almacenesIds);

    @Modifying
    @Query(value = """
      UPDATE lotes_productos
      SET stock_reservado = stock_reservado + :cantidad,
          agotado = CASE WHEN stock_lote - stock_reservado - :cantidad <= 0 THEN true ELSE agotado END,
          fecha_agotado = CASE WHEN stock_lote - stock_reservado - :cantidad <= 0 THEN NOW() ELSE fecha_agotado END
      WHERE id = :loteId AND agotado = false AND (stock_lote - stock_reservado) >= :cantidad
    """, nativeQuery = true)
    int reservarStock(@Param("loteId") Long loteId, @Param("cantidad") BigDecimal cantidad);

    // LÍNEA CODEx: nuevas consultas para disponibilidad detallada por producto
    @Query("""
      select lp.estado as estado, coalesce(sum(lp.stockLote),0)
      from LoteProducto lp
      where lp.producto.id = :productoId and lp.stockLote > 0
      group by lp.estado
    """)
    List<Object[]> sumarPorEstado(@Param("productoId") Long productoId);

    @Query("""
     select new com.willyes.clemenintegra.bom.dto.LoteResumenDTO(
        lp.id, lp.codigoLote, lp.estado, a.nombre, lp.stockLote,
        lp.fechaVencimiento, lp.fechaLiberacion, u.nombreCompleto
     )
     from LoteProducto lp
     left join lp.almacen a
     left join lp.usuarioLiberador u
     where lp.producto.id = :productoId and lp.stockLote > 0
     order by lp.estado asc, lp.fechaVencimiento asc
    """)
    List<com.willyes.clemenintegra.bom.dto.LoteResumenDTO> listarLotesPorProducto(@Param("productoId") Long productoId);
}

