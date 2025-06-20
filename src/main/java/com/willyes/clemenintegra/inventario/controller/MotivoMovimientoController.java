package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.MotivoMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MotivoMovimientoMapper;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/motivos")
@RequiredArgsConstructor
public class MotivoMovimientoController {

    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final MotivoMovimientoMapper motivoMovimientoMapper;

    @GetMapping
    public List<MotivoMovimientoResponseDTO> listar() {
        return motivoMovimientoRepository.findAll().stream()
                .map(motivoMovimientoMapper::toDTO)
                .collect(Collectors.toList());
    }
}

