package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.NoConformidadDTO;
import com.willyes.clemenintegra.calidad.mapper.NoConformidadMapper;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.repository.NoConformidadRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoConformidadServiceImpl implements NoConformidadService {

    private final NoConformidadRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final NoConformidadMapper mapper;

    public List<NoConformidadDTO> listarTodos() {
        return repository.findAll()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public NoConformidadDTO crear(NoConformidadDTO dto) {
        if (repository.existsByCodigo(dto.getCodigo())) {
            throw new IllegalArgumentException("Ya existe una no conformidad con código: " + dto.getCodigo());
        }
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioReportaId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getUsuarioReportaId()));
        NoConformidad entity = mapper.toEntity(dto, usuario);
        return mapper.toDTO(repository.save(entity));
    }

    public NoConformidadDTO actualizar(Long id, NoConformidadDTO dto) {
        NoConformidad existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No conformidad no encontrada con ID: " + id));
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioReportaId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getUsuarioReportaId()));
        existing.setCodigo(dto.getCodigo());
        existing.setOrigen(dto.getOrigen());
        existing.setSeveridad(dto.getSeveridad());
        existing.setDescripcion(dto.getDescripcion());
        existing.setEvidencia(dto.getEvidencia());
        existing.setFechaRegistro(dto.getFechaRegistro());
        existing.setUsuarioReporta(usuario);
        return mapper.toDTO(repository.save(existing));
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    public NoConformidadDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("No conformidad no encontrada con ID: " + id));
    }
}

