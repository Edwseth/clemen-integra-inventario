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
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.OrdenCompraService;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MovimientoInventarioServiceImpl implements MovimientoInventarioService {

    private static final Logger log = LoggerFactory.getLogger(MovimientoInventarioServiceImpl.class);
    private final AlmacenRepository almacenRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final OrdenCompraService ordenCompraService;
    private final LoteProductoRepository loteProductoRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    private final MovimientoInventarioRepository repository;
    private final MovimientoInventarioMapper mapper;
    private final UsuarioService usuarioService;

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
                ? entityManager.getReference(Usuario.class, dto.usuarioId())
                : usuarioService.obtenerUsuarioAutenticado();

        TipoMovimiento tipoMovimiento = dto.tipoMovimiento();

        // Detección automática de devolución interna
        boolean devolucionInterna = false;
        if (tipoMovimiento == TipoMovimiento.TRANSFERENCIA && almacenOrigen != null && almacenDestino != null) {
            devolucionInterna = esDevolucionInterna(producto, almacenOrigen, almacenDestino);
            if (devolucionInterna) {
                tipoMovimiento = TipoMovimiento.DEVOLUCION;
                movimiento.setTipoMovimiento(TipoMovimiento.DEVOLUCION);
                log.debug("Movimiento detectado como devolución interna");
            }
        }

        validarParametros(tipoMovimiento, almacenOrigen, almacenDestino);

        MotivoMovimiento motivoMovimiento = null;
        if (dto.motivoMovimientoId() != null) {
            motivoMovimiento = motivoMovimientoRepository.findById(dto.motivoMovimientoId())
                    .orElseThrow(() -> new NoSuchElementException("Motivo no encontrado"));
        }

        OrdenCompra orden = null;
        if (tipoMovimiento == TipoMovimiento.RECEPCION
                && motivoMovimiento != null
                && motivoMovimiento.getMotivo() == ClasificacionMovimientoInventario.RECEPCION_COMPRA) {
            if (dto.ordenCompraId() == null) {
                throw new IllegalArgumentException("Se requiere una orden de compra para la recepción de compra");
            }
            orden = ordenCompraRepository.findById(dto.ordenCompraId().longValue())
                    .orElseThrow(() -> new NoSuchElementException("Orden de compra no encontrada"));
            if (orden.getEstado() == EstadoOrdenCompra.CERRADA
                    || orden.getEstado() == EstadoOrdenCompra.CANCELADA
                    || orden.getEstado() == EstadoOrdenCompra.RECHAZADA) {
                throw new IllegalStateException("La orden de compra no se encuentra activa");
            }
            boolean incluido = orden.getDetalles().stream()
                    .anyMatch(d -> d.getProducto() != null &&
                            d.getProducto().getId().equals(producto.getId()));
            if (!incluido) {
                throw new IllegalArgumentException("El producto no pertenece a la orden de compra");
            }
        }

        BigDecimal cantidad = dto.cantidad();
        LoteProducto lote;

        if (tipoMovimiento == TipoMovimiento.RECEPCION) {
            lote = crearLoteRecepcion(dto, producto, almacenDestino, usuario, cantidad);
        } else {
            lote = procesarMovimientoConLoteExistente(dto, tipoMovimiento, almacenOrigen,
                    almacenDestino, producto, cantidad, devolucionInterna);
        }

        OrdenCompraDetalle ordenCompraDetalle = actualizarOrdenCompraDetalle(dto, cantidad);
        if (orden != null) {
            ordenCompraService.evaluarYActualizarEstado(orden);
        }

        actualizarStockProducto(producto, tipoMovimiento, cantidad, devolucionInterna);
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
    public Page<MovimientoInventarioResponseDTO> listarTodos(Pageable pageable) {
        Sort sort = pageable.getSort().isEmpty()
                ? Sort.by(Sort.Direction.DESC, "fechaIngreso")
                : pageable.getSort();

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<MovimientoInventario> movimientos = repository.findAll(sortedPageable);
        return movimientos.map(mapper::toResponseDTO);
    }

    @Override
    public Page<MovimientoInventario> consultarMovimientosConFiltros(
            MovimientoInventarioFiltroDTO filtro, Pageable pageable) {
        return repository.filtrarMovimientos(
                filtro.productoId(),
                filtro.almacenId(),
                filtro.tipoMovimiento(),
                filtro.clasificacion(),
                filtro.fechaInicio(),
                filtro.fechaFin(),
                pageable
        );
    }

    @Override
    public List<MovimientoInventarioResponseDTO> consultarMovimientos(MovimientoInventarioFiltroDTO filtro) {
        List<MovimientoInventario> lista = repository.buscarMovimientos(
                filtro.productoId(),
                filtro.almacenId(),
                filtro.tipoMovimiento(),
                filtro.clasificacion(),
                filtro.fechaInicio(),
                filtro.fechaFin()
        );
        return lista.stream().map(mapper::toResponseDTO).toList();
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

    private void validarParametros(TipoMovimiento tipo, Almacen origen, Almacen destino) {
        if (tipo == TipoMovimiento.RECEPCION && destino == null) {
            throw new IllegalArgumentException("El almacén destino es obligatorio para la recepción.");
        }

        if (tipo == TipoMovimiento.TRANSFERENCIA) {
            if (origen == null || destino == null) {
                throw new IllegalArgumentException("En una transferencia se requieren almacén origen y destino.");
            }
            if (Objects.equals(origen.getId(), destino.getId())) {
                throw new IllegalArgumentException("El almacén origen y destino no pueden ser iguales.");
            }
        }
    }

    private LoteProducto crearLoteRecepcion(MovimientoInventarioDTO dto, Producto producto,
                                            Almacen destino, Usuario usuario, BigDecimal cantidad) {
        if (dto.loteProductoId() != null) {
            LoteProducto existente = loteProductoRepository.findById(dto.loteProductoId())
                    .orElseThrow(() -> new NoSuchElementException("Lote no encontrado"));
            BigDecimal nuevo = Optional.ofNullable(existente.getStockLote()).orElse(BigDecimal.ZERO).add(cantidad);
            existente.setStockLote(nuevo);
            existente.setAlmacen(destino);
            return loteProductoRepository.save(existente);
        }

        LoteProducto lote = LoteProducto.builder()
                .codigoLote(dto.codigoLote())
                .fechaFabricacion(LocalDate.now())
                .fechaVencimiento(dto.fechaVencimiento())
                .fechaLiberacion(producto.getTipoAnalisisCalidad() == TipoAnalisisCalidad.NINGUNO ? LocalDate.now() : null)
                .estado(obtenerEstadoInicial(producto))
                .producto(producto)
                .almacen(destino)
                .usuarioLiberador(producto.getTipoAnalisisCalidad() == TipoAnalisisCalidad.NINGUNO ? usuario : null)
                .stockLote(cantidad)
                .build();
        return loteProductoRepository.save(lote);
    }

    private LoteProducto procesarMovimientoConLoteExistente(MovimientoInventarioDTO dto,
                                                            TipoMovimiento tipo,
                                                            Almacen origen,
                                                            Almacen destino,
                                                            Producto producto,
                                                            BigDecimal cantidad,
                                                            boolean devolucionInterna) {
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
            if (motivo.getMotivo() != ClasificacionMovimientoInventario.AJUSTE_NEGATIVO) {
                throw new IllegalStateException("No se puede mover: el lote está vencido");
            }
        }

        // Declaración necesaria (si aún no está)
        Almacen almacenOrigen = entityManager.getReference(Almacen.class, dto.almacenOrigenId());

        // Determinar si es una devolución interna
        boolean esDevolucionInterna =
                dto.tipoMovimiento() == TipoMovimiento.DEVOLUCION &&
                        dto.clasificacionMovimientoInventario() == ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION;

        // Validar almacén del lote origen
        if (!esDevolucionInterna && !loteOrigen.getAlmacen().getId().equals(almacenOrigen.getId())) {
            log.error("Validación fallida: lote está en almacén {}, pero se recibió almacenOrigenId {}",
                    loteOrigen.getAlmacen().getId(), almacenOrigen.getId());
            throw new IllegalStateException("El lote no pertenece al almacén origen indicado.");
        }

        if (loteOrigen.getStockLote().compareTo(cantidad) < 0) {
            throw new IllegalStateException("Stock insuficiente en el lote para transferir.");
        }

        if (tipo == TipoMovimiento.SALIDA) {
            loteOrigen.setStockLote(loteOrigen.getStockLote().subtract(cantidad));
            return loteProductoRepository.save(loteOrigen);
        }

        if (tipo == TipoMovimiento.TRANSFERENCIA) {
            if (loteOrigen.getEstado() != EstadoLote.DISPONIBLE) {
                throw new IllegalStateException("El lote no está disponible para transferir");
            }
            int cmp = loteOrigen.getStockLote().compareTo(cantidad);
            if (cmp > 0) {
                // Transferencia parcial: mover solo parte del stock
                loteOrigen.setStockLote(loteOrigen.getStockLote().subtract(cantidad));
                loteProductoRepository.save(loteOrigen);

                Optional<LoteProducto> destinoExistente = loteProductoRepository
                        .findByCodigoLoteAndProductoIdAndAlmacenId(
                                loteOrigen.getCodigoLote(),
                                producto.getId(),
                                destino.getId());

                LoteProducto loteDestino = destinoExistente.orElseGet(() -> LoteProducto.builder()
                        .producto(producto)
                        .codigoLote(loteOrigen.getCodigoLote())
                        .fechaFabricacion(loteOrigen.getFechaFabricacion())
                        .fechaVencimiento(loteOrigen.getFechaVencimiento())
                        .estado(loteOrigen.getEstado())
                        .almacen(destino)
                        .stockLote(BigDecimal.ZERO)
                        .build());

                BigDecimal nuevoStock = Optional.ofNullable(loteDestino.getStockLote()).orElse(BigDecimal.ZERO)
                        .add(cantidad);
                loteDestino.setStockLote(nuevoStock);
                return loteProductoRepository.save(loteDestino);
            } else {
                // Transferencia completa: mover el lote sin dividir
                loteOrigen.setAlmacen(destino);
                return loteProductoRepository.save(loteOrigen);
            }
        }

        if (tipo == TipoMovimiento.DEVOLUCION && destino != null) {
            loteOrigen.setStockLote(loteOrigen.getStockLote().subtract(cantidad));
            loteProductoRepository.save(loteOrigen);

            Optional<LoteProducto> destinoExistente = loteProductoRepository
                    .findByCodigoLoteAndProductoIdAndAlmacenId(
                            loteOrigen.getCodigoLote(),
                            producto.getId(),
                            destino.getId());

            LoteProducto loteDestino = destinoExistente.orElseGet(() -> LoteProducto.builder()
                    .producto(producto)
                    .codigoLote(loteOrigen.getCodigoLote())
                    .fechaFabricacion(loteOrigen.getFechaFabricacion())
                    .fechaVencimiento(loteOrigen.getFechaVencimiento())
                    .estado(loteOrigen.getEstado())
                    .almacen(destino)
                    .stockLote(BigDecimal.ZERO)
                    .build());

            BigDecimal nuevoStock = Optional.ofNullable(loteDestino.getStockLote()).orElse(BigDecimal.ZERO).add(cantidad);
            loteDestino.setStockLote(nuevoStock);
            return loteProductoRepository.save(loteDestino);
        }

        return loteOrigen;
    }

    private OrdenCompraDetalle actualizarOrdenCompraDetalle(MovimientoInventarioDTO dto, BigDecimal cantidad) {
        if (dto.ordenCompraDetalleId() == null) {
            return null;
        }
        OrdenCompraDetalle detalle = entityManager.getReference(OrdenCompraDetalle.class, dto.ordenCompraDetalleId());
        BigDecimal recibida = Optional.ofNullable(detalle.getCantidadRecibida()).orElse(BigDecimal.ZERO);
        BigDecimal solicitada = Optional.ofNullable(detalle.getCantidad()).orElse(BigDecimal.ZERO);
        BigDecimal nuevaCantidad = recibida.add(cantidad);
        if (nuevaCantidad.compareTo(solicitada) > 0) {
            throw new IllegalStateException("La cantidad recibida excede la solicitada en la orden.");
        }
        detalle.setCantidadRecibida(nuevaCantidad);
        return entityManager.merge(detalle);
    }

    private void actualizarStockProducto(Producto producto, TipoMovimiento tipo, BigDecimal cantidad,
                                         boolean devolucionInterna) {
        switch (tipo) {
            case ENTRADA, RECEPCION, AJUSTE ->
                    producto.setStockActual(Optional.ofNullable(producto.getStockActual()).orElse(BigDecimal.ZERO).add(cantidad));
            case DEVOLUCION -> {
                if (!devolucionInterna) {
                    producto.setStockActual(Optional.ofNullable(producto.getStockActual()).orElse(BigDecimal.ZERO).add(cantidad));
                }
            }
            case SALIDA ->
                    producto.setStockActual(Optional.ofNullable(producto.getStockActual()).orElse(BigDecimal.ZERO).subtract(cantidad));
            case TRANSFERENCIA -> {
                // No afecta stock global
            }
            default -> throw new IllegalArgumentException("Tipo de movimiento no soportado: " + tipo);
        }
    }

    private EstadoLote obtenerEstadoInicial(Producto producto) {
        return producto.getTipoAnalisisCalidad() == TipoAnalisisCalidad.NINGUNO
                ? EstadoLote.DISPONIBLE
                : EstadoLote.EN_CUARENTENA;
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
        boolean categoriaPermitida = tipo == TipoCategoria.MATERIA_PRIMA
                || tipo == TipoCategoria.MATERIAL_EMPAQUE;

        String nombreOrigen = normalizar(almacenOrigen.getNombre());
        String nombreDestino = normalizar(almacenDestino.getNombre());

        boolean origenPreBodega = "pre-bodega produccion".equals(nombreOrigen);
        boolean destinoDiferente = !"pre-bodega produccion".equals(nombreDestino);

        return categoriaPermitida && origenPreBodega && destinoDiferente;
    }

    /**
     * Normaliza un nombre de almacén ignorando mayúsculas y acentos.
     */
    private String normalizar(String nombre) {
        if (nombre == null) {
            return "";
        }
        String nfd = java.text.Normalizer.normalize(nombre, java.text.Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "").toLowerCase();
    }

}
