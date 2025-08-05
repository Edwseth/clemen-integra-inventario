package com.willyes.clemenintegra.bom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.bom.dto.*;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.*;
import com.willyes.clemenintegra.bom.service.*;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bom/formulas")
@RequiredArgsConstructor
public class FormulaProductoController {

    private final FormulaProductoService formulaService;
    private final BomMapper bomMapper;

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
        FormulaProductoRequest request = new ObjectMapper().readValue(formulaJson, FormulaProductoRequest.class);
        Producto producto = new Producto(); producto.setId(request.productoId.intValue());
        Usuario creador = new Usuario(); creador.setId(request.creadoPorId);
        FormulaProducto entidad = bomMapper.toEntity(request, producto, creador);
        return ResponseEntity.ok(bomMapper.toResponse(formulaService.guardar(entidad)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<FormulaProductoResponse> actualizar(@PathVariable Long id, @RequestBody FormulaProductoRequest request) {
        return formulaService.buscarPorId(id)
                .map(existente -> {
                    Producto producto = new Producto(); producto.setId(request.productoId.intValue());
                    Usuario creador = new Usuario(); creador.setId(request.creadoPorId);
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

