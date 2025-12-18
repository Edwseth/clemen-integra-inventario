package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.config.InventoryVencidosProperties;
import com.willyes.clemenintegra.inventario.dto.LoteConsumoDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioFiltroDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoInventarioService {

    MovimientoInventarioResponseDTO registrarMovimiento(MovimientoInventarioDTO dto);
    MovimientoInventarioResponseDTO registrarMovimiento(MovimientoInventarioDTO dto, String idempotencyKey);
    void consumirInsumosPorOrden(Long ordenProduccionId, Long usuarioId);

    List<LoteConsumoDTO> simulateFefo(Long productoId, BigDecimal cantidad, Long almacenId);

    Page<MovimientoInventarioResponseDTO> filtrar(
            LocalDateTime fechaInicio, LocalDateTime fechaFin,
            Long productoId, Long almacenId,
            TipoMovimiento tipoMovimiento, ClasificacionMovimientoInventario clasificacion,
            Pageable pageable);

    List<MovimientoInventarioResponseDTO> consultarMovimientos(MovimientoInventarioFiltroDTO filtro);

    Workbook generarReporteMovimientosExcel(LocalDateTime inicio, LocalDateTime fin);

    Page<MovimientoInventarioResponseDTO> listarTodos(String codigoRecepcion, TipoMovimiento tipoMovimiento, Pageable pageable);

    MovimientoInventario registrarRetiroPorVencimiento(LoteProducto lote,
                                                       InventoryVencidosProperties properties,
                                                       java.time.LocalDateTime fechaMovimiento);

    boolean existeMovimientoVencimientoHoy(Long loteId,
                                           Long motivoId,
                                           java.time.LocalDateTime fechaInicio,
                                           java.time.LocalDateTime fechaFin);

}
