package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.dto.*;
import com.willyes.clemenintegra.produccion.mapper.EtapaPlantillaMapper;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.service.EtapaPlantillaService;
import com.willyes.clemenintegra.inventario.model.Producto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produccion/plantillas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
public class EtapaPlantillaController {

    private final EtapaPlantillaService service;
    private final EtapaPlantillaMapper mapper;

    @GetMapping("/{productoId}")
    public List<EtapaPlantillaResponse> listar(@PathVariable Integer productoId) {
        return service.listarPorProducto(productoId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @PostMapping("/{productoId}")
    public ResponseEntity<EtapaPlantillaResponse> crear(@PathVariable Integer productoId,
                                                         @RequestBody EtapaPlantillaRequest request) {
        Producto producto = new Producto();
        producto.setId(productoId);
        EtapaPlantilla entidad = mapper.toEntity(request);
        entidad.setProducto(producto);
        return ResponseEntity.ok(mapper.toResponse(service.crear(entidad)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EtapaPlantillaResponse> actualizar(@PathVariable Long id,
                                                             @RequestBody EtapaPlantillaRequest request) {
        EtapaPlantilla entidad = mapper.toEntity(request);
        return ResponseEntity.ok(mapper.toResponse(service.actualizar(id, entidad)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{productoId}/reordenar")
    public ResponseEntity<Void> reordenar(@PathVariable Integer productoId,
                                          @RequestBody List<EtapaPlantillaReordenRequest> cambios) {
        service.reordenar(productoId, cambios);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{productoId}/preview")
    public List<EtapaPlantillaResponse> preview(@PathVariable Integer productoId) {
        return service.preview(productoId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
