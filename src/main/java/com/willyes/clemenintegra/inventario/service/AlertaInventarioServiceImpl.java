package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertaInventarioServiceImpl implements AlertaInventarioService {

    private final ProductoRepository productoRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final StockQueryService stockQueryService;

    public List<ProductoAlertaResponseDTO> obtenerProductosConStockBajo() {
        List<Producto> productos = productoRepository.findAll();
        Map<Long, BigDecimal> stockMap = stockQueryService.obtenerStockDisponible(
                productos.stream().map(p -> p.getId().longValue()).toList());
        return productos.stream()
                .filter(p -> stockMap.getOrDefault(p.getId().longValue(), BigDecimal.ZERO)
                        .compareTo(p.getStockMinimo()) < 0)
                .map(p -> mapToResponse(p, stockMap.getOrDefault(p.getId().longValue(), BigDecimal.ZERO)))
                .collect(Collectors.toList());
    }

    private ProductoAlertaResponseDTO mapToResponse(Producto producto, BigDecimal stockDisponible) {
        return ProductoAlertaResponseDTO.builder()
                .productoId(producto.getId().longValue())
                .nombreProducto(producto.getNombre())
                .stockDisponible(stockDisponible)
                .stockMinimo(producto.getStockMinimo())
                .build();
    }

    public List<LoteAlertaResponseDTO> obtenerLotesVencidos() {
        return loteProductoRepository.findAll().stream()
                .filter(lote -> lote.getFechaVencimiento() != null
                        && lote.getFechaVencimiento().isBefore(LocalDateTime.now()))
                .filter(lote -> lote.getProducto() != null)
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
                                lote.getFechaFabricacion().isBefore(LocalDate.now().minusDays(10).atStartOfDay())  // Cambia a 10 días para prolongados, campo para modificar la alerta
                )
                .filter(lote -> lote.getProducto() != null)
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

    public List<AlertaInventarioResponseDTO> obtenerAlertasInventario() {
        List<AlertaInventarioResponseDTO> alertas = new java.util.ArrayList<>();

        // Productos con stock bajo
        List<Producto> productos = productoRepository.findAll();
        Map<Long, BigDecimal> stockMap = stockQueryService.obtenerStockDisponible(
                productos.stream().map(p -> p.getId().longValue()).toList());
        productos.stream()
                .filter(p -> stockMap.getOrDefault(p.getId().longValue(), BigDecimal.ZERO)
                        .compareTo(p.getStockMinimo()) < 0)
                .forEach(producto -> {
                    BigDecimal stock = stockMap.getOrDefault(producto.getId().longValue(), BigDecimal.ZERO);
                    alertas.add(AlertaInventarioResponseDTO.builder()
                            .tipo("Stock bajo")
                            .nombreProducto(producto.getNombre())
                            .nombreAlmacen("")
                            .codigoLote("")
                            .fechaVencimiento(null)
                            .stockDisponible(stock)
                            .stockMinimo(producto.getStockMinimo())
                            .estado("")
                            .criticidad("stock")
                            .build());
                });

        // Lotes vencidos
        loteProductoRepository.findAll().stream()
                .filter(lote -> lote.getFechaVencimiento() != null && lote.getFechaVencimiento().isBefore(LocalDateTime.now()))
                .filter(lote -> lote.getProducto() != null)
                .forEach(lote -> {
                    String nombreAlmacen = lote.getAlmacen() != null && lote.getAlmacen().getNombre() != null
                            ? lote.getAlmacen().getNombre() : "";
                    alertas.add(AlertaInventarioResponseDTO.builder()
                            .tipo("Lote vencido")
                            .nombreProducto(lote.getProducto().getNombre())
                            .nombreAlmacen(nombreAlmacen)
                            .codigoLote(lote.getCodigoLote())
                            .fechaVencimiento(lote.getFechaVencimiento())
                            .stockDisponible(lote.getStockLote().subtract(lote.getStockReservado()))
                            .stockMinimo(null)
                            .estado("")
                            .criticidad("critica")
                            .build());
                });

        // Lotes en cuarentena o retenidos prolongados
        loteProductoRepository.findAll().stream()
                .filter(lote -> lote.getEstado() != null &&
                        (lote.getEstado().name().equals("EN_CUARENTENA") || lote.getEstado().name().equals("RETENIDO")))
                .filter(lote -> lote.getFechaFabricacion() != null &&
                        lote.getFechaFabricacion().isBefore(LocalDateTime.now().minusDays(15)))
                .filter(lote -> lote.getProducto() != null)
                .forEach(lote -> {
                    String nombreAlmacen = lote.getAlmacen() != null && lote.getAlmacen().getNombre() != null
                            ? lote.getAlmacen().getNombre() : "";
                    alertas.add(AlertaInventarioResponseDTO.builder()
                            .tipo("Lote en cuarentena")
                            .nombreProducto(lote.getProducto().getNombre())
                            .nombreAlmacen(nombreAlmacen)
                            .codigoLote(lote.getCodigoLote())
                            .fechaVencimiento(lote.getFechaVencimiento())
                            .stockDisponible(lote.getStockLote().subtract(lote.getStockReservado()))
                            .stockMinimo(null)
                            .estado(lote.getEstado().name())
                            .criticidad("advertencia")
                            .build());
                });

        return alertas;
    }

}

