package com.willyes.clemenintegra.inventario.service;


import com.willyes.clemenintegra.inventario.dto.*;

import java.util.List;

public interface AlertaInventarioService {
    List<ProductoAlertaResponseDTO> obtenerProductosConStockBajo();
    List<LoteAlertaResponseDTO> obtenerLotesVencidos();
    List<LoteEstadoProlongadoResponseDTO> obtenerLotesRetenidosOCuarentenaProlongados();
    List<AlertaInventarioResponseDTO> obtenerAlertasInventario();
}
