package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.DisponibilidadProductoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.LoteDisponibleDTO;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventarioConsultaServiceImpl implements InventarioConsultaService {

    private final LoteProductoRepository loteProductoRepository;
    private final ProductoService productoService;

    @Override
    public DisponibilidadProductoResponseDTO obtenerDisponibilidadPorProducto(Long productoId) {
        Producto producto = productoService.findById(productoId);

        Map<String, BigDecimal> totalesPorEstado = new HashMap<>();
        for (EstadoLote estado : EstadoLote.values()) {
            totalesPorEstado.put(estado.name(), BigDecimal.ZERO);
        }

        List<Object[]> resultados = loteProductoRepository.sumarStockPorEstado(productoId);
        for (Object[] fila : resultados) {
            EstadoLote estado = (EstadoLote) fila[0];
            BigDecimal total = (BigDecimal) fila[1];
            if (total != null) {
                totalesPorEstado.put(estado.name(), total);
            }
        }

        // CODEx: uso actual de la consulta FIFO disponible
        List<LoteDisponibleDTO> lotesDisponibles = loteProductoRepository
                .findDisponiblesFifo(productoId, List.of(EstadoLote.DISPONIBLE, EstadoLote.LIBERADO))
                .stream()
                .map(lp -> LoteDisponibleDTO.builder()
                        .loteProductoId(lp.getId())
                        .codigoLote(lp.getCodigoLote())
                        .stockLote(lp.getStockLote())
                        .fechaVencimiento(lp.getFechaVencimiento())
                        .almacenId(lp.getAlmacen() != null ? lp.getAlmacen().getId() : null)
                        .nombreAlmacen(lp.getAlmacen() != null ? lp.getAlmacen().getNombre() : null)
                        .build())
                .collect(Collectors.toList());

        BigDecimal totalDisponible = totalesPorEstado.getOrDefault(EstadoLote.DISPONIBLE.name(), BigDecimal.ZERO);

        String unidadMedida = null;
        if (producto.getUnidadMedida() != null) {
            unidadMedida = producto.getUnidadMedida().getSimbolo();
            if (unidadMedida == null || unidadMedida.isEmpty()) {
                unidadMedida = producto.getUnidadMedida().getNombre();
            }
        }

        return DisponibilidadProductoResponseDTO.builder()
                .productoId(producto.getId().longValue())
                .nombreProducto(producto.getNombre())
                .totalesPorEstado(totalesPorEstado)
                .lotesDisponiblesFIFO(lotesDisponibles)
                .totalDisponible(totalDisponible)
                .unidadMedida(unidadMedida)
                .build();
    }
}
