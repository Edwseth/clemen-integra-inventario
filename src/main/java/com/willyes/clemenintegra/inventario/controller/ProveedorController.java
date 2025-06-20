package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.ProveedorRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ProveedorResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.ProveedorMapper;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import com.willyes.clemenintegra.inventario.repository.ProveedorRepository;
import com.willyes.clemenintegra.shared.dto.ErrorResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorRepository proveedorRepository;
    private final ProveedorMapper proveedorMapper;

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
    public List<ProveedorResponseDTO> listar() {
        return proveedorRepository.findAll().stream()
                .map(proveedorMapper::toDTO)
                .collect(Collectors.toList());
    }

}

