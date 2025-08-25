package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.MotivoMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.service.MotivoMovimientoService;
import com.willyes.clemenintegra.shared.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/motivos")
@RequiredArgsConstructor
public class MotivoMovimientoController {

    private final MotivoMovimientoService service;

    @GetMapping
    public Page<MotivoMovimientoResponseDTO> listar(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        Pageable sanitized = PaginationUtil.sanitize(pageable, List.of("id"), "id");
        return service.listar(sanitized);
    }
}

