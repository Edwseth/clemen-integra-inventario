package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioFiltroDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.inventario.mapper.TipoMovimientoMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento.*;

@Service
@RequiredArgsConstructor
public class MovimientoInventarioServiceImpl implements MovimientoInventarioService {

    private final AlmacenRepository almacenRepository;
    private final ProveedorRepository proveedorRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    private final MovimientoInventarioRepository repository;

    @Transactional
    @Override
    public MovimientoInventarioDTO registrarMovimiento(MovimientoInventarioDTO dto) {
        // Sólo hago el find de las entidades restantes y persisto:
        Producto producto = new Producto(); producto.setId(dto.productoId());
        LoteProducto lote   = new LoteProducto(); lote.setId(dto.loteProductoId());
        Almacen almacen      = almacenRepository.findById(dto.almacenId())
                .orElseThrow(() -> new NoSuchElementException("Almacén no encontrado"));
        Proveedor prov       = Optional.ofNullable(dto.proveedorId())
                .flatMap(proveedorRepository::findById).orElse(null);
        OrdenCompra oc       = Optional.ofNullable(dto.ordenCompraId())
                .flatMap(ordenCompraRepository::findById).orElse(null);
        MotivoMovimiento mvTo= Optional.ofNullable(dto.motivoMovimientoId())
                .flatMap(motivoMovimientoRepository::findById).orElse(null);
        TipoMovimientoDetalle det = tipoMovimientoDetalleRepository.findById(dto.tipoMovimientoDetalleId())
                .orElseThrow(() -> new NoSuchElementException("Detalle no encontrado"));

        MovimientoInventario ent = MovimientoInventarioMapper.toEntity(dto);
        ent.setProducto(producto);
        ent.setLote(lote);
        ent.setAlmacen(almacen);
        ent.setProveedor(prov);
        ent.setOrdenCompra(oc);
        ent.setMotivoMovimiento(mvTo);
        ent.setTipoMovimientoDetalle(det);

        MovimientoInventario saved = repository.save(ent);
        return MovimientoInventarioMapper.toDTO(saved);
    }


    public Page<MovimientoInventario> consultarMovimientosConFiltros(MovimientoInventarioFiltroDTO filtro, Pageable pageable) {
        return repository.filtrarMovimientos(
                filtro.productoId(),
                filtro.almacenId(),
                filtro.tipoMovimiento(),
                filtro.fechaInicio(),
                filtro.fechaFin(),
                pageable
        );
    }
}


