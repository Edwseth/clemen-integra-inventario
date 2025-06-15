package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.model.enums.*;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/almacenes")
@RequiredArgsConstructor
public class AlmacenController {

    private static final Logger log = LoggerFactory.getLogger(AlmacenController.class);

    private final AlmacenRepository almacenRepository;

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
    public ResponseEntity<List<AlmacenResponseDTO>> buscarPorTipoYCategoria(
            @RequestParam(required = false) TipoAlmacen tipo,
            @RequestParam(required = false) TipoCategoria categoria) {
        List<Almacen> resultados;
        if (tipo != null && categoria != null) {
            resultados = almacenRepository.findByTipoAndCategoria(tipo, categoria);
        } else {
            resultados = almacenRepository.findAll();
        }
        return ResponseEntity.ok(resultados.stream()
                .map(AlmacenResponseDTO::new)
                .collect(Collectors.toList()));
    }
}

