package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.dto.FormulaActivaProduccionDTO;
import com.willyes.clemenintegra.bom.dto.FormulaProductoDetalleDTO;
import com.willyes.clemenintegra.bom.dto.FormulaProductoResponse;
import com.willyes.clemenintegra.bom.dto.FormulaProductoSelectorDTO;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FormulaProductoService {
    List<FormulaProducto> listarTodas();
    Page<FormulaProductoSelectorDTO> listarResumen(EstadoFormula estado, String producto, Pageable pageable);
    Optional<FormulaProducto> buscarPorId(Long id);
    Optional<FormulaProductoDetalleDTO> buscarDetallePorId(Long id);
    FormulaProducto guardar(FormulaProducto formula);
    void eliminar(Long id);

    FormulaProductoResponse obtenerFormulaActivaPorProducto(Long productoId, BigDecimal cantidad);

    FormulaActivaProduccionDTO obtenerFormulaActivaProduccion(Long productoId);

    FormulaProducto cambiarEstado(Long formulaId, EstadoFormula nuevoEstado, Long usuarioId);

    FormulaProducto clonarFormula(Long formulaId, Long usuarioId);
}
