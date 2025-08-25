package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.service.AlmacenService;
import com.willyes.clemenintegra.inventario.model.enums.*;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.willyes.clemenintegra.shared.util.PaginationUtil;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/almacenes")
@RequiredArgsConstructor
public class AlmacenController {

    private static final Logger log = LoggerFactory.getLogger(AlmacenController.class);

    private final AlmacenRepository almacenRepository;
    private final AlmacenService almacenService;

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody AlmacenRequestDTO dto) {
        try {
            if (almacenRepository.existsByNombre(dto.getNombre())) {
                throw new EntityExistsException("Ya existe un almacén con ese nombre.");
            }
            Almacen almacen = Almacen.builder()
                    .nombre(dto.getNombre())
                    .ubicacion(dto.getUbicacion())
                    .categoria(dto.getCategoria())
                    .tipo(dto.getTipo())
                    .build();
            return ResponseEntity.status(201).body(new AlmacenResponseDTO(almacenRepository.save(almacen)));
        } catch (EntityExistsException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al crear almacén", e);
            return ResponseEntity.status(500).body("Error inesperado: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Page<AlmacenResponseDTO>> buscarPorTipoYCategoria(
            @RequestParam(required = false) TipoAlmacen tipo,
            @RequestParam(required = false) TipoCategoria categoria,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            return ResponseEntity.badRequest().build();
        }
        Pageable sanitized = PaginationUtil.sanitize(pageable, List.of("id", "nombre"), "id");
        Page<AlmacenResponseDTO> page = almacenService.buscarPorTipoYCategoria(tipo, categoria, sanitized);
        return ResponseEntity.ok(page);
    }
}

