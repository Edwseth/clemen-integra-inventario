package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.ProveedorRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ProveedorResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.ProveedorMapper;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import com.willyes.clemenintegra.inventario.proveedor.dto.ProveedorAutocompleteDTO;
import com.willyes.clemenintegra.inventario.repository.ProveedorRepository;
import com.willyes.clemenintegra.inventario.service.ProveedorService;
import com.willyes.clemenintegra.shared.dto.ErrorResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorRepository proveedorRepository;
    private final ProveedorMapper proveedorMapper;
    private final ProveedorService proveedorService;

    @Operation(summary = "Crear un nuevo proveedor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Proveedor creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Proveedor.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Proveedor duplicado (identificación o correo ya registrado)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<Proveedor> crear(@RequestBody @Valid ProveedorRequestDTO dto) {
        if (proveedorRepository.existsByIdentificacion(dto.getIdentificacion()) ||
                proveedorRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un proveedor con esa identificación o correo");
        }

        Proveedor proveedor = proveedorMapper.toEntity(dto);
        proveedorRepository.save(proveedor);
        return ResponseEntity.status(HttpStatus.CREATED).body(proveedor);
    }

    @GetMapping
    public ResponseEntity<Page<ProveedorResponseDTO>> listar(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(proveedorService.listar(pageable));
    }

    @GetMapping("/autocomplete")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ')")
    public ResponseEntity<Page<ProveedorAutocompleteDTO>> autocomplete(
            @RequestParam("term") String term,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        String sanitizedTerm = term != null ? term.trim() : "";
        if (sanitizedTerm.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El término de búsqueda debe tener al menos 2 caracteres");
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
        return ResponseEntity.ok(proveedorService.buscarAutocomplete(sanitizedTerm, pageable));
    }

}

