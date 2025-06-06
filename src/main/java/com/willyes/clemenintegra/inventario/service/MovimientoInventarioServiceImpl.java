package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioFiltroDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MovimientoInventarioServiceImpl implements MovimientoInventarioService {

    private final AlmacenRepository almacenRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    private final MovimientoInventarioRepository repository;

    @Resource
    private final EntityManager entityManager;

    @Transactional
    @Override
    public MovimientoInventarioDTO registrarMovimiento(MovimientoInventarioDTO dto) {
        MovimientoInventario movimiento = MovimientoInventarioMapper.toEntity(dto);

        // Recuperar entidades
        Producto producto = productoRepository.findById(dto.productoId())
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));

        LoteProducto lote = loteProductoRepository.findById(dto.loteProductoId())
                .orElseThrow(() -> new NoSuchElementException("Lote no encontrado"));

        Almacen almacen = entityManager.getReference(Almacen.class, dto.almacenId());
        Proveedor proveedor = dto.proveedorId() != null
                ? entityManager.getReference(Proveedor.class, dto.proveedorId()) : null;
        OrdenCompra ordenCompra = dto.ordenCompraId() != null
                ? entityManager.getReference(OrdenCompra.class, dto.ordenCompraId()) : null;
        MotivoMovimiento motivo = dto.motivoMovimientoId() != null
                ? entityManager.getReference(MotivoMovimiento.class, dto.motivoMovimientoId()) : null;
        TipoMovimientoDetalle detalle = dto.tipoMovimientoDetalleId() != null
                ? entityManager.getReference(TipoMovimientoDetalle.class, dto.tipoMovimientoDetalleId()) : null;
        Usuario usuario = dto.usuarioId() != null
                ? entityManager.getReference(Usuario.class, dto.usuarioId()) : null;
        OrdenCompraDetalle ordenCompraDetalle = dto.ordenCompraDetalleId() != null
                ? entityManager.getReference(OrdenCompraDetalle.class, dto.ordenCompraDetalleId()) : null;

        // Validar estado del lote
        if (lote.getEstado() == EstadoLote.EN_CUARENTENA || lote.getEstado() == EstadoLote.RETENIDO) {
            throw new IllegalStateException("No se puede mover: el lote está en cuarentena o retenido");
        }

        if (lote.getEstado() == EstadoLote.VENCIDO) {
            throw new IllegalStateException("No se puede mover: el lote está vencido");
        }

        BigDecimal cantidad = dto.cantidad();
        if (ordenCompraDetalle != null) {
            BigDecimal cantidadRecibida = Optional.ofNullable(ordenCompraDetalle.getCantidadRecibida()).orElse(BigDecimal.ZERO);
            BigDecimal cantidadSolicitada = Optional.ofNullable(ordenCompraDetalle.getCantidad()).orElse(BigDecimal.ZERO);
            BigDecimal nuevaCantidad = cantidadRecibida.add(cantidad);

            if (nuevaCantidad.compareTo(cantidadSolicitada) > 0) {
                throw new IllegalStateException("La cantidad recibida excede la cantidad solicitada en la orden.");
            }

            ordenCompraDetalle.setCantidadRecibida(nuevaCantidad);
            entityManager.merge(ordenCompraDetalle); // Actualizar cantidad recibida
        }

// Actualización de stock (usando BigDecimal)
            switch (dto.tipoMovimiento()) {
            case ENTRADA_PRODUCCION, RECEPCION_COMPRA, AJUSTE_POSITIVO -> {
                producto.setStockActual(
                        Optional.ofNullable(producto.getStockActual()).orElse(BigDecimal.ZERO).add(cantidad)
                );
                lote.setStockLote(
                        Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO).add(cantidad)
                );
            }
            case SALIDA_PRODUCCION, AJUSTE_NEGATIVO, SALIDA_VENCIDO -> {
                producto.setStockActual(
                        Optional.ofNullable(producto.getStockActual()).orElse(BigDecimal.ZERO).subtract(cantidad)
                );
                lote.setStockLote(
                        Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO).subtract(cantidad)
                );
            }
            default -> throw new IllegalArgumentException("Tipo de movimiento no soportado: " + dto.tipoMovimiento());
        }

        productoRepository.save(producto);
        productoRepository.flush(); // ✅ Esto asegura visibilidad del cambio en el test

        loteProductoRepository.save(lote);

        // Asociar entidades al movimiento
        movimiento.setProducto(producto);
        movimiento.setLote(lote);
        movimiento.setAlmacen(almacen);
        movimiento.setProveedor(proveedor);
        movimiento.setOrdenCompra(ordenCompra);
        movimiento.setOrdenCompraDetalle(ordenCompraDetalle);
        movimiento.setMotivoMovimiento(motivo);
        movimiento.setTipoMovimientoDetalle(detalle);
        movimiento.setRegistradoPor(usuario);

        // Guardar movimiento
        MovimientoInventario guardado = repository.save(movimiento);

        return MovimientoInventarioMapper.toDTO(guardado);
    }

    @Override
    public Page<MovimientoInventario> consultarMovimientosConFiltros(
            MovimientoInventarioFiltroDTO filtro, Pageable pageable) {
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
