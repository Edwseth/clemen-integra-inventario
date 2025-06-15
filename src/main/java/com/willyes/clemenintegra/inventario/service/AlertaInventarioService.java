package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.LoteAlertaResponseDTO;
import com.willyes.clemenintegra.inventario.dto.LoteEstadoProlongadoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoAlertaResponseDTO;

import java.util.List;

public interface AlertaInventarioService {
    List<ProductoAlertaResponseDTO> obtenerProductosConStockBajo();
    List<LoteAlertaResponseDTO> obtenerLotesVencidos();
    List<LoteEstadoProlongadoResponseDTO> obtenerLotesRetenidosOCuarentenaProlongados();
}
