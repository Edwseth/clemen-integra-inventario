package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.mapper.OrdenProduccionMapper;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoRequestDTO;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import com.willyes.clemenintegra.shared.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrdenProduccionServiceImpl implements OrdenProduccionService {

    private final FormulaProductoRepository formulaProductoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudMovimientoService solicitudMovimientoService;
    private final OrdenProduccionRepository repository;
    private final OrdenProduccionMapper ordenProduccionMapper;


    @Transactional
    public OrdenProduccion guardarConValidacionStock(OrdenProduccion orden) {
        Long productoId = orden.getProducto().getId().longValue();

        // 1. Buscar fórmula asociada al producto
        FormulaProducto formula = formulaProductoRepository.findByProductoId(productoId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró una fórmula asociada al producto"));

        Map<Long, Producto> insumosMap = new HashMap<>();

        // 2. Verificar stock suficiente para cada insumo
        for (DetalleFormula insumo : formula.getDetalles()) {
            Long insumoId = insumo.getInsumo().getId().longValue();
            Producto producto = productoRepository.findById(insumoId)
                    .orElseThrow(() -> new IllegalArgumentException("Insumo no encontrado: ID " + insumoId));

            BigDecimal stockActual = producto.getStockActual();
            BigDecimal cantidadNecesaria = insumo.getCantidadNecesaria();

            if (stockActual.compareTo(cantidadNecesaria) < 0) {
                throw new IllegalStateException("Stock insuficiente para el insumo: " + producto.getNombre());
            }

            insumosMap.put(insumoId, producto);
        }

        // 3. Guardar la orden
        OrdenProduccion guardada = repository.save(orden);

        // 4. Registrar solicitudes de movimiento para cada insumo
        for (DetalleFormula insumo : formula.getDetalles()) {
            SolicitudMovimientoRequestDTO req = SolicitudMovimientoRequestDTO.builder()
                    .tipoMovimiento(TipoMovimiento.SALIDA)
                    .productoId(insumo.getInsumo().getId().longValue())
                    .cantidad(insumo.getCantidadNecesaria())
                    .ordenProduccionId(guardada.getId())
                    .usuarioSolicitanteId(guardada.getResponsable().getId())
                    .build();
            solicitudMovimientoService.registrarSolicitud(req);
        }

        return guardada;
    }

    public OrdenProduccion crearOrden(OrdenProduccionRequestDTO dto) {
        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        Usuario responsable = usuarioRepository.findById(dto.getResponsableId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        OrdenProduccion orden = ordenProduccionMapper.toEntity(dto, producto, responsable);
        return guardarConValidacionStock(orden);
    }


    public List<OrdenProduccion> listarTodas() {
        return repository.findAll();
    }

    public Optional<OrdenProduccion> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
