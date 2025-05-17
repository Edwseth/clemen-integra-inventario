package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.CapaDTO;
import com.willyes.clemenintegra.calidad.mapper.CapaMapper;
import com.willyes.clemenintegra.calidad.model.Capa;
import com.willyes.clemenintegra.calidad.repository.*;
import com.willyes.clemenintegra.inventario.model.Usuario;
import com.willyes.clemenintegra.inventario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CapaService {

    private final CapaRepository capaRepository;
    private final NoConformidadRepository noConformidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final CapaMapper mapper;

    public List<CapaDTO> listarTodos() {
        return capaRepository.findAll()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public CapaDTO crear(CapaDTO dto) {
        var nc = noConformidadRepository.findById(dto.getNoConformidadId())
                .orElseThrow(() -> new NoSuchElementException("No conformidad no encontrada con ID: " + dto.getNoConformidadId()));
        var user = usuarioRepository.findById(dto.getResponsableId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getResponsableId()));
        Capa entity = mapper.toEntity(dto, nc, user);
        return mapper.toDTO(capaRepository.save(entity));
    }

    public CapaDTO actualizar(Long id, CapaDTO dto) {
        Capa existing = capaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("CAPA no encontrada con ID: " + id));
        var nc = noConformidadRepository.findById(dto.getNoConformidadId())
                .orElseThrow(() -> new NoSuchElementException("No conformidad no encontrada con ID: " + dto.getNoConformidadId()));
        var user = usuarioRepository.findById(dto.getResponsableId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getResponsableId()));
        existing.setNoConformidad(nc);
        existing.setTipo(dto.getTipo());
        existing.setResponsable(user);
        existing.setFechaInicio(dto.getFechaInicio());
        existing.setFechaCierre(dto.getFechaCierre());
        existing.setEstado(dto.getEstado());
        existing.setObservaciones(dto.getObservaciones());
        return mapper.toDTO(capaRepository.save(existing));
    }

    public CapaDTO obtenerPorId(Long id) {
        return capaRepository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("CAPA no encontrada con ID: " + id));
    }

    public void eliminar(Long id) {
        capaRepository.deleteById(id);
    }
}

