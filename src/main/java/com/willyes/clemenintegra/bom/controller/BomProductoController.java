package com.willyes.clemenintegra.bom.controller;

import com.willyes.clemenintegra.bom.dto.ProductoMinResponseDTO;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bom")
@RequiredArgsConstructor
public class BomProductoController {

    private final ProductoRepository productoRepository;

    @GetMapping("/productos-terminados")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<List<ProductoMinResponseDTO>> listarProductosTerminados() {
        List<Producto> items = productoRepository
                .findByCategoriaProducto_TipoAndActivoTrueOrderByNombreAsc(TipoCategoria.PRODUCTO_TERMINADO);

        List<ProductoMinResponseDTO> dto = items.stream()
                .map(p -> new ProductoMinResponseDTO(p.getId(), p.getCodigoSku(), p.getNombre()))
                .toList();

        return ResponseEntity.ok(dto);
    }
}

