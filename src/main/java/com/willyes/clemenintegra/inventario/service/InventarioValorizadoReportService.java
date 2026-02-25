package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.reportes.InventarioValorizadoRowDTO;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventarioValorizadoReportService {

    Page<InventarioValorizadoRowDTO> listar(Pageable pageable);

    Workbook generarExcel();
}
