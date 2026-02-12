package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.AjusteInventarioRequestDTO;
import com.willyes.clemenintegra.inventario.dto.AjusteInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.mapper.AjusteInventarioMapper;
import com.willyes.clemenintegra.inventario.model.AjusteInventario;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.AjusteInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AjusteInventarioServiceImpl implements AjusteInventarioService {

    private final AjusteInventarioRepository repository;
    private final AjusteInventarioMapper mapper;
    private final ProductoRepository productoRepository;
    private final AlmacenRepository almacenRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final UsuarioService usuarioService;
    private final MovimientoInventarioService movimientoInventarioService;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;


    public Page<AjusteInventarioResponseDTO> listar(Pageable pageable) {
        Page<AjusteInventario> page = repository.findAll(pageable);
        return page.map(mapper::toResponseDTO);
    }

    @Transactional(rollbackFor = Exception.class)
    public AjusteInventarioResponseDTO crear(AjusteInventarioRequestDTO dto) {

        if (dto.getCantidad().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("La cantidad no puede ser cero");
        }

        var producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        var almacen = almacenRepository.findById(dto.getAlmacenId())
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado"));
        var usuario = usuarioService.obtenerUsuarioAutenticado();

        var lote = loteProductoRepository.findByIdForUpdate(dto.getLoteProductoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_ENCONTRADO"));

        if (lote.getProducto() == null || !Objects.equals(lote.getProducto().getId(), producto.getId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_PRODUCTO_INVALIDO");
        }

        if (lote.getAlmacen() == null || !Objects.equals(lote.getAlmacen().getId(), almacen.getId().intValue())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_PERTENECE_ALMACEN_ORIGEN");
        }

        ClasificacionMovimientoInventario clasificacion = dto.getCantidad().compareTo(BigDecimal.ZERO) > 0
                ? ClasificacionMovimientoInventario.AJUSTE_POSITIVO
                : ClasificacionMovimientoInventario.AJUSTE_NEGATIVO;

        Long motivoMovimientoId = motivoMovimientoRepository.findByMotivo(clasificacion)
                .map(m -> m.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "MOTIVO_AJUSTE_NO_CONFIGURADO"));

        Long tipoDetalleId = tipoMovimientoDetalleRepository.findByDescripcion(clasificacion.name())
                .map(t -> t.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "TIPO_DETALLE_AJUSTE_NO_CONFIGURADO"));

        MovimientoInventarioDTO movimientoDto = new MovimientoInventarioDTO(
                null,
                dto.getCantidad().abs(),
                TipoMovimiento.AJUSTE,
                clasificacion,
                dto.getMotivo(),
                dto.getObservaciones(),
                null,
                null,
                null,
                producto.getId(),
                lote.getId(),
                clasificacion == ClasificacionMovimientoInventario.AJUSTE_NEGATIVO ? almacen.getId() : null,
                clasificacion == ClasificacionMovimientoInventario.AJUSTE_POSITIVO ? almacen.getId() : null,
                null,
                null,
                motivoMovimientoId,
                tipoDetalleId,
                null,
                usuario.getId(),
                null,
                null,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                null,
                null,
                null
        );

        movimientoInventarioService.registrarMovimiento(movimientoDto);

        var entity = mapper.toEntity(dto, producto, almacen, usuario);
        entity.setFecha(LocalDateTime.now());
        var guardado = repository.save(entity);
        return mapper.toResponseDTO(guardado);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
