package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
                .productoId(producto.getId().longValue())
                .nombreProducto(producto.getNombre())
                .stockActual(producto.getStockActual())
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
                        .fechaVencimiento(LocalDate.from(lote.getFechaVencimiento()))
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
                        .fechaFabricacion(LocalDate.from(lote.getFechaFabricacion()))
                        .diasEnEstado((int) ChronoUnit.DAYS.between(lote.getFechaFabricacion(), LocalDate.now()))
                        .nombreProducto(lote.getProducto().getNombre())
                        .build())
                .collect(Collectors.toList());
    }

    public List<AlertaInventarioResponseDTO> obtenerAlertasInventario() {
        List<AlertaInventarioResponseDTO> alertas = new java.util.ArrayList<>();

        // Productos con stock bajo
        productoRepository.findAll().stream()
                .filter(this::tieneStockPorDebajoDelMinimo)
                .forEach(producto -> {
                    if (producto != null) {
                        alertas.add(AlertaInventarioResponseDTO.builder()
                                .tipo("Stock bajo")
                                .nombreProducto(producto.getNombre())
                                .nombreAlmacen("")
                                .codigoLote("")
                                .fechaVencimiento(null)
                                .stockActual(producto.getStockActual())
                                .stockMinimo(producto.getStockMinimo())
                                .estado("")
                                .criticidad("stock")
                                .build());
                    }
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
                            .fechaVencimiento(LocalDate.from(lote.getFechaVencimiento()))
                            .stockActual(lote.getStockLote())
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
                            .fechaVencimiento(LocalDate.from(lote.getFechaVencimiento()))
                            .stockActual(lote.getStockLote())
                            .stockMinimo(null)
                            .estado(lote.getEstado().name())
                            .criticidad("advertencia")
                            .build());
                });

        return alertas;
    }

}

