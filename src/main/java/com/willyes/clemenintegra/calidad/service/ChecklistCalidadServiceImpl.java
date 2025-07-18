package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ChecklistCalidadDTO;
import com.willyes.clemenintegra.calidad.mapper.ChecklistCalidadMapper;
import com.willyes.clemenintegra.calidad.model.ChecklistCalidad;
import com.willyes.clemenintegra.calidad.model.enums.TipoChecklist;
import com.willyes.clemenintegra.calidad.repository.ChecklistCalidadRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ChecklistCalidadServiceImpl implements ChecklistCalidadService {

    private final ChecklistCalidadRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final ChecklistCalidadMapper mapper;

    public Page<ChecklistCalidadDTO> listar(TipoChecklist tipo, Pageable pageable) {
        Page<ChecklistCalidad> page = (tipo != null)
                ? repository.findByTipoChecklist(tipo, pageable)
                : repository.findAll(pageable);
        return page.map(mapper::toDTO);
    }

    public ChecklistCalidadDTO crear(ChecklistCalidadDTO dto) {
        Usuario user = usuarioRepository.findById(dto.getCreadoPorId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getCreadoPorId()));
        ChecklistCalidad entity = mapper.toEntity(dto, user);
        ChecklistCalidad saved = repository.save(entity);
        return mapper.toDTO(saved);
    }


    public ChecklistCalidadDTO actualizar(Long id, ChecklistCalidadDTO dto) {
        ChecklistCalidad existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Checklist no encontrada con ID: " + id));
        Usuario user = usuarioRepository.findById(dto.getCreadoPorId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getCreadoPorId()));
        existing.setTipoChecklist(dto.getTipoChecklist());
        existing.setFechaCreacion(dto.getFechaCreacion());
        existing.setDescripcionGeneral(dto.getDescripcionGeneral());
        existing.setCreadoPor(user);
        return mapper.toDTO(repository.save(existing));
    }

    public ChecklistCalidadDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("Checklist no encontrada con ID: " + id));
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}

