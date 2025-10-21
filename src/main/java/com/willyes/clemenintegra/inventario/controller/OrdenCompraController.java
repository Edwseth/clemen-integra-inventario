package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.mapper.OrdenCompraMapper;
import com.willyes.clemenintegra.inventario.mapper.HistorialEstadoOrdenMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.OrdenCompraPdfService;
import com.willyes.clemenintegra.inventario.service.OrdenCompraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final OrdenCompraPdfService ordenCompraPdfService;
    private final OrdenCompraMapper mapper;

    @PostMapping
    @PreAuthorize("hasAuthority('ROL_COMPRADOR')")
    @Transactional
    public ResponseEntity<OrdenCompra> crear(@RequestBody OrdenCompraRequestDTO dto) {
        // 1. Validar proveedor
        Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proveedor no encontrado"));

        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Debe registrar al menos un detalle");
        }
        
        // 2. Crear orden y detalles dentro de la misma transacción
        OrdenCompra orden = OrdenCompra.builder()
                .proveedor(proveedor)
                .estado(EstadoOrdenCompra.CREADA)
                .fechaOrden(LocalDateTime.now())
                .observaciones(dto.getObservaciones())
                .build();

        orden.setCodigoOrden(ordenCompraService.generarCodigoOrdenCompra());

        List<OrdenCompraDetalle> detalles = dto.getDetalles().stream().map(d -> {
            Producto producto = productoRepository.findById(d.getProductoId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Producto no encontrado"));

            BigDecimal valorTotal = d.getValorUnitario().multiply(d.getCantidad());

            return OrdenCompraDetalle.builder()
                    .ordenCompra(orden)
                    .producto(producto)
                    .cantidad(d.getCantidad())
                    .valorUnitario(d.getValorUnitario())
                    .valorTotal(valorTotal)
                    .iva(d.getIva())
                    .cantidadRecibida(BigDecimal.ZERO)
                    .build();
        }).toList();

        orden.setDetalles(detalles);

        try {
            OrdenCompra guardada = ordenCompraRepository.save(orden);
            return ResponseEntity.status(CREATED).body(guardada);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Error al guardar la orden de compra");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_SUPER_ADMIN')")
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

        // Eliminar detalles anteriores
        detalleRepository.deleteByOrdenCompra_Id(id);

        // Crear y guardar nuevos detalles
        List<OrdenCompraDetalle> nuevosDetalles = dto.getDetalles().stream().map(d -> {
            Producto producto = productoRepository.findById(d.getProductoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

            BigDecimal valorTotal = d.getValorUnitario().multiply(d.getCantidad());

            return OrdenCompraDetalle.builder()
                    .ordenCompra(orden)
                    .producto(producto)
                    .cantidad(d.getCantidad())
                    .valorUnitario(d.getValorUnitario())
                    .valorTotal(valorTotal)
                    .iva(d.getIva())
                    .cantidadRecibida(BigDecimal.ZERO)
                    .build();
        }).toList();

        detalleRepository.saveAll(nuevosDetalles);

        ordenCompraRepository.save(orden);
        return ResponseEntity.ok(orden);
    }

    @GetMapping
    public ResponseEntity<Page<OrdenCompraResponseDTO>> listar(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<OrdenCompraResponseDTO> page = ordenCompraService.listar(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/estado")
    public ResponseEntity<Page<OrdenCompraResponseDTO>> listarPorEstado(
            @RequestParam EstadoOrdenCompra estado,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<OrdenCompraResponseDTO> page = ordenCompraService.listarPorEstado(estado, pageable);
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
    public ResponseEntity<HistorialEstadoOrdenResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestBody CambioEstadoOrdenRequest request) {
        var historial = ordenCompraService.cambiarEstado(
                id,
                request.estado,
                request.usuarioId,
                request.observaciones);
        return ResponseEntity.ok(HistorialEstadoOrdenMapper.toResponse(historial));
    }

    @GetMapping(value = "/{id}/pdf", produces = "application/pdf")
    @PreAuthorize("hasAnyAuthority('ROL_SUPER_ADMIN','ROL_COMPRADOR')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        var oc = ordenCompraService.buscarPorIdConDetalles(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // ---- Mapear a DTO (rellena con lo que tengas disponible) ----
        var dto = new OrdenCompraPdfDTO();
        dto.numero = oc.getCodigoOrden();
        dto.fecha  = oc.getFechaOrden() != null ? oc.getFechaOrden() : LocalDateTime.now();
        dto.condicionesPago = (oc.getCondicionesPago() == null)
                ? "30 DÍAS NETO"
                : switch (oc.getCondicionesPago()) {
            case CONTADO -> "CONTADO";
            case DIAS_30 -> "30 DÍAS NETO";
            case DIAS_60 -> "60 DÍAS NETO";
        };
        dto.observaciones = oc.getObservaciones();
        dto.comprador = java.util.Optional.ofNullable(oc.getComprador()).orElse("");


        // Empresa fija (encabezado)
        dto.empresa = new OrdenCompraPdfDTO.EmpresaDTO();
        dto.empresa.nombre    = "LABORATORIO CLEMEN SAS";
        dto.empresa.nit       = "901626440";
        dto.empresa.direccion = "Carrera 41 D # 46-40 Union de Vivienda, Cali Valle del Cauca, Colombia";
        dto.empresa.telefono  = "Teléfono / Movil 3175081762";

        // Proveedor (pon lo que tengas en tu entidad)
        var prov = new OrdenCompraPdfDTO.ProveedorDTO();
        prov.nombre    = oc.getProveedor().getNombre();
        prov.nit       = oc.getProveedor().getIdentificacion();
        prov.direccion = oc.getProveedor().getDireccion();
        prov.ciudad    = oc.getProveedor().getCiudad();
        prov.telefono  = oc.getProveedor().getTelefono();
        prov.paginaWeb = oc.getProveedor().getPaginaWeb();
        dto.proveedor = prov;

        // Items (adaptar a tus campos)
        var items = new java.util.ArrayList<OrdenCompraPdfDTO.ItemDTO>();
        oc.getDetalles().forEach(d -> {
            var it = new OrdenCompraPdfDTO.ItemDTO();
            it.codigo         = d.getProducto().getCodigoSku();
            it.descripcion    = d.getProducto().getNombre();
            it.udm            = d.getProducto().getUnidadMedida() != null ? d.getProducto().getUnidadMedida().getSimbolo() : "";
            it.cantidad       = bd(d.getCantidad());
            it.precioUnitario = bd(d.getValorUnitario());
            it.iva            = bd(d.getIva());
            it.icui           = bd(0);
            it.total          = it.cantidad.multiply(it.precioUnitario); // ajusta si sumas IVA aquí
            items.add(it);
        });
        dto.items = items;

        dto.subtotal = items.stream().map(i -> i.total).reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.valorIva = BigDecimal.ZERO;  // calcula si corresponde
        dto.valorIcui = BigDecimal.ZERO; // idem
        dto.total = dto.subtotal.add(dto.valorIva).add(dto.valorIcui);

        // ---- Construir HTML sustituyendo placeholders ----
        var fecha = dto.fecha;
        String html = loadTemplate("templates/oc/orden-compra.ftl");  // <- l minúscula

        html = html.replace("${empresa.nombre}", esc(dto.empresa.nombre))
                .replace("${empresa.nit}", esc(dto.empresa.nit))
                .replace("${empresa.direccion}", esc(dto.empresa.direccion))
                .replace("${empresa.telefono}", esc(dto.empresa.telefono))
                .replace("${numero}", esc(dto.numero))
                .replace("${fechaDia}", String.format("%02d", fecha.getDayOfMonth()))
                .replace("${fechaMes}", fecha.getMonth().name().substring(0,3))
                .replace("${fechaAnio}", String.valueOf(fecha.getYear()))
                .replace("${condicionesPago}", esc(dto.condicionesPago))
                .replace("${observaciones!''}", esc(nullSafe(dto.observaciones)))
                .replace("${comprador!''}", esc(nullSafe(dto.comprador)))
                .replace("${proveedor.nombre}", esc(nullSafe(prov.nombre)))
                .replace("${proveedor.nit}", esc(nullSafe(prov.nit)))
                .replace("${proveedor.direccion}", esc(nullSafe(prov.direccion)))
                .replace("${proveedor.ciudad}", esc(nullSafe(prov.ciudad)))
                .replace("${proveedor.telefono}", esc(nullSafe(prov.telefono)))
                .replace("${proveedor.paginaWeb}", esc(nullSafe(prov.paginaWeb)));

        // Render de filas de items (sencillo; si usas motor de plantillas, úsalo)
        StringBuilder filas = new StringBuilder();
        for (var it : dto.items) {
            filas.append("<tr>")
                    .append("<td>").append(esc(nullSafe(it.codigo))).append("</td>")
                    .append("<td>").append(esc(nullSafe(it.descripcion))).append("</td>")
                    .append("<td>").append(esc(nullSafe(it.fechaNecesidad))).append("</td>")
                    .append("<td>").append(esc(nullSafe(it.udm))).append("</td>")
                    .append("<td class='right'>").append(it.cantidad).append("</td>")
                    .append("<td class='right'>").append(it.precioUnitario).append("</td>")
                    .append("<td class='right'>").append(it.iva).append("</td>")
                    .append("<td class='right'>").append(it.icui).append("</td>")
                    .append("<td class='right'>").append(it.total).append("</td>")
                    .append("</tr>");
        }
        html = html.replaceFirst("(?s)<tbody>.*?</tbody>", "<tbody>" + filas + "</tbody>");
        html = html.replace("${valorIva}", dto.valorIva.toString())
                .replace("${valorIcui}", dto.valorIcui.toString())
                .replace("${total}", dto.total.toString());

        // Generar PDF
        byte[] pdf = ordenCompraPdfService.render(html);
        var headers = new org.springframework.http.HttpHeaders();
        headers.set("Content-Disposition", "attachment; filename=orden-compra-" + id + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    private String loadTemplate(String classpathLocation) {        // <- sin guion bajo
        try (InputStream is = new ClassPathResource(classpathLocation).getInputStream()) {
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("No se encontró la plantilla: " + classpathLocation, e);
        }
    }

    private static java.math.BigDecimal bd(Number n) {
        return n == null ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(n.toString());
    }
    private static String nullSafe(String s) { return s == null ? "" : s; }
    private static String esc(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

}

