package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.LoteAlertaResponseDTO;
import com.willyes.clemenintegra.inventario.dto.LoteEstadoProlongadoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoAlertaResponseDTO;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertaInventarioServiceImpl implements AlertaInventarioService {

    private final ProductoRepository productoRepository;
    private final LoteProductoRepository loteProductoRepository;

    public List<ProductoAlertaResponseDTO> obtenerProductosConStockBajo() {
        return productoRepository.findAll().stream()
                .filter(this::tieneStockPorDebajoDelMinimo)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private boolean tieneStockPorDebajoDelMinimo(Producto producto) {
        return producto.getStockActual().compareTo(producto.getStockMinimo()) < 0;
    }

    private ProductoAlertaResponseDTO mapToResponse(Producto producto) {
        return ProductoAlertaResponseDTO.builder()
                .productoId(producto.getId())
                .nombreProducto(producto.getNombre())
                .stockActual(producto.getStockActual())
                .stockMinimo(producto.getStockMinimo())
                .build();
    }

    public List<LoteAlertaResponseDTO> obtenerLotesVencidos() {
        return loteProductoRepository.findAll().stream()
                .filter(lote -> lote.getFechaVencimiento() != null
                        && lote.getFechaVencimiento().isBefore(LocalDate.now()))
                .map(lote -> LoteAlertaResponseDTO.builder()
                        .loteId(lote.getId())
                        .codigoLote(lote.getCodigoLote())
                        .fechaVencimiento(lote.getFechaVencimiento())
                        .nombreProducto(lote.getProducto().getNombre())
                        .nombreAlmacen(lote.getAlmacen().getNombre())
                        .build())
                .collect(Collectors.toList());
    }

    public List<LoteEstadoProlongadoResponseDTO> obtenerLotesRetenidosOCuarentenaProlongados() {
        return loteProductoRepository.findAll().stream()
                .filter(lote ->
                        lote.getEstado() != null &&
                                (lote.getEstado().name().equals("EN_CUARENTENA") || lote.getEstado().name().equals("RETENIDO")) &&
                                lote.getFechaFabricacion() != null &&
                                lote.getFechaFabricacion().isBefore(LocalDate.now().minusDays(15))
                )
                .map(lote -> LoteEstadoProlongadoResponseDTO.builder()
                        .loteId(lote.getId())
                        .codigoLote(lote.getCodigoLote())
                        .estado(lote.getEstado().name())
                        .fechaFabricacion(lote.getFechaFabricacion())
                        .diasEnEstado((int) ChronoUnit.DAYS.between(lote.getFechaFabricacion(), LocalDate.now()))
                        .nombreProducto(lote.getProducto().getNombre())
                        .build())
                .collect(Collectors.toList());
    }

}

