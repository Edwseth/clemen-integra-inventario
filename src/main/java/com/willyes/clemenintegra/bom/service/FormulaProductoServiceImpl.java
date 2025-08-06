package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.dto.DetalleFormulaResponse;
import com.willyes.clemenintegra.bom.dto.FormulaProductoResponse;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.*;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.*;
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

        BigDecimal cantidadProduccion = (cantidad != null && cantidad.compareTo(BigDecimal.ZERO) > 0)
                ? cantidad
                : BigDecimal.ONE;

        if (response.detalles != null && formula.getDetalles() != null) {
            List<DetalleFormula> detallesEntidad = formula.getDetalles();
            for (int i = 0; i < detallesEntidad.size(); i++) {
                DetalleFormula entidad = detallesEntidad.get(i);
                DetalleFormulaResponse dto = response.detalles.get(i);

                BigDecimal totalNecesaria = entidad.getCantidadNecesaria().multiply(cantidadProduccion);
                dto.cantidadTotalNecesaria = totalNecesaria.doubleValue();

                BigDecimal stockActual = entidad.getInsumo() != null ? entidad.getInsumo().getStockActual() : BigDecimal.ZERO;
                dto.stockActual = stockActual;
                dto.estadoStock = stockActual.compareTo(totalNecesaria) >= 0 ? "SUFICIENTE" : "INSUFICIENTE";
            }
        }

        return response;
    }
}

