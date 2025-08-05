package com.willyes.clemenintegra.bom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.bom.dto.*;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.*;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.model.enums.TipoDocumento;
import com.willyes.clemenintegra.bom.service.*;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bom/formulas")
@RequiredArgsConstructor
public class FormulaProductoController {

    private final FormulaProductoService formulaService;
    private final BomMapper bomMapper;
    private final ProductoService productoService;
    private final UnidadMedidaRepository unidadMedidaRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public List<FormulaProductoResponse> listarTodas() {
        return formulaService.listarTodas().stream()
                .map(formula -> bomMapper.toResponse(formula))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<FormulaProductoResponse> obtenerPorId(@PathVariable Long id) {
        return formulaService.buscarPorId(id)
                .map(formula -> bomMapper.toResponse(formula))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<FormulaProductoResponse> crear(
            @RequestPart("formula") String formulaJson,
            @RequestPart(value = "pdfs", required = false) MultipartFile[] pdfs) throws IOException {
        // Convertir JSON a DTO
        FormulaProductoRequest request = new ObjectMapper().readValue(formulaJson, FormulaProductoRequest.class);

        // Obtiene entidades principales
        Producto producto = productoService.findById(request.getProductoId());
        Usuario creador = new Usuario();
        creador.setId(request.getCreadoPorId());

        FormulaProducto formula = bomMapper.toEntity(request, producto, creador);
        formula.setFechaCreacion(LocalDateTime.now());
        if (request.getEstado() != null) {
            formula.setEstado(EstadoFormula.valueOf(request.getEstado()));
        }

        // Mapear insumos a DetalleFormula
        if (request.getInsumos() != null) {
            List<DetalleFormula> detalles = request.getInsumos().stream().map(dto -> {
                Producto productoInsumo = productoService.findById(dto.getProductoId());
                UnidadMedida unidad = unidadMedidaRepository.findByNombre(dto.getUnidadMedida())
                        .orElseThrow(() -> new IllegalArgumentException("Unidad de medida no encontrada: " + dto.getUnidadMedida()));

                DetalleFormula detalle = new DetalleFormula();
                detalle.setFormula(formula);
                detalle.setInsumo(productoInsumo);
                detalle.setUnidadMedida(unidad);
                detalle.setCantidadNecesaria(BigDecimal.valueOf(dto.getCantidad()));
                detalle.setObligatorio("OBLIGATORIO".equalsIgnoreCase(dto.getTipo()));
                return detalle;
            }).collect(Collectors.toList());
            formula.setDetalles(detalles);
        }

        // Procesar documentos adjuntos
        if (pdfs != null && pdfs.length > 0) {
            List<DocumentoFormula> documentos = new ArrayList<>();
            for (MultipartFile pdf : pdfs) {
                if (pdf == null || pdf.isEmpty()) continue;

                String nombreOriginal = pdf.getOriginalFilename();
                String nombreSanitizado = (nombreOriginal != null ? nombreOriginal : "documento")
                        .replaceAll("[^a-zA-Z0-9._-]", "_");
                String nombreArchivo = System.currentTimeMillis() + "_" + nombreSanitizado;

                Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "formulas");
                Files.createDirectories(uploadDir);
                Path destino = uploadDir.resolve(nombreArchivo);
                pdf.transferTo(destino.toFile());

                TipoDocumento tipoDoc = TipoDocumento.PROCEDIMIENTO;
                if (nombreOriginal != null) {
                    String lower = nombreOriginal.toLowerCase();
                    if (lower.contains("msds")) tipoDoc = TipoDocumento.MSDS;
                    else if (lower.contains("instructivo")) tipoDoc = TipoDocumento.INSTRUCTIVO;
                    else if (lower.contains("procedimiento")) tipoDoc = TipoDocumento.PROCEDIMIENTO;
                }

                DocumentoFormula documento = DocumentoFormula.builder()
                        .tipoDocumento(tipoDoc)
                        .rutaArchivo(nombreArchivo)
                        .formula(formula)
                        .build();
                documentos.add(documento);
            }
            formula.setDocumentos(documentos);
        }

        FormulaProducto guardado = formulaService.guardar(formula);
        return ResponseEntity.ok(bomMapper.toResponse(guardado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<FormulaProductoResponse> actualizar(@PathVariable Long id, @RequestBody FormulaProductoRequest request) {
        return formulaService.buscarPorId(id)
                .map(existente -> {
                    Producto producto = new Producto(); producto.setId(request.getProductoId().intValue());
                    Usuario creador = new Usuario(); creador.setId(request.getCreadoPorId());
                    FormulaProducto entidad = bomMapper.toEntity(request, producto, creador);
                    entidad.setId(existente.getId());
                    return ResponseEntity.ok(bomMapper.toResponse(formulaService.guardar(entidad)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        formulaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activa")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<FormulaProductoResponse> obtenerFormulaActiva(@RequestParam Long productoId) {
        return ResponseEntity.ok(formulaService.obtenerFormulaActivaPorProducto(productoId));
    }
}

