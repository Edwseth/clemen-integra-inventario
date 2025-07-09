package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioFiltroDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final MovimientoInventarioMapper mapper;

    @Resource
    private final EntityManager entityManager;

    @Transactional
    @Override
    public MovimientoInventarioResponseDTO registrarMovimiento(MovimientoInventarioDTO dto) {
        MovimientoInventario movimiento = mapper.toEntity(dto);

        // 1. Cargar entidades principales
        Producto producto = productoRepository.findById(dto.productoId().longValue())
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));

        Almacen almacenOrigen = dto.almacenOrigenId() != null
                ? entityManager.getReference(Almacen.class, dto.almacenOrigenId()) : null;

        Almacen almacenDestino = dto.almacenDestinoId() != null
                ? entityManager.getReference(Almacen.class, dto.almacenDestinoId()) : null;

        Usuario usuario = dto.usuarioId() != null
                ? entityManager.getReference(Usuario.class, dto.usuarioId()) : null;

        TipoMovimiento tipoMovimiento = dto.tipoMovimiento();

        // Detección automática de devolución interna
        boolean deteccionDevolucionInterna = false;
        if (tipoMovimiento == TipoMovimiento.TRANSFERENCIA
                && almacenOrigen != null && almacenDestino != null) {
            deteccionDevolucionInterna = esDevolucionInterna(producto, almacenOrigen, almacenDestino);
        }

        // 2. Validaciones generales
        if (tipoMovimiento == TipoMovimiento.RECEPCION && almacenDestino == null) {
            throw new IllegalArgumentException("El almacén destino es obligatorio para la recepción.");
        }

        if (tipoMovimiento == TipoMovimiento.TRANSFERENCIA) {
            if (almacenOrigen == null || almacenDestino == null) {
                throw new IllegalArgumentException("En una transferencia se requieren almacén origen y destino.");
            }
            if (almacenOrigen.getId().equals(almacenDestino.getId())) {
                throw new IllegalArgumentException("El almacén origen y destino no pueden ser iguales.");
            }
        }

        LoteProducto lote;
        BigDecimal cantidad = dto.cantidad();

        // 3. Gestión de lote
        if (tipoMovimiento == TipoMovimiento.RECEPCION) {
            lote = LoteProducto.builder()
                    .codigoLote(dto.codigoLote())
                    .fechaFabricacion(LocalDate.now())
                    .fechaVencimiento(dto.fechaVencimiento())
                    .fechaLiberacion(producto.getRequiereInspeccion() ? null : LocalDate.now())
                    .estado(obtenerEstadoInicial(producto))
                    .producto(producto)
                    .almacen(almacenDestino)
                    .usuarioLiberador(producto.getRequiereInspeccion() ? null : usuario)
                    .stockLote(cantidad)
                    .build();

            loteProductoRepository.save(lote);
        } else {
            if (dto.loteProductoId() == null) {
                throw new IllegalArgumentException("Debe especificar el ID del lote para este tipo de movimiento.");
            }

            LoteProducto loteOrigen = loteProductoRepository.findById(dto.loteProductoId())
                    .orElseThrow(() -> new NoSuchElementException("Lote no encontrado"));

            if (loteOrigen.getEstado() == EstadoLote.EN_CUARENTENA || loteOrigen.getEstado() == EstadoLote.RETENIDO) {
                throw new IllegalStateException("No se puede mover: el lote está en cuarentena o retenido");
            }

            if (loteOrigen.getEstado() == EstadoLote.VENCIDO) {
                if (dto.motivoMovimientoId() == null) {
                    throw new IllegalStateException("No se puede mover: el lote está vencido");
                }
                MotivoMovimiento motivo = entityManager.getReference(MotivoMovimiento.class, dto.motivoMovimientoId());
                if (motivo.getMotivo() != ClasificacionMovimientoInventario.SALIDA_VENCIDO) {
                    throw new IllegalStateException("No se puede mover: el lote está vencido");
                }
            }

            boolean esDevolucionInterna = (tipoMovimiento == TipoMovimiento.DEVOLUCION
                    && almacenOrigen != null && almacenDestino != null)
                    || deteccionDevolucionInterna;

            if (tipoMovimiento == TipoMovimiento.TRANSFERENCIA || esDevolucionInterna) {
                if (!loteOrigen.getAlmacen().getId().equals(almacenOrigen.getId())) {
                    throw new IllegalStateException("El lote no pertenece al almacén origen indicado.");
                }
                if (loteOrigen.getStockLote().compareTo(cantidad) < 0) {
                    throw new IllegalStateException("Stock insuficiente en el lote para transferir.");
                }

                // Descontar del lote origen
                loteOrigen.setStockLote(loteOrigen.getStockLote().subtract(cantidad));
                loteProductoRepository.save(loteOrigen);

                // Buscar o crear lote destino
                Optional<LoteProducto> destinoExistente = loteProductoRepository
                        .findByCodigoLoteAndProductoIdAndAlmacenId(
                                loteOrigen.getCodigoLote(),
                                producto.getId(),
                                almacenDestino.getId());

                if (destinoExistente.isPresent()) {
                    lote = destinoExistente.get();
                    lote.setStockLote(lote.getStockLote().add(cantidad));
                } else {
                    lote = LoteProducto.builder()
                            .producto(producto)
                            .codigoLote(loteOrigen.getCodigoLote())
                            .fechaFabricacion(loteOrigen.getFechaFabricacion())
                            .fechaVencimiento(loteOrigen.getFechaVencimiento())
                            .estado(loteOrigen.getEstado())
                            .almacen(almacenDestino)
                            .stockLote(cantidad)
                            .build();
                }

                loteProductoRepository.save(lote);
            } else {
                lote = loteOrigen;
            }
        }

        // 4. Orden de compra detalle (si aplica)
        OrdenCompraDetalle ordenCompraDetalle = dto.ordenCompraDetalleId() != null
                ? entityManager.getReference(OrdenCompraDetalle.class, dto.ordenCompraDetalleId()) : null;

        if (ordenCompraDetalle != null) {
            BigDecimal recibida = Optional.ofNullable(ordenCompraDetalle.getCantidadRecibida()).orElse(BigDecimal.ZERO);
            BigDecimal solicitada = Optional.ofNullable(ordenCompraDetalle.getCantidad()).orElse(BigDecimal.ZERO);
            BigDecimal nuevaCantidad = recibida.add(cantidad);

            if (nuevaCantidad.compareTo(solicitada) > 0) {
                throw new IllegalStateException("La cantidad recibida excede la solicitada en la orden.");
            }

            ordenCompraDetalle.setCantidadRecibida(nuevaCantidad);
            entityManager.merge(ordenCompraDetalle);
        }

        // 5. Stock total del producto (excepto transferencia)
        switch (tipoMovimiento) {
            case ENTRADA, RECEPCION, AJUSTE ->
                    producto.setStockActual(Optional.ofNullable(producto.getStockActual()).orElse(BigDecimal.ZERO).add(cantidad));
            case DEVOLUCION -> {
                // Si es devolución entre almacenes no afecta el stock global
                if (!(almacenOrigen != null && almacenDestino != null)) {
                    producto.setStockActual(Optional.ofNullable(producto.getStockActual()).orElse(BigDecimal.ZERO).add(cantidad));
                }
            }
            case SALIDA ->
                    producto.setStockActual(Optional.ofNullable(producto.getStockActual()).orElse(BigDecimal.ZERO).subtract(cantidad));
            case TRANSFERENCIA -> {
                // Ya manejado lote a lote
            }
            default -> throw new IllegalArgumentException("Tipo de movimiento no soportado: " + tipoMovimiento);
        }

        productoRepository.save(producto);

        // 6. Asociar entidades al movimiento
        movimiento.setProducto(producto);
        movimiento.setLote(lote);
        movimiento.setAlmacenOrigen(almacenOrigen);
        movimiento.setAlmacenDestino(almacenDestino);
        movimiento.setProveedor(dto.proveedorId() != null
                ? entityManager.getReference(Proveedor.class, dto.proveedorId()) : null);
        movimiento.setOrdenCompra(dto.ordenCompraId() != null
                ? entityManager.getReference(OrdenCompra.class, dto.ordenCompraId()) : null);
        movimiento.setOrdenCompraDetalle(ordenCompraDetalle);
        movimiento.setMotivoMovimiento(dto.motivoMovimientoId() != null
                ? entityManager.getReference(MotivoMovimiento.class, dto.motivoMovimientoId()) : null);
        movimiento.setTipoMovimientoDetalle(dto.tipoMovimientoDetalleId() != null
                ? entityManager.getReference(TipoMovimientoDetalle.class, dto.tipoMovimientoDetalleId()) : null);
        movimiento.setRegistradoPor(usuario);

        MovimientoInventario guardado = repository.save(movimiento);
        return mapper.toResponseDTO(guardado);
    }

    @Override
    public List<MovimientoInventarioResponseDTO> listarTodos() {
        List<MovimientoInventario> movimientos = repository.findAll(
                Sort.by(Sort.Direction.DESC, "fechaIngreso")
        );
        return movimientos.stream()
                .filter(Objects::nonNull)
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
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

    @Override
    public Workbook generarReporteMovimientosExcel() {
        List<MovimientoInventario> movimientos = repository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Movimientos Inventario");

        // Cabecera
        String[] encabezados = {
                "ID", "Fecha", "Tipo Movimiento", "Producto", "SKU", "Cantidad", "Unidad Medida",
                "Lote", "Almacén", "Proveedor", "Orden Compra", "Motivo", "Detalle Tipo Movimiento", "Usuario"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < encabezados.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(encabezados[i]);
        }

        // Cuerpo
        int fila = 1;
        for (MovimientoInventario mov : movimientos) {
            Row row = sheet.createRow(fila++);

            row.createCell(0).setCellValue(mov.getId());
            row.createCell(1).setCellValue(mov.getFechaIngreso() != null ? mov.getFechaIngreso().toString() : "");
            row.createCell(2).setCellValue(mov.getTipoMovimiento().name());
            String nombreProducto = mov.getProducto() != null ? mov.getProducto().getNombre() : "";
            String codigoSku = mov.getProducto() != null ? mov.getProducto().getCodigoSku() : "";
            String unidad = (mov.getProducto() != null && mov.getProducto().getUnidadMedida() != null)
                    ? mov.getProducto().getUnidadMedida().getNombre() : "";

            row.createCell(3).setCellValue(nombreProducto);
            row.createCell(4).setCellValue(codigoSku);
            row.createCell(5).setCellValue(mov.getCantidad().doubleValue());
            row.createCell(6).setCellValue(unidad);
            row.createCell(7).setCellValue(mov.getLote() != null ? mov.getLote().getCodigoLote() : "");
            String nombreAlmacen = mov.getAlmacenDestino() != null
                    ? mov.getAlmacenDestino().getNombre()
                    : (mov.getAlmacenOrigen() != null ? mov.getAlmacenOrigen().getNombre() : "");
            row.createCell(8).setCellValue(nombreAlmacen);
            row.createCell(9).setCellValue(mov.getProveedor() != null ? mov.getProveedor().getNombre() : "");
            row.createCell(10).setCellValue(mov.getOrdenCompra() != null ? mov.getOrdenCompra().getId().toString() : "");
            row.createCell(11).setCellValue(mov.getMotivoMovimiento() != null ? mov.getMotivoMovimiento().getDescripcion() : "");
            row.createCell(12).setCellValue(mov.getTipoMovimientoDetalle() != null ? mov.getTipoMovimientoDetalle().getDescripcion() : "");
            row.createCell(13).setCellValue(mov.getRegistradoPor() != null ? mov.getRegistradoPor().getNombreCompleto() : "");
        }

        // Autosize columnas
        for (int i = 0; i < encabezados.length; i++) {
            sheet.autoSizeColumn(i);
        }

        return workbook;
    }

    public ByteArrayInputStream exportarMovimientosAExcel(List<MovimientoInventario> movimientos) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Movimientos");
            Row header = sheet.createRow(0);
            String[] columnas = {"ID", "Producto", "Cantidad", "Tipo Movimiento", "Fecha"};
            for (int i = 0; i < columnas.length; i++) {
                header.createCell(i).setCellValue(columnas[i]);
            }

            int rowNum = 1;
            for (MovimientoInventario mov : movimientos) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(mov.getId());
                String nombreProducto = mov.getProducto() != null ? mov.getProducto().getNombre() : "";
                row.createCell(1).setCellValue(nombreProducto);
                row.createCell(2).setCellValue(mov.getCantidad().doubleValue());
                row.createCell(3).setCellValue(mov.getTipoMovimiento().name());
                row.createCell(4).setCellValue(mov.getFechaIngreso().toString());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new IllegalStateException("Error generando el archivo Excel", e);
        }
    }

    private EstadoLote obtenerEstadoInicial(Producto producto) {
        return producto.getRequiereInspeccion()
                ? EstadoLote.EN_CUARENTENA
                : EstadoLote.DISPONIBLE;
    }

    /**
     * Detecta si un movimiento marcado como transferencia corresponde en realidad
     * a una devolución interna entre bodegas.
     *
     * @param producto       Producto en movimiento
     * @param almacenOrigen  almacén desde donde se mueve
     * @param almacenDestino almacén hacia donde se mueve
     * @return {@code true} si se cumplen las reglas de devolución interna
     */
    private boolean esDevolucionInterna(Producto producto, Almacen almacenOrigen, Almacen almacenDestino) {
        if (producto == null || producto.getCategoriaProducto() == null
                || almacenOrigen == null || almacenDestino == null) {
            return false;
        }

        var tipo = producto.getCategoriaProducto().getTipo();
        boolean categoriaPermitida = tipo == TipoCategoria.MATERIA_PRIMA || tipo == TipoCategoria.MATERIAL_EMPAQUE;

        boolean origenPreBodega = almacenOrigen.getNombre() != null
                && almacenOrigen.getNombre().equalsIgnoreCase("Pre-Bodega Producción");

        boolean destinoDiferente = almacenDestino.getNombre() != null
                && !almacenDestino.getNombre().equalsIgnoreCase("Pre-Bodega Producción");

        return categoriaPermitida && origenPreBodega && destinoDiferente;
    }

}
