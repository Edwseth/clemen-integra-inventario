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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bom/formulas")
@RequiredArgsConstructor
@Slf4j
public class FormulaProductoController {

    private static final String BOM_READ_ROLE_FALLBACK = "'ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_PLANEADOR','ROL_SUPER_ADMIN'";
    private static final String BOM_WRITE_ROLE_FALLBACK = "'ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN'";
    // TODO(rbac-bom-cut1): retirar fallback por roles y permisos granulares BOM_* legacy al finalizar migracion canonica.

    private final FormulaProductoService formulaService;
    private final BomMapper bomMapper;
    private final ProductoService productoService;
    private final UnidadMedidaRepository unidadMedidaRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('BOM_READ','BOM_FORMULA_READ'," + BOM_READ_ROLE_FALLBACK + ")")
    public Page<FormulaProductoSelectorDTO> listarTodas(
            @RequestParam(required = false) EstadoFormula estado,
            @RequestParam(required = false) String producto,
            @PageableDefault(size = 20) Pageable pageable) {
        return formulaService.listarResumen(estado, producto, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('BOM_READ','BOM_FORMULA_READ'," + BOM_READ_ROLE_FALLBACK + ")")
    public ResponseEntity<FormulaProductoDetalleDTO> obtenerPorId(@PathVariable Long id) {
        return formulaService.buscarDetallePorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('BOM_WRITE','BOM_FORMULA_WRITE'," + BOM_WRITE_ROLE_FALLBACK + ")")
    public ResponseEntity<FormulaProductoResponse> crear(
            @RequestPart("formula") String formulaJson,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) throws IOException {
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
                UnidadMedida unidad = unidadMedidaRepository
                        .findByNombreIgnoreCaseOrSimboloIgnoreCase(dto.getUnidadMedida(), dto.getUnidadMedida())
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

        // Procesar documento adjunto
        if (archivo != null && !archivo.isEmpty()) {
            String nombreOriginal = archivo.getOriginalFilename();
            String nombreSanitizado = (nombreOriginal != null ? nombreOriginal : "documento")
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
            String nombreArchivo = System.currentTimeMillis() + "_" + nombreSanitizado;

            Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "formulas");
            Files.createDirectories(uploadDir);
            Path destino = uploadDir.resolve(nombreArchivo);
            archivo.transferTo(destino.toFile());

            TipoDocumento tipoDoc = TipoDocumento.PROCEDIMIENTO;
            if (nombreOriginal != null) {
                String lower = nombreOriginal.toLowerCase();
                if (lower.contains("msds")) tipoDoc = TipoDocumento.MSDS;
                else if (lower.contains("instructivo")) tipoDoc = TipoDocumento.INSTRUCTIVO;
                else if (lower.contains("procedimiento")) tipoDoc = TipoDocumento.PROCEDIMIENTO;
            }

            DocumentoFormula documento = DocumentoFormula.builder()
                    .tipoDocumento(tipoDoc)
                    .nombreArchivo(nombreArchivo)
                    .rutaArchivo(Paths.get("uploads", "formulas", nombreArchivo).toString())
                    .fechaSubida(LocalDateTime.now())
                    .usuario(creador)
                    .formula(formula)
                    .build();
            formula.setDocumentos(List.of(documento));
        } else {
            log.info("No se recibió archivo adjunto para la fórmula");
        }

        FormulaProducto guardado = formulaService.guardar(formula);
        return ResponseEntity.ok(bomMapper.toResponseDTO(guardado));
    }

    @PostMapping("/{id}/clonar")
    @PreAuthorize("hasAnyAuthority('BOM_WRITE','BOM_FORMULA_WRITE'," + BOM_WRITE_ROLE_FALLBACK + ")")
    public ResponseEntity<FormulaProductoResumenDTO> clonar(
            @PathVariable Long id,
            @AuthenticationPrincipal com.willyes.clemenintegra.shared.security.service.CustomUserDetails usuario) {
        FormulaProducto nuevaFormula = formulaService.clonarFormula(id, usuario.getId());
        FormulaProductoResumenDTO dto = bomMapper.toResumenDTO(nuevaFormula);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('BOM_WRITE','BOM_FORMULA_WRITE'," + BOM_WRITE_ROLE_FALLBACK + ")")
    public ResponseEntity<FormulaProductoResponse> actualizar(@PathVariable Long id, @RequestBody FormulaProductoRequest request) {
        return formulaService.buscarPorId(id)
                .map(existente -> {
                    Producto producto = new Producto(); producto.setId(request.getProductoId().intValue());
                    Usuario creador = new Usuario(); creador.setId(request.getCreadoPorId());
                    FormulaProducto entidad = bomMapper.toEntity(request, producto, creador);
                    entidad.setId(existente.getId());
                    return ResponseEntity.ok(bomMapper.toResponseDTO(formulaService.guardar(entidad)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyAuthority('BOM_DECIDE','BOM_WORKFLOW','BOM_WORKFLOW_FINISH','BOM_WRITE','BOM_FORMULA_WRITE'," + BOM_WRITE_ROLE_FALLBACK + ")")
    @PostMapping("/{id}/cambiar-estado")
    // TODO:REMOVE_AFTER_BOM_FULL_MIGRATION
    public ResponseEntity<FormulaProductoResumenDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestBody @Valid CambiarEstadoFormulaRequest request,
            @AuthenticationPrincipal com.willyes.clemenintegra.shared.security.service.CustomUserDetails usuario) {
        FormulaProducto formulaActualizada = formulaService.cambiarEstado(id, request.nuevoEstado(), usuario.getId());
        FormulaProductoResumenDTO dto = bomMapper.toResumenDTO(formulaActualizada);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('BOM_WRITE','BOM_FORMULA_WRITE'," + BOM_WRITE_ROLE_FALLBACK + ")")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        formulaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activa")
    @PreAuthorize("hasAnyAuthority('BOM_READ','BOM_FORMULA_READ'," + BOM_READ_ROLE_FALLBACK + ")")
    public ResponseEntity<FormulaProductoResponse> obtenerFormulaActiva(@RequestParam Long productoId,
                                                                        @RequestParam(defaultValue = "1") BigDecimal cantidad) {
        // LÍNEA CODEx: endpoint consultado por Producción para validar disponibilidad de insumos
        return ResponseEntity.ok(formulaService.obtenerFormulaActivaPorProducto(productoId, cantidad));
    }

    @GetMapping("/producto/{productoId}/formula-activa")
    @PreAuthorize("hasAnyAuthority('BOM_READ','BOM_FORMULA_READ'," + BOM_READ_ROLE_FALLBACK + ")")
    public ResponseEntity<FormulaActivaProduccionDTO> obtenerFormulaActivaProduccion(@PathVariable Long productoId) {
        return ResponseEntity.ok(formulaService.obtenerFormulaActivaProduccion(productoId));
    }
}
