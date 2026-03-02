package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.calidad.dto.CondicionUsoResponseDTO;
import com.willyes.clemenintegra.calidad.model.enums.EstadoCondicionUso;
import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.LotePendienteUbicarPtResponseDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoPorLoteDTO;
import com.willyes.clemenintegra.calidad.dto.EstadoCalidadLoteResponseDTO;
import com.willyes.clemenintegra.calidad.dto.ReaperturaLoteRequestDTO;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface LoteProductoService {
    LoteProductoResponseDTO crearLote(LoteProductoRequestDTO dto);
    List<LoteProductoResponseDTO> obtenerLotesPorEstado(String estado);
    org.springframework.data.domain.Page<LoteProductoResponseDTO> obtenerLotesPorEvaluar(EstadoLote estado, org.springframework.data.domain.Pageable pageable);
    Workbook generarReporteLotesPorVencerExcel(LocalDateTime inicio, LocalDateTime fin);
    ByteArrayOutputStream generarReporteAlertasActivasExcel();
    Page<LoteProductoResponseDTO> listarTodos(String producto, Long productoId, EstadoLote estado, String almacen, Long almacenId, Boolean vencidos, LocalDateTime fechaInicio, LocalDateTime fechaFin, Pageable pageable);
    Page<LotePendienteUbicarPtResponseDTO> obtenerPendientesUbicarPt(Pageable pageable);

    LoteProductoResponseDTO liberarLote(Long id, String observacion);
    LoteProductoResponseDTO rechazarLote(Long id, String observacion);
    LoteProductoResponseDTO liberarLoteRetenido(Long id, String observacion);

    /**
     * Libera un lote validando las evaluaciones de calidad requeridas.
     *
     * @param loteId        identificador del lote a liberar
     * @param usuarioActual usuario autenticado que realiza la liberación
     * @return información del lote actualizado
     */
    LoteProductoResponseDTO liberarLotePorCalidad(Long loteId, com.willyes.clemenintegra.shared.model.Usuario usuarioActual, String observacion);

    EstadoCalidadLoteResponseDTO obtenerEstadoCalidad(Long loteId);

    List<CondicionUsoResponseDTO> listarCondicionesUso(Long loteId, EstadoCondicionUso estado);

    LoteProductoResponseDTO reabrirParaReevaluacion(Long loteId, ReaperturaLoteRequestDTO dto, com.willyes.clemenintegra.shared.model.Usuario usuarioActual);

    ProductoPorLoteDTO resolverProductoPorLote(String codigoLote, Long ordenProduccionId);

}
