package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.produccion.dto.InsumoFaltanteDTO;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.mapper.OrdenProduccionMapper;
import com.willyes.clemenintegra.produccion.mapper.ProduccionMapper;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.RoundingMode;
import java.time.LocalDateTime;

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
    public ResultadoValidacionOrdenDTO guardarConValidacionStock(OrdenProduccion orden) {
        Long productoId = orden.getProducto().getId().longValue();

        FormulaProducto formula = formulaProductoRepository
                .findByProductoIdAndEstadoAndActivoTrue(productoId, EstadoFormula.APROBADA)
                .orElseThrow(() -> new IllegalArgumentException("No existe una fórmula activa y aprobada para el producto"));

        Map<Long, BigDecimal> cantidadesEscaladas = new HashMap<>();
        List<InsumoFaltanteDTO> faltantes = new ArrayList<>();
        boolean stockSuficiente = true;
        Integer maxProducible = null;

        BigDecimal cantidadProgramada = BigDecimal.valueOf(orden.getCantidadProgramada());

        for (DetalleFormula insumo : formula.getDetalles()) {
            Long insumoId = insumo.getInsumo().getId().longValue();
            Producto productoInsumo = productoRepository.findById(insumoId)
                    .orElseThrow(() -> new IllegalArgumentException("Insumo no encontrado: ID " + insumoId));

            BigDecimal cantidadRequerida = insumo.getCantidadNecesaria().multiply(cantidadProgramada);
            cantidadesEscaladas.put(insumoId, cantidadRequerida);

            BigDecimal stockActual = productoInsumo.getStockActual();

            int producibleConEste = 0;
            if (insumo.getCantidadNecesaria().compareTo(BigDecimal.ZERO) > 0) {
                producibleConEste = stockActual.divide(insumo.getCantidadNecesaria(), 0, RoundingMode.DOWN).intValue();
            }
            if (maxProducible == null || producibleConEste < maxProducible) {
                maxProducible = producibleConEste;
            }

            if (stockActual.compareTo(cantidadRequerida) < 0) {
                stockSuficiente = false;
                faltantes.add(InsumoFaltanteDTO.builder()
                        .productoId(insumoId)
                        .nombre(productoInsumo.getNombre())
                        .requerido(cantidadRequerida)
                        .disponible(stockActual)
                        .build());
            }
        }

        if (!stockSuficiente) {
            return ResultadoValidacionOrdenDTO.builder()
                    .esValida(false)
                    .mensaje("Stock insuficiente para algunos insumos")
                    .unidadesMaximasProducibles(maxProducible)
                    .insumosFaltantes(faltantes)
                    .build();
        }

        OrdenProduccion guardada = repository.save(orden);

        for (DetalleFormula insumo : formula.getDetalles()) {
            Long insumoId = insumo.getInsumo().getId().longValue();
            BigDecimal cantidad = cantidadesEscaladas.get(insumoId);
            SolicitudMovimientoRequestDTO req = SolicitudMovimientoRequestDTO.builder()
                    .tipoMovimiento(TipoMovimiento.SALIDA)
                    .productoId(insumoId)
                    .cantidad(cantidad)
                    .ordenProduccionId(guardada.getId())
                    .usuarioSolicitanteId(guardada.getResponsable().getId())
                    .build();
            solicitudMovimientoService.registrarSolicitud(req);
        }

        OrdenProduccionResponseDTO ordenResp = ProduccionMapper.toResponse(guardada);

        return ResultadoValidacionOrdenDTO.builder()
                .esValida(true)
                .mensaje("Orden de producción creada correctamente")
                .orden(ordenResp)
                .build();
    }

    public ResultadoValidacionOrdenDTO crearOrden(OrdenProduccionRequestDTO dto) {
        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        Usuario responsable = usuarioRepository.findById(dto.getResponsableId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        OrdenProduccion orden = ordenProduccionMapper.toEntity(dto, producto, responsable);
        orden.setFechaInicio(LocalDateTime.now());
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
