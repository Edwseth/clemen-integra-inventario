package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.mapper.OrdenCompraMapper;
import com.willyes.clemenintegra.inventario.mapper.HistorialEstadoOrdenMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.model.enums.TipoOrdenCompra;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.OrdenCompraPdfService;
import com.willyes.clemenintegra.inventario.service.OrdenCompraService;
import com.willyes.clemenintegra.inventario.service.HistorialEstadoOrdenService;
import com.willyes.clemenintegra.inventario.service.RecepcionOCService;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping({"/api/inventario/ordenes", "/api/ordenes-compra"})
@RequiredArgsConstructor
public class OrdenCompraController {

    private final OrdenCompraRepository ordenCompraRepository;
    private final OrdenCompraDetalleRepository detalleRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final OrdenCompraService ordenCompraService;
    private final HistorialEstadoOrdenService historialEstadoOrdenService;
    private final RecepcionOCService recepcionOCService;
    private final OrdenCompraPdfService ordenCompraPdfService;
    private final OrdenCompraMapper mapper;

    private static final BigDecimal IVA_MIN = BigDecimal.ZERO;
    private static final BigDecimal IVA_MAX = new BigDecimal("100");

    @PostMapping
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_WRITE')")
    @Transactional
    public ResponseEntity<OrdenCompra> crear(@RequestBody OrdenCompraRequestDTO dto) {
        // 1. Validar proveedor
        Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proveedor no encontrado"));

        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Debe registrar al menos un detalle");
        }
        
        // 2. Crear orden y detalles dentro de la misma transacción
        LocalDate fechaCompromisoEntrega = ordenCompraService.calcularFechaCompromisoEntrega(dto.getDetalles());

        OrdenCompra orden = OrdenCompra.builder()
                .proveedor(proveedor)
                .estado(EstadoOrdenCompra.CREADA)
                .fechaOrden(LocalDateTime.now())
                .observaciones(dto.getObservaciones())
                .comprador(dto.getComprador())
                .condicionesPago(dto.getCondicionesPago())
                .descuento(dto.getDescuento() != null
                        ? dto.getDescuento()
                        : BigDecimal.ZERO)
                .fechaCompromisoEntrega(fechaCompromisoEntrega)
                .build();

        orden.setCodigoOrden(ordenCompraService.generarCodigoOrdenCompra());

        List<Producto> productosDetalle = dto.getDetalles().stream().map(d ->
                productoRepository.findById(d.getProductoId())
                        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Producto no encontrado"))
        ).toList();

        orden.setTipo(ordenCompraService.determinarTipoOrdenPorDetalles(productosDetalle));

        List<OrdenCompraDetalle> detalles = new java.util.ArrayList<>();
        for (int i = 0; i < dto.getDetalles().size(); i++) {
            var d = dto.getDetalles().get(i);
            Producto producto = productosDetalle.get(i);

            validarIvaPorcentaje(d.getIva());
            BigDecimal valorTotal = calcularValorTotalLinea(d.getCantidad(), d.getValorUnitario(), d.getIva());

            detalles.add(OrdenCompraDetalle.builder()
                    .ordenCompra(orden)
                    .producto(producto)
                    .cantidad(d.getCantidad())
                    .valorUnitario(d.getValorUnitario())
                    .valorTotal(valorTotal)
                    .iva(d.getIva())
                    .cantidadRecibida(BigDecimal.ZERO)
                    .fechaNecesidad(d.getFechaNecesidad())
                    .build());
        }

        orden.setDetalles(detalles);

