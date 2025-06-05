package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.mapper.HistorialEstadoOrdenMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.service.HistorialEstadoOrdenService;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventario/historial-ordenes")
public class HistorialEstadoOrdenController {

    @Autowired private HistorialEstadoOrdenService service;

    @GetMapping
    public List<HistorialEstadoOrdenResponse> listarTodos() {
        return service.listarTodos().stream()
                .map(HistorialEstadoOrdenMapper::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<HistorialEstadoOrdenResponse> crear(@RequestBody HistorialEstadoOrdenRequest request) {
        OrdenCompra orden = new OrdenCompra(); orden.setId(request.ordenCompraId);
        Usuario usuario = new Usuario(); usuario.setId(request.usuarioId);
        HistorialEstadoOrden entidad = HistorialEstadoOrdenMapper.toEntity(request, orden, usuario);
        return ResponseEntity.ok(HistorialEstadoOrdenMapper.toResponse(service.guardar(entidad)));
    }
}

