package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.ByteArrayOutputStream;
import java.util.List;

public interface LoteProductoService {
    LoteProductoResponseDTO crearLote(LoteProductoRequestDTO dto);
    List<LoteProductoResponseDTO> obtenerLotesPorEstado(String estado);
    Workbook generarReporteLotesPorVencerExcel();
    ByteArrayOutputStream generarReporteAlertasActivasExcel();
    List<LoteProductoResponseDTO> listarTodos();
}