        try {
            OrdenCompra guardada = ordenCompraRepository.save(orden);
            return ResponseEntity.status(CREATED).body(guardada);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Error al guardar la orden de compra");
        }
    }

    @PutMapping("/{id}")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_WRITE','INV_WORKFLOW','INV_DECIDE')")
    public ResponseEntity<OrdenCompra> actualizar(@PathVariable Long id,
                                                  @RequestBody OrdenCompraRequestDTO dto) {
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada"));

        // Actualizar proveedor si cambió
        if (!orden.getProveedor().getId().equals(dto.getProveedorId())) {
            Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor no encontrado"));
            orden.setProveedor(proveedor);
        }

        orden.setObservaciones(dto.getObservaciones());
        orden.setFechaCompromisoEntrega(ordenCompraService.calcularFechaCompromisoEntrega(dto.getDetalles()));

        // Eliminar detalles anteriores
        detalleRepository.deleteByOrdenCompra_Id(id);

        List<Producto> productosDetalle = dto.getDetalles().stream().map(d ->
                productoRepository.findById(d.getProductoId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"))
        ).toList();
        orden.setTipo(ordenCompraService.determinarTipoOrdenPorDetalles(productosDetalle));

        // Crear y guardar nuevos detalles
        List<OrdenCompraDetalle> nuevosDetalles = new java.util.ArrayList<>();
        for (int i = 0; i < dto.getDetalles().size(); i++) {
            var d = dto.getDetalles().get(i);
            Producto producto = productosDetalle.get(i);

            validarIvaPorcentaje(d.getIva());
            BigDecimal valorTotal = calcularValorTotalLinea(d.getCantidad(), d.getValorUnitario(), d.getIva());

            nuevosDetalles.add(OrdenCompraDetalle.builder()
                    .ordenCompra(orden)
                    .producto(producto)
                    .cantidad(d.getCantidad())
                    .valorUnitario(d.getValorUnitario())
                    .valorTotal(valorTotal)
                    .iva(d.getIva())
                    .cantidadRecibida(BigDecimal.ZERO)
                    .fechaNecesidad(d.getFechaNecesidad())
                    .build());
        }

        detalleRepository.saveAll(nuevosDetalles);

        ordenCompraRepository.save(orden);
        return ResponseEntity.ok(orden);
    }

    @GetMapping
    public ResponseEntity<Page<OrdenCompraResponseDTO>> listar(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "estado", required = false) EstadoOrdenCompra estado,
            @RequestParam(name = "tipo", required = false) TipoOrdenCompra tipo,
            @RequestParam(name = "proveedor", required = false) String proveedor,
            @RequestParam(name = "atrasadas", required = false, defaultValue = "false") boolean atrasadas) {
        TipoOrdenCompra tipoFiltro = tipo != null ? tipo : TipoOrdenCompra.BIENES;
        Page<OrdenCompraResponseDTO> page = ordenCompraService.listarFiltrado(pageable, atrasadas, estado, tipoFiltro, proveedor);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/estado")
    public ResponseEntity<Page<OrdenCompraResponseDTO>> listarPorEstado(
            @RequestParam EstadoOrdenCompra estado,
            @RequestParam(name = "tipo", required = false) TipoOrdenCompra tipo,
            @PageableDefault(size = 10) Pageable pageable) {
        TipoOrdenCompra tipoFiltro = tipo != null ? tipo : TipoOrdenCompra.BIENES;
        Page<OrdenCompraResponseDTO> page = ordenCompraService.listarPorEstado(estado, tipoFiltro, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}/detalles")
    public ResponseEntity<OrdenCompraConDetallesResponse> obtenerOrdenConDetalles(@PathVariable Long id) {
        return ordenCompraService.buscarPorIdConDetalles(id)
                .map(mapper::toOrdenCompraConDetallesResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/estado")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_WRITE','INV_WORKFLOW','INV_DECIDE')")
    public ResponseEntity<HistorialEstadoOrdenResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestBody CambioEstadoOrdenRequest request,
            @AuthenticationPrincipal CustomUserDetails usuarioAutenticado) {
        var historial = ordenCompraService.cambiarEstado(
                id,
                request.estado,
                usuarioAutenticado,
                request.observaciones);
        return ResponseEntity.ok(HistorialEstadoOrdenMapper.toResponse(historial));
    }

    @GetMapping("/{id}/transiciones")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ','INV_WORKFLOW')")
    public ResponseEntity<List<String>> obtenerTransiciones(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails usuarioAutenticado) {
        if (usuarioAutenticado == null) {
            throw new ResponseStatusException(FORBIDDEN, "USUARIO_NO_AUTENTICADO");
        }

        OrdenCompra orden = ordenCompraService.buscarPorIdConDetalles(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada"));

        List<String> estados = ordenCompraService.transicionesPermitidas(
                        orden,
                        usuarioAutenticado.getAuthorities().stream()
                                .map(a -> a.getAuthority())
                                .toList())
                .stream()
                .map(Enum::name)
                .toList();

        return ResponseEntity.ok(estados);
    }

    @GetMapping("/{id}/pdf")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_EXPORT')")
    public ResponseEntity<byte[]> pdf(@PathVariable("id") Long id) {   // <-- Long
        // 1) Cargar OC con detalles
        var ocOpt = ordenCompraService.buscarPorIdConDetalles(id);
        if (ocOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var oc = ocOpt.get(); // <-- desenvolver Optional

        // 2) Generar PDF con la entidad (no Optional)
        byte[] pdf = ordenCompraPdfService.generarPdf(oc);

        // 3) Nombre de archivo seguro
        String nombre = "OC-" + (oc.getCodigoOrden() != null ? oc.getCodigoOrden() : oc.getId()) + ".pdf";

        // 4) Respuesta
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .body(pdf);
    }

    private String loadTemplate(String classpathLocation) {        // <- sin guion bajo
        try (InputStream is = new ClassPathResource(classpathLocation).getInputStream()) {
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("No se encontró la plantilla: " + classpathLocation, e);
        }
    }

    private void validarIvaPorcentaje(BigDecimal iva) {
        if (iva == null) {
            return;
        }
        if (iva.compareTo(IVA_MIN) < 0 || iva.compareTo(IVA_MAX) > 0) {
            throw new CustomBusinessException(ApiErrorCode.IVA_PORCENTAJE_INVALIDO,
                    "IVA_PORCENTAJE_INVALIDO");
        }
    }

    private BigDecimal calcularValorTotalLinea(BigDecimal cantidad, BigDecimal valorUnitario, BigDecimal ivaPorcentaje) {
        BigDecimal cantidadSegura = cantidad != null ? cantidad : BigDecimal.ZERO;
        BigDecimal valorUnitarioSeguro = valorUnitario != null ? valorUnitario : BigDecimal.ZERO;
        BigDecimal ivaSeguro = ivaPorcentaje != null ? ivaPorcentaje : BigDecimal.ZERO;

        BigDecimal subtotal = cantidadSegura.multiply(valorUnitarioSeguro);
        BigDecimal ivaValor = subtotal.multiply(ivaSeguro)
                .divide(BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP);
        return subtotal.add(ivaValor).setScale(6, java.math.RoundingMode.HALF_UP);
    }

    private static java.math.BigDecimal bd(Number n) {
        return n == null ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(n.toString());
    }
    private static String nullSafe(String s) { return s == null ? "" : s; }
    private static String esc(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    @GetMapping("/{id}/recepciones")
    public ResponseEntity<List<RecepcionOCResponseDTO>> recepcionesPorOrden(@PathVariable Long id) {
        List<RecepcionOCResponseDTO> recepciones = recepcionOCService.listarRecepcionesPorOrden(id);
        return ResponseEntity.ok(recepciones);
    }


    @PutMapping("/{id}/ejecutar-servicio")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_WRITE','INV_WORKFLOW','INV_DECIDE')")
    public ResponseEntity<HistorialEstadoOrdenResponse> ejecutarServicio(
            @PathVariable Long id,
            @RequestBody(required = false) EjecutarServicioOrdenRequest request,
            @AuthenticationPrincipal CustomUserDetails usuarioAutenticado) {
        HistorialEstadoOrden historial = ordenCompraService.ejecutarServicio(
                id,
                usuarioAutenticado,
                request != null ? request.observaciones() : null);
        return ResponseEntity.ok(HistorialEstadoOrdenMapper.toResponse(historial));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialEstadoOrdenResponse>> historial(@PathVariable Long id) {
        List<HistorialEstadoOrdenResponse> historial = historialEstadoOrdenService.listarPorOrden(id)
                .stream()
                .map(HistorialEstadoOrdenMapper::toResponse)
                .toList();
        return ResponseEntity.ok(historial);
    }

}
