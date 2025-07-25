package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayOutputStream;
import java.util.List;

public interface LoteProductoService {
    LoteProductoResponseDTO crearLote(LoteProductoRequestDTO dto);
    List<LoteProductoResponseDTO> obtenerLotesPorEstado(String estado);
    List<LoteProductoResponseDTO> obtenerLotesPorEvaluar();
    Workbook generarReporteLotesPorVencerExcel();
    ByteArrayOutputStream generarReporteAlertasActivasExcel();
    Page<LoteProductoResponseDTO> listarTodos(Pageable pageable);

    LoteProductoResponseDTO liberarLote(Long id);
    LoteProductoResponseDTO rechazarLote(Long id);
    LoteProductoResponseDTO liberarLoteRetenido(Long id);
}
