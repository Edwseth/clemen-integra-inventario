package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;

public interface LoteProductoService {
    LoteProductoResponseDTO crearLote(LoteProductoRequestDTO dto);
    List<LoteProductoResponseDTO> obtenerLotesPorEstado(String estado);
    List<LoteProductoResponseDTO> obtenerLotesPorEvaluar();
    Workbook generarReporteLotesPorVencerExcel();
    ByteArrayOutputStream generarReporteAlertasActivasExcel();
    Page<LoteProductoResponseDTO> listarTodos(String producto, EstadoLote estado, String almacen, Boolean vencidos, LocalDateTime fechaInicio, LocalDateTime fechaFin, Pageable pageable);

    LoteProductoResponseDTO liberarLote(Long id);
    LoteProductoResponseDTO rechazarLote(Long id);
    LoteProductoResponseDTO liberarLoteRetenido(Long id);

    /**
     * Libera un lote validando las evaluaciones de calidad requeridas.
     *
     * @param loteId        identificador del lote a liberar
     * @param usuarioActual usuario autenticado que realiza la liberación
     * @return información del lote actualizado
     */
    LoteProductoResponseDTO liberarLotePorCalidad(Long loteId, com.willyes.clemenintegra.shared.model.Usuario usuarioActual);
}
