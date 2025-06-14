package com.willyes.clemenintegra.inventario.application.service;

import com.willyes.clemenintegra.inventario.application.dto.UnidadMedidaRequestDTO;
import com.willyes.clemenintegra.inventario.application.dto.UnidadMedidaResponseDTO;
import com.willyes.clemenintegra.inventario.application.mapper.UnidadMedidaMapper;
import com.willyes.clemenintegra.inventario.domain.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.domain.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.domain.repository.UnidadMedidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnidadMedidaService {

    private final UnidadMedidaRepository unidadMedidaRepository;
    private final ProductoRepository productoRepository;
    private final UnidadMedidaMapper mapper;

    public List<UnidadMedidaResponseDTO> listarTodas() {
        return unidadMedidaRepository.findAll()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public UnidadMedidaResponseDTO obtenerPorId(Long id) {
        UnidadMedida unidad = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidad de medida no encontrada"));
        return mapper.toDTO(unidad);
    }

    public UnidadMedidaResponseDTO crear(UnidadMedidaRequestDTO dto) {
        UnidadMedida unidad = mapper.toEntity(dto);

        unidadMedidaRepository.save(unidad);
        return mapper.toDTO(unidad);
    }

    public UnidadMedidaResponseDTO actualizar(Long id, UnidadMedidaRequestDTO dto) {
        UnidadMedida unidad = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidad no encontrada"));

        mapper.updateEntityFromDto(dto, unidad);

        unidadMedidaRepository.save(unidad);
        return mapper.toDTO(unidad);
    }

    public void eliminar(Long id) {
        UnidadMedida unidad = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidad no encontrada"));

        if (productoRepository.existsByUnidadMedida(unidad)) {
            throw new IllegalStateException("No se puede eliminar: está en uso por productos.");
        }

        unidadMedidaRepository.delete(unidad);
    }
}

