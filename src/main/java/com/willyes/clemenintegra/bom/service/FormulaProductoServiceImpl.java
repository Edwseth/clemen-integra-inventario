package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.dto.*;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.*;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.*;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FormulaProductoServiceImpl implements FormulaProductoService {

    private final FormulaProductoRepository formulaRepository;
    private final BomMapper bomMapper;
    private final LoteProductoRepository loteProductoRepository;

    public List<FormulaProducto> listarTodas() {
        return formulaRepository.findAll();
    }

    public Optional<FormulaProducto> buscarPorId(Long id) {
        return formulaRepository.findById(id);
    }

    public FormulaProducto guardar(FormulaProducto formula) {
        if (formula.getEstado() == EstadoFormula.APROBADA) {
            formula.setActivo(true);
        }
        return formulaRepository.save(formula);
    }

    public void eliminar(Long id) {
        formulaRepository.deleteById(id);
    }

    @Override
    public FormulaProductoResponse obtenerFormulaActivaPorProducto(Long productoId, BigDecimal cantidad) {
        FormulaProducto formula = formulaRepository
                .findByProductoIdAndEstadoAndActivoTrue(productoId, EstadoFormula.APROBADA)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe fórmula activa aprobada para este producto."));

        FormulaProductoResponse response = bomMapper.toResponseDTO(formula);
        response.unidadBaseFormula = formula.getProducto() != null && formula.getProducto().getUnidadMedida() != null
                ? formula.getProducto().getUnidadMedida().getSimbolo() : null;
        response.cantidadBaseFormula = BigDecimal.ONE;

        BigDecimal cantidadProduccion = (cantidad != null && cantidad.compareTo(BigDecimal.ZERO) > 0)
                ? cantidad
                : BigDecimal.ONE;

        if (response.detalles != null && formula.getDetalles() != null) {
            List<DetalleFormula> detallesEntidad = formula.getDetalles();
            for (int i = 0; i < detallesEntidad.size(); i++) {
                DetalleFormula entidad = detallesEntidad.get(i);
                DetalleFormulaResponse dto = response.detalles.get(i);

                BigDecimal totalNecesaria = entidad.getCantidadNecesaria().multiply(cantidadProduccion);
                dto.cantidadTotalNecesaria = totalNecesaria;

                // LÍNEA CODEx: antes se tomaba stockActual directo del producto sin discriminar lotes
                Long insumoId = entidad.getInsumo().getId().longValue();
                DisponibilidadInsumoDTO disponibilidad = new DisponibilidadInsumoDTO();

                java.util.Map<EstadoLote, BigDecimal> totales = new java.util.EnumMap<>(EstadoLote.class);
                for (EstadoLote e : EstadoLote.values()) {
                    totales.put(e, BigDecimal.ZERO);
                }

                java.util.List<Object[]> filas = loteProductoRepository.sumarPorEstado(insumoId);
                for (Object[] fila : filas) {
                    EstadoLote estado = (EstadoLote) fila[0];
                    BigDecimal total = (BigDecimal) fila[1];
                    if (total != null) {
                        totales.put(estado, total);
                    }
                }

                java.util.List<LoteResumenDTO> lotes = loteProductoRepository.listarLotesPorProducto(insumoId);
                BigDecimal vencidoExtra = BigDecimal.ZERO;
                java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
                for (LoteResumenDTO lote : lotes) {
                    if (lote.getFechaVencimiento() != null && lote.getFechaVencimiento().isBefore(ahora)) {
                        BigDecimal st = lote.getStockLote();
                        vencidoExtra = vencidoExtra.add(st);
                        totales.put(lote.getEstado(), totales.get(lote.getEstado()).subtract(st));
                    }
                }
                totales.put(EstadoLote.VENCIDO, totales.get(EstadoLote.VENCIDO).add(vencidoExtra));

                disponibilidad.setDisponible(totales.get(EstadoLote.DISPONIBLE).add(totales.get(EstadoLote.LIBERADO)));
                disponibilidad.setEnCuarentena(totales.get(EstadoLote.EN_CUARENTENA));
                disponibilidad.setRetenido(totales.get(EstadoLote.RETENIDO));
                disponibilidad.setRechazado(totales.get(EstadoLote.RECHAZADO));
                disponibilidad.setVencido(totales.get(EstadoLote.VENCIDO));
                BigDecimal totalProducto = disponibilidad.getDisponible()
                        .add(disponibilidad.getEnCuarentena())
                        .add(disponibilidad.getRetenido())
                        .add(disponibilidad.getRechazado())
                        .add(disponibilidad.getVencido());
                disponibilidad.setTotalProducto(totalProducto);

                boolean insuficiente = disponibilidad.getDisponible().compareTo(totalNecesaria) < 0;
                String motivo = "OK";
                if (insuficiente) {
                    if (disponibilidad.getEnCuarentena().compareTo(BigDecimal.ZERO) > 0) {
                        motivo = "CUARENTENA";
                    } else if (disponibilidad.getRetenido().compareTo(BigDecimal.ZERO) > 0) {
                        motivo = "RETENIDO";
                    } else if (disponibilidad.getVencido().compareTo(BigDecimal.ZERO) > 0) {
                        motivo = "VENCIDO";
                    } else if (disponibilidad.getRechazado().compareTo(BigDecimal.ZERO) > 0) {
                        motivo = "RECHAZADO";
                    } else {
                        motivo = "SIN_STOCK";
                    }
                }

                dto.stockActual = disponibilidad.getTotalProducto();
                dto.estadoStock = disponibilidad.getDisponible().compareTo(totalNecesaria) >= 0 ? "SUFICIENTE" : "INSUFICIENTE";
                dto.disponibilidad = disponibilidad;
                dto.bloqueante = new BloqueanteDTO(insuficiente, motivo);
                dto.lotes = lotes;
            }
        }

        return response;
    }
}

