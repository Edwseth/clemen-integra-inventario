package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.CapaDTO;
import com.willyes.clemenintegra.calidad.mapper.CapaMapper;
import com.willyes.clemenintegra.calidad.model.Capa;
import com.willyes.clemenintegra.calidad.model.enums.EstadoCapa;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.calidad.repository.*;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CapaServiceImpl implements CapaService {

    private final CapaRepository capaRepository;
    private final NoConformidadRepository noConformidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final CapaMapper mapper;

    public Page<CapaDTO> listar(EstadoCapa estado, SeveridadNoConformidad severidad, Pageable pageable) {
        Page<Capa> page;
        if (estado != null && severidad != null) {
            page = capaRepository.findByNoConformidad_SeveridadAndEstado(severidad, estado, pageable);
        } else if (estado != null) {
            page = capaRepository.findByEstado(estado, pageable);
        } else if (severidad != null) {
            page = capaRepository.findByNoConformidad_Severidad(severidad, pageable);
        } else {
            page = capaRepository.findAll(pageable);
        }
        return page.map(mapper::toDTO);
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

