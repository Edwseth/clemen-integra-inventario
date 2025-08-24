package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioFiltroDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoInventarioService {

    MovimientoInventarioResponseDTO registrarMovimiento(MovimientoInventarioDTO dto);

    Page<MovimientoInventarioResponseDTO> filtrar(
            LocalDateTime fechaInicio, LocalDateTime fechaFin,
            Long productoId, Long almacenId,
            TipoMovimiento tipoMovimiento, ClasificacionMovimientoInventario clasificacion,
            Pageable pageable);

    List<MovimientoInventarioResponseDTO> consultarMovimientos(MovimientoInventarioFiltroDTO filtro);

    Workbook generarReporteMovimientosExcel();

    Page<MovimientoInventarioResponseDTO> listarTodos(Pageable pageable);

}
