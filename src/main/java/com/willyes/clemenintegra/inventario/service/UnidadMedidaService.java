package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.dto.UnidadMedidaRequestDTO;
import com.willyes.clemenintegra.inventario.dto.UnidadMedidaResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnidadMedidaService {

    private final UnidadMedidaRepository unidadMedidaRepository;
    private final ProductoRepository productoRepository;

    public List<UnidadMedidaResponseDTO> listarTodas() {
        return unidadMedidaRepository.findAll()
                .stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    public UnidadMedidaResponseDTO obtenerPorId(Long id) {
        UnidadMedida unidad = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidad de medida no encontrada"));
        return mapearADTO(unidad);
    }

    public UnidadMedidaResponseDTO crear(UnidadMedidaRequestDTO dto) {
        UnidadMedida unidad = UnidadMedida.builder()
                .nombre(dto.getNombre())
                .simbolo(dto.getSimbolo())
                .build();

        unidadMedidaRepository.save(unidad);
        return mapearADTO(unidad);
    }

    public UnidadMedidaResponseDTO actualizar(Long id, UnidadMedidaRequestDTO dto) {
        UnidadMedida unidad = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidad no encontrada"));

        unidad.setNombre(dto.getNombre());
        unidad.setSimbolo(dto.getSimbolo());

        unidadMedidaRepository.save(unidad);
        return mapearADTO(unidad);
    }

    public void eliminar(Long id) {
        UnidadMedida unidad = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidad no encontrada"));

        if (productoRepository.existsByUnidadMedida(unidad)) {
            throw new IllegalStateException("No se puede eliminar: está en uso por productos.");
        }

        unidadMedidaRepository.delete(unidad);
    }

    private UnidadMedidaResponseDTO mapearADTO(UnidadMedida unidad) {
        return UnidadMedidaResponseDTO.builder()
                .id(unidad.getId())
                .nombre(unidad.getNombre())
                .simbolo(unidad.getSimbolo())
                .build();
    }
}

