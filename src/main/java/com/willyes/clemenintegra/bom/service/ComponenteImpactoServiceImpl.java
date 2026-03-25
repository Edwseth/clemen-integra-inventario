package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.dto.ComponenteImpactoResponseDTO;
import com.willyes.clemenintegra.bom.dto.FormulaImpactoReferenciaDTO;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.DetalleFormulaRepository;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComponenteImpactoServiceImpl implements ComponenteImpactoService {

    private final DetalleFormulaRepository detalleFormulaRepository;
    private final ProductoRepository productoRepository;

    @Override
    @Transactional(readOnly = true)
    public ComponenteImpactoResponseDTO obtenerImpactoPorProductoId(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new CustomBusinessException(
                        ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Producto no encontrado con ID: " + productoId));

        List<FormulaImpactoReferenciaDTO> activas = detalleFormulaRepository
                .findImpactoActivasByInsumoId(productoId, EstadoFormula.APROBADA);
        List<FormulaImpactoReferenciaDTO> historicas = detalleFormulaRepository
                .findImpactoHistoricasByInsumoId(productoId, EstadoFormula.APROBADA);

        ComponenteImpactoResponseDTO response = new ComponenteImpactoResponseDTO();
        response.producto = mapProducto(producto);
        response.formulasActivas = activas;
        response.formulasHistoricas = historicas;
        response.resumen = mapResumen(activas.size(), historicas.size());
        return response;
    }

    private ComponenteImpactoResponseDTO.ProductoImpactoDTO mapProducto(Producto producto) {
        ComponenteImpactoResponseDTO.ProductoImpactoDTO dto = new ComponenteImpactoResponseDTO.ProductoImpactoDTO();
        dto.id = producto.getId() != null ? producto.getId().longValue() : null;
        dto.sku = producto.getCodigoSku();
        dto.nombre = producto.getNombre();
        dto.activo = producto.isActivo();
        return dto;
    }

    private ComponenteImpactoResponseDTO.ResumenImpactoDTO mapResumen(int activas, int historicas) {
        ComponenteImpactoResponseDTO.ResumenImpactoDTO dto = new ComponenteImpactoResponseDTO.ResumenImpactoDTO();
        dto.referenciasActivas = activas;
        dto.referenciasHistoricas = historicas;
        dto.totalReferencias = activas + historicas;
        dto.puedeInactivarse = activas == 0;
        dto.motivoBloqueo = activas > 0 ? "REFERENCIADO_EN_FORMULA_ACTIVA" : null;
        return dto;
    }
}
