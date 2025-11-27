package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.DetalleFormulaRepository;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DetalleFormulaServiceImpl implements DetalleFormulaService {

    private final DetalleFormulaRepository detalleRepository;
    private final FormulaProductoRepository formulaRepository;

    @Override
    public List<DetalleFormula> listarTodas() {
        return detalleRepository.findAll();
    }

    @Override
    public Optional<DetalleFormula> buscarPorId(Long id) {
        return detalleRepository.findById(id);
    }

    @Override
    public DetalleFormula guardar(DetalleFormula detalle) {
        FormulaProducto formula = obtenerFormula(detalle);
        verificarFormulaEditable(formula);
        if (detalle.getInsumo() == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "El insumo es obligatorio para los detalles de fórmula.");
        }
        if (detalle.getUnidadMedida() == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "La unidad de medida del insumo es obligatoria para los detalles de fórmula.");
        }
        detalle.setFormula(formula);
        return detalleRepository.save(detalle);
    }

    @Override
    public void eliminar(Long id) {
        DetalleFormula existente = detalleRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Detalle de fórmula no encontrado"));
        verificarFormulaEditable(existente.getFormula());
        detalleRepository.delete(existente);
    }

    private FormulaProducto obtenerFormula(DetalleFormula detalle) {
        if (detalle.getFormula() == null || detalle.getFormula().getId() == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "Debe indicar la fórmula asociada al detalle");
        }

        return formulaRepository.findById(detalle.getFormula().getId())
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Fórmula no encontrada"));
    }

    private void verificarFormulaEditable(FormulaProducto formula) {
        EstadoFormula estado = formula != null ? formula.getEstado() : null;
        if (EstadoFormula.BORRADOR.equals(estado) || EstadoFormula.EN_REVISION.equals(estado)) {
            return;
        }

        String estadoNombre = estado != null ? estado.name() : "DESCONOCIDO";
        throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                "La fórmula en estado " + estadoNombre + " no permite modificar sus detalles");
    }
}
