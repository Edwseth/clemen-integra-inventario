package com.willyes.clemenintegra.inventario.service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.PicklistPtEstado;
import com.willyes.clemenintegra.inventario.model.enums.PicklistPtModoAsignacion;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.PicklistPtRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PicklistPtServiceImpl implements PicklistPtService {

    private static final EnumSet<EstadoLote> ESTADOS_FEFO = EnumSet.of(EstadoLote.DISPONIBLE, EstadoLote.LIBERADO);

    private final PicklistPtRepository picklistRepository;
    private final ProductoRepository productoRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final InventoryCatalogResolver catalogResolver;
    private final MovimientoInventarioService movimientoInventarioService;
    private final LoteCalidadValidator loteCalidadValidator;
    private final UsuarioService usuarioService;

    @Override
    @Transactional
    public PicklistPtResponse crear(PicklistPtCreateRequest request) {
        validarSolicitud(request);
        Usuario creador = usuarioService.obtenerUsuarioAutenticado();
        Integer almacenPtId = obtenerAlmacenPtRequerido();
        Integer tipoDetalleSalidaPt = obtenerTipoDetalleSalidaPt();
        String codigo = generarCodigoPicklist();

        PicklistPt picklist = PicklistPt.builder()
                .codigo(codigo)
                .clienteNombre(request.clienteNombre())
                .minVidaUtilDias(request.minVidaUtilDias())
                .estado(PicklistPtEstado.BORRADOR)
                .almacenPtId(almacenPtId)
                .tipoMovimientoDetalleId(tipoDetalleSalidaPt)
                .docReferencia(request.docReferencia())
                .observaciones(request.observaciones())
                .creadoPor(creador)
                .build();

        List<PicklistPtLinea> lineas = construirLineas(picklist, request.lineas());
        picklist.setLineas(lineas);

        List<PicklistPtAsignacion> asignaciones = resolverAsignaciones(picklist, lineas);
        picklist.setAsignaciones(asignaciones);
        picklist.setEstado(PicklistPtEstado.GENERADO);

        PicklistPt guardado = picklistRepository.save(picklist);
        return toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PicklistPtResumenDTO> listar(PicklistPtEstado estado,
                                             String cliente,
                                             LocalDateTime desde,
                                             LocalDateTime hasta,
                                             Pageable pageable) {
        Page<PicklistPt> page = picklistRepository.buscar(estado, cliente, desde, hasta, pageable);
        return page.map(picklist -> new PicklistPtResumenDTO(
                picklist.getId(),
                picklist.getCodigo(),
                picklist.getClienteNombre(),
                picklist.getEstado(),
                picklist.getFechaCreacion()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public PicklistPtResponse obtener(Long id) {
        PicklistPt picklist = picklistRepository.findWithDetallesById(id)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Picklist no encontrado"));
        return toResponse(picklist);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarPdf(Long id) {
        PicklistPt picklist = picklistRepository.findWithDetallesById(id)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Picklist no encontrado"));
        return buildPdf(picklist);
    }

    @Override
    @Transactional
    public PicklistPtResponse confirmar(Long id) {
        PicklistPt picklist = picklistRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Picklist no encontrado"));
        if (picklist.getEstado() != PicklistPtEstado.GENERADO) {
            throw new CustomBusinessException(ApiErrorCode.PICKLIST_ESTADO_INVALIDO,
                    "Solo se puede confirmar un picklist en estado GENERADO");
        }
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        picklist.setEstado(PicklistPtEstado.CONFIRMADO);
        picklist.setConfirmadoPor(usuario);
        picklist.setFechaConfirmacion(LocalDateTime.now());
        return toResponse(picklistRepository.save(picklist));
    }

    @Override
    @Transactional
    public PicklistPtResponse ejecutar(Long id) {
        PicklistPt picklist = picklistRepository.findWithDetallesById(id)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Picklist no encontrado"));
        if (picklist.getEstado() != PicklistPtEstado.CONFIRMADO) {
            throw new CustomBusinessException(ApiErrorCode.PICKLIST_ESTADO_INVALIDO,
                    "Solo se puede ejecutar un picklist en estado CONFIRMADO");
        }

        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        validarAsignacionesParaEjecucion(picklist);

        for (PicklistPtAsignacion asignacion : picklist.getAsignaciones()) {
            MovimientoInventarioDTO movimiento = new MovimientoInventarioDTO(
                    null,
                    asignacion.getCantidadAsignada(),
                    TipoMovimiento.SALIDA,
                    ClasificacionMovimientoInventario.SALIDA_CLIENTE,
                    picklist.getDocReferencia(),
                    picklist.getClienteNombre(),
                    asignacion.getProducto().getId(),
                    asignacion.getLoteProducto().getId(),
                    picklist.getAlmacenPtId(),
                    null,
                    null,
                    null,
                    null,
                    picklist.getTipoMovimientoDetalleId().longValue(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            String idempotencyKey = "PICKLIST_PT:%d:%d".formatted(picklist.getId(), asignacion.getId());
            movimientoInventarioService.registrarMovimiento(movimiento, idempotencyKey);
        }

        picklist.setEstado(PicklistPtEstado.EJECUTADO);
        picklist.setEjecutadoPor(usuario);
        picklist.setFechaEjecucion(LocalDateTime.now());
        return toResponse(picklistRepository.save(picklist));
    }

    @Override
    @Transactional
    public PicklistPtResponse cancelar(Long id) {
        PicklistPt picklist = picklistRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Picklist no encontrado"));
        if (picklist.getEstado() == PicklistPtEstado.EJECUTADO) {
            throw new CustomBusinessException(ApiErrorCode.PICKLIST_ESTADO_INVALIDO,
                    "No se puede cancelar un picklist ejecutado");
        }
        picklist.setEstado(PicklistPtEstado.CANCELADO);
        return toResponse(picklistRepository.save(picklist));
    }

    private Integer obtenerAlmacenPtRequerido() {
        Long almacenPtId = catalogResolver.getAlmacenPtId();
        if (almacenPtId == null || almacenPtId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CONFIG_FALTANTE");
        }
        return almacenPtId.intValue();
    }

    private Integer obtenerTipoDetalleSalidaPt() {
        Long tipoDetalle = catalogResolver.getTipoDetalleSalidaPtId();
        if (tipoDetalle == null || tipoDetalle <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CONFIG_FALTANTE");
        }
        return tipoDetalle.intValue();
    }

    private void validarSolicitud(PicklistPtCreateRequest request) {
        if (request == null || request.lineas() == null || request.lineas().isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.PICKLIST_LINEAS_REQUERIDAS,
                    "Debe incluir al menos una línea");
        }
    }

    private List<PicklistPtLinea> construirLineas(PicklistPt picklist, List<PicklistPtLineaRequest> lineas) {
        List<PicklistPtLinea> resultado = new ArrayList<>();
        for (PicklistPtLineaRequest lineaRequest : lineas) {
            if (lineaRequest.cantidad() == null || lineaRequest.cantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CustomBusinessException(ApiErrorCode.PICKLIST_LINEA_INVALIDA,
                        "La cantidad debe ser mayor a cero");
            }
            Producto producto = productoRepository.findById(lineaRequest.productoId())
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                            "Producto no encontrado"));
            validarProductoTerminado(producto);

            LoteProducto lote = null;
            if (lineaRequest.modoAsignacion() == PicklistPtModoAsignacion.MANUAL_LOTE) {
                if (lineaRequest.loteProductoId() == null) {
                    throw new CustomBusinessException(ApiErrorCode.PICKLIST_LINEA_INVALIDA,
                            "Se requiere lote para modo MANUAL_LOTE");
                }
                lote = loteProductoRepository.findById(lineaRequest.loteProductoId())
                        .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.PICKLIST_LOTE_INVALIDO,
                                "Lote no encontrado"));
            }

            resultado.add(PicklistPtLinea.builder()
                    .picklist(picklist)
                    .producto(producto)
                    .cantidad(lineaRequest.cantidad().setScale(6, RoundingMode.HALF_UP))
                    .modoAsignacion(lineaRequest.modoAsignacion())
                    .loteProducto(lote)
                    .build());
        }
        return resultado;
    }

    private List<PicklistPtAsignacion> resolverAsignaciones(PicklistPt picklist, List<PicklistPtLinea> lineas) {
        List<PicklistPtAsignacion> asignaciones = new ArrayList<>();
        short orden = 1;
        LocalDateTime ahora = LocalDateTime.now();
        Integer minVidaUtilDias = picklist.getMinVidaUtilDias();
        for (PicklistPtLinea linea : lineas) {
            BigDecimal cantidad = linea.getCantidad().setScale(6, RoundingMode.HALF_UP);
            if (linea.getModoAsignacion() == PicklistPtModoAsignacion.MANUAL_LOTE) {
                LoteProducto lote = linea.getLoteProducto();
                validarLoteManual(linea, lote, picklist.getAlmacenPtId(), minVidaUtilDias, ahora);
                asignaciones.add(crearAsignacion(picklist, linea.getProducto(), lote, cantidad, orden++));
                continue;
            }

            BigDecimal restante = cantidad;
            List<LoteProducto> candidatos = loteProductoRepository.findFefoSalidaPt(
                    linea.getProducto().getId().longValue(),
                    picklist.getAlmacenPtId().longValue(),
                    ESTADOS_FEFO
            );
            for (LoteProducto candidato : candidatos) {
                if (restante.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                if (!esLoteElegibleAuto(candidato, minVidaUtilDias, ahora)) {
                    continue;
                }
                BigDecimal disponible = disponibleParaSalida(candidato);
                if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal tomar = disponible.min(restante);
                asignaciones.add(crearAsignacion(picklist, linea.getProducto(), candidato, tomar, orden++));
                restante = restante.subtract(tomar);
            }
            if (restante.compareTo(BigDecimal.ZERO) > 0) {
                throw new CustomBusinessException(ApiErrorCode.PICKLIST_STOCK_INSUFICIENTE,
                        "Stock insuficiente para producto " + linea.getProducto().getNombre());
            }
        }
        return asignaciones;
    }

    private PicklistPtAsignacion crearAsignacion(PicklistPt picklist,
                                                 Producto producto,
                                                 LoteProducto lote,
                                                 BigDecimal cantidad,
                                                 short orden) {
        return PicklistPtAsignacion.builder()
                .picklist(picklist)
                .producto(producto)
                .loteProducto(lote)
                .cantidadAsignada(cantidad.setScale(6, RoundingMode.HALF_UP))
                .fechaVencimiento(lote.getFechaVencimiento())
                .almacenId(picklist.getAlmacenPtId())
                .orden(orden)
                .build();
    }

    private void validarProductoTerminado(Producto producto) {
        TipoCategoria tipo = producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getTipo() : null;
        if (tipo != TipoCategoria.PRODUCTO_TERMINADO) {
            throw new CustomBusinessException(ApiErrorCode.PICKLIST_PRODUCTO_NO_PT,
                    "El producto no corresponde a producto terminado");
        }
    }

    private void validarLoteManual(PicklistPtLinea linea,
                                   LoteProducto lote,
                                   Integer almacenPtId,
                                   Integer minVidaUtilDias,
                                   LocalDateTime ahora) {
        if (lote == null || lote.getId() == null) {
            throw new CustomBusinessException(ApiErrorCode.PICKLIST_LOTE_INVALIDO, "Lote requerido");
        }
        if (!Objects.equals(lote.getProducto().getId(), linea.getProducto().getId())) {
            throw new CustomBusinessException(ApiErrorCode.PICKLIST_LOTE_INVALIDO,
                    "El lote no corresponde al producto");
        }
        Long almacenActual = lote.getAlmacen() != null ? lote.getAlmacen().getId().longValue() : null;
        if (almacenActual == null || !Objects.equals(almacenActual, almacenPtId.longValue())) {
            throw new CustomBusinessException(ApiErrorCode.PICKLIST_LOTE_INVALIDO,
                    "El lote no pertenece al almacén PT");
        }
        validarMinVidaUtil(lote.getFechaVencimiento(), minVidaUtilDias, ahora);
        loteCalidadValidator.validarLoteUtilizable(lote);
        BigDecimal disponible = disponibleParaSalida(lote);
        if (disponible.compareTo(linea.getCantidad()) < 0) {
            throw new CustomBusinessException(ApiErrorCode.PICKLIST_STOCK_INSUFICIENTE,
                    "Stock insuficiente para lote " + lote.getCodigoLote());
        }
    }

    private boolean esLoteElegibleAuto(LoteProducto lote, Integer minVidaUtilDias, LocalDateTime ahora) {
        if (!validarMinVidaUtilSuave(lote.getFechaVencimiento(), minVidaUtilDias, ahora)) {
            return false;
        }
        try {
            loteCalidadValidator.validarLoteUtilizable(lote);
        } catch (CustomBusinessException ex) {
            return false;
        }
        return true;
    }

    private void validarMinVidaUtil(LocalDateTime fechaVencimiento,
                                    Integer minVidaUtilDias,
                                    LocalDateTime ahora) {
        if (minVidaUtilDias == null || minVidaUtilDias <= 0) {
            return;
        }
        if (fechaVencimiento == null) {
            throw new CustomBusinessException(ApiErrorCode.PICKLIST_VIDA_UTIL_INSUFICIENTE,
                    "El lote no tiene fecha de vencimiento para validar vida útil");
        }
        LocalDateTime umbral = ahora.plusDays(minVidaUtilDias);
        if (fechaVencimiento.isBefore(umbral)) {
            throw new CustomBusinessException(ApiErrorCode.PICKLIST_VIDA_UTIL_INSUFICIENTE,
                    "El lote no cumple la vida útil mínima");
        }
    }

    private boolean validarMinVidaUtilSuave(LocalDateTime fechaVencimiento,
                                            Integer minVidaUtilDias,
                                            LocalDateTime ahora) {
        if (minVidaUtilDias == null || minVidaUtilDias <= 0) {
            return true;
        }
        if (fechaVencimiento == null) {
            return false;
        }
        return !fechaVencimiento.isBefore(ahora.plusDays(minVidaUtilDias));
    }

    private BigDecimal disponibleParaSalida(LoteProducto lote) {
        BigDecimal stock = lote.getStockLote() != null ? lote.getStockLote() : BigDecimal.ZERO;
        BigDecimal reservado = lote.getStockReservado() != null ? lote.getStockReservado() : BigDecimal.ZERO;
        BigDecimal disponible = stock.subtract(reservado);
        return disponible.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : disponible;
    }

    private void validarAsignacionesParaEjecucion(PicklistPt picklist) {
        Integer minVidaUtilDias = picklist.getMinVidaUtilDias();
        LocalDateTime ahora = LocalDateTime.now();
        for (PicklistPtAsignacion asignacion : picklist.getAsignaciones()) {
            LoteProducto lote = loteProductoRepository.findByIdForUpdate(asignacion.getLoteProducto().getId())
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.PICKLIST_DESACTUALIZADO,
                            "El lote asignado ya no existe"));
            if (lote.getProducto() == null
                    || asignacion.getProducto() == null
                    || !Objects.equals(lote.getProducto().getId(), asignacion.getProducto().getId())) {
                throw new CustomBusinessException(ApiErrorCode.PICKLIST_DESACTUALIZADO,
                        "El lote asignado ya no coincide con el producto");
            }
            Long almacenActual = lote.getAlmacen() != null ? lote.getAlmacen().getId().longValue() : null;
            if (almacenActual == null || !Objects.equals(almacenActual, picklist.getAlmacenPtId().longValue())) {
                throw new CustomBusinessException(ApiErrorCode.PICKLIST_DESACTUALIZADO,
                        "El lote asignado ya no está en el almacén PT");
            }
            try {
                validarMinVidaUtil(lote.getFechaVencimiento(), minVidaUtilDias, ahora);
                loteCalidadValidator.validarLoteUtilizable(lote);
            } catch (CustomBusinessException ex) {
                throw new CustomBusinessException(ApiErrorCode.PICKLIST_DESACTUALIZADO,
                        "La validación del lote falló en ejecución", ex.getDetails());
            }
            BigDecimal disponible = disponibleParaSalida(lote);
            if (disponible.compareTo(asignacion.getCantidadAsignada()) < 0) {
                throw new CustomBusinessException(ApiErrorCode.PICKLIST_DESACTUALIZADO,
                        "Stock insuficiente al ejecutar el picklist");
            }
        }
    }

    private String generarCodigoPicklist() {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PL-PT-" + fecha + "-";
        String ultimo = picklistRepository.findTopByCodigoStartingWithOrderByCodigoDesc(prefix)
                .map(PicklistPt::getCodigo)
                .orElse(null);
        int siguiente = 1;
        if (ultimo != null) {
            String[] partes = ultimo.split("-");
            if (partes.length >= 4) {
                String secuencia = partes[partes.length - 1];
                try {
                    siguiente = Integer.parseInt(secuencia) + 1;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return prefix + String.format("%04d", siguiente);
    }

    private PicklistPtResponse toResponse(PicklistPt picklist) {
        List<PicklistPtLineaResponse> lineas = picklist.getLineas() == null
                ? List.of()
                : picklist.getLineas().stream()
                .map(linea -> new PicklistPtLineaResponse(
                        linea.getId(),
                        linea.getProducto() != null ? linea.getProducto().getId().longValue() : null,
                        linea.getProducto() != null ? linea.getProducto().getNombre() : null,
                        linea.getCantidad(),
                        linea.getModoAsignacion(),
                        linea.getLoteProducto() != null ? linea.getLoteProducto().getId() : null,
                        linea.getLoteProducto() != null ? linea.getLoteProducto().getCodigoLote() : null
                ))
                .toList();

        List<PicklistPtAsignacionResponse> asignaciones = picklist.getAsignaciones() == null
                ? List.of()
                : picklist.getAsignaciones().stream()
                .map(asignacion -> new PicklistPtAsignacionResponse(
                        asignacion.getId(),
                        asignacion.getProducto() != null ? asignacion.getProducto().getId().longValue() : null,
                        asignacion.getProducto() != null ? asignacion.getProducto().getNombre() : null,
                        asignacion.getLoteProducto() != null ? asignacion.getLoteProducto().getId() : null,
                        asignacion.getLoteProducto() != null ? asignacion.getLoteProducto().getCodigoLote() : null,
                        asignacion.getCantidadAsignada(),
                        asignacion.getFechaVencimiento(),
                        asignacion.getAlmacenId(),
                        asignacion.getOrden()
                ))
                .toList();

        return new PicklistPtResponse(
                picklist.getId(),
                picklist.getCodigo(),
                picklist.getClienteNombre(),
                picklist.getMinVidaUtilDias(),
                picklist.getEstado(),
                picklist.getAlmacenPtId(),
                picklist.getTipoMovimientoDetalleId(),
                picklist.getDocReferencia(),
                picklist.getObservaciones(),
                picklist.getFechaCreacion(),
                picklist.getFechaConfirmacion(),
                picklist.getFechaEjecucion(),
                lineas,
                asignaciones
        );
    }

    private byte[] buildPdf(PicklistPt picklist) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(36f, 36f, 36f, 36f);

            Table header = new Table(new float[]{60f, 40f}).useAllAvailableWidth();
            Cell left = new Cell().setBorder(Border.NO_BORDER);
            left.add(new Paragraph()
                    .add(new Text("Picklist PT: ").setBold().setFontSize(11))
                    .add(new Text(picklist.getCodigo()).setFontSize(10)));
            left.add(new Paragraph()
                    .add(new Text("Cliente: ").setBold().setFontSize(11))
                    .add(new Text(picklist.getClienteNombre()).setFontSize(10)));
            Cell right = new Cell().setBorder(Border.NO_BORDER);
            right.add(new Paragraph()
                    .add(new Text("Fecha: ").setBold().setFontSize(11))
                    .add(new Text(picklist.getFechaCreacion() != null
                            ? picklist.getFechaCreacion().toLocalDate().toString()
                            : "-").setFontSize(10)));
            right.add(new Paragraph()
                    .add(new Text("Vida útil mínima (días): ").setBold().setFontSize(11))
                    .add(new Text(picklist.getMinVidaUtilDias() != null
                            ? picklist.getMinVidaUtilDias().toString()
                            : "-").setFontSize(10)));
            header.addCell(left);
            header.addCell(right);
            header.setMarginBottom(10f);
            document.add(header);

            if (picklist.getDocReferencia() != null && !picklist.getDocReferencia().isBlank()) {
                document.add(new Paragraph("Doc. ref: " + picklist.getDocReferencia()).setFontSize(9));
            }
            if (picklist.getObservaciones() != null && !picklist.getObservaciones().isBlank()) {
                document.add(new Paragraph("Observaciones: " + picklist.getObservaciones()).setFontSize(9));
            }

            float[] widths = {26f, 16f, 12f, 10f, 10f, 14f};
            Table table = new Table(widths).useAllAvailableWidth();
            String[] headers = {"Producto", "Lote", "Vencimiento", "Cant.", "UM", "Almacén"};
            for (String h : headers) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setFontSize(9).setBold())
                        .setBackgroundColor(new DeviceRgb(0xF2, 0xF2, 0xF2))
                        .setPadding(6)
                        .setTextAlignment(TextAlignment.LEFT));
            }

            int index = 0;
            for (PicklistPtAsignacion asignacion : picklist.getAsignaciones()) {
                index++;
                boolean zebra = index % 2 == 0;
                DeviceRgb zebraColor = new DeviceRgb(0xFA, 0xFA, 0xFA);

                String producto = asignacion.getProducto() != null ? asignacion.getProducto().getNombre() : "-";
                String lote = asignacion.getLoteProducto() != null ? asignacion.getLoteProducto().getCodigoLote() : "-";
                String venc = asignacion.getFechaVencimiento() != null
                        ? asignacion.getFechaVencimiento().toLocalDate().toString()
                        : "-";
                String cantidad = asignacion.getCantidadAsignada() != null
                        ? String.format(Locale.US, "%,.3f", asignacion.getCantidadAsignada())
                        : "-";
                String unidad = asignacion.getProducto() != null
                        && asignacion.getProducto().getUnidadMedida() != null
                        ? asignacion.getProducto().getUnidadMedida().getNombre()
                        : "-";
                String almacen = asignacion.getLoteProducto() != null
                        && asignacion.getLoteProducto().getAlmacen() != null
                        ? asignacion.getLoteProducto().getAlmacen().getNombre()
                        : "-";

                String[] valores = {producto, lote, venc, cantidad, unidad, almacen};
                for (int i = 0; i < valores.length; i++) {
                    Cell cell = new Cell()
                            .add(new Paragraph(valores[i]).setFontSize(9))
                            .setPadding(5)
                            .setTextAlignment(i == 3 ? TextAlignment.RIGHT : TextAlignment.LEFT);
                    if (zebra) {
                        cell.setBackgroundColor(zebraColor);
                    }
                    table.addCell(cell);
                }
            }
            document.add(table);

            Table firmas = new Table(new float[]{33f, 33f, 33f})
                    .useAllAvailableWidth()
                    .setMarginTop(20f);
            for (int i = 0; i < 3; i++) {
                firmas.addCell(new Cell().add(new Paragraph("____________________"))
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.CENTER));
            }
            String[] labels = {"Alistó", "Verificó", "Despachó"};
            for (String l : labels) {
                firmas.addCell(new Cell().add(new Paragraph(l).setFontSize(9))
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.CENTER));
            }
            document.add(firmas);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF", e);
        }
    }
}
