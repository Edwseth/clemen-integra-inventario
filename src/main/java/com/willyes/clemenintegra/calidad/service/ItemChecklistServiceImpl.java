package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ItemChecklistDTO;
import com.willyes.clemenintegra.calidad.mapper.ItemChecklistMapper;
import com.willyes.clemenintegra.calidad.model.*;
import com.willyes.clemenintegra.calidad.repository.*;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemChecklistServiceImpl implements ItemChecklistService {

    private final ItemChecklistRepository repository;
    private final ChecklistCalidadRepository checklistRepository;
    private final UsuarioRepository usuarioRepository;
    private final ItemChecklistMapper mapper;

    public List<ItemChecklistDTO> listarTodos() {
        return repository.findAll()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public ItemChecklistDTO crear(ItemChecklistDTO dto) {
        ChecklistCalidad chk = checklistRepository.findById(dto.getChecklistId())
                .orElseThrow(() -> new NoSuchElementException("Checklist no encontrado con ID: " + dto.getChecklistId()));
        Usuario user = usuarioRepository.findById(dto.getRevisadoPorId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getRevisadoPorId()));
        ItemChecklist entity = mapper.toEntity(dto, chk, user);
        return mapper.toDTO(repository.save(entity));
    }

    public ItemChecklistDTO actualizar(Long id, ItemChecklistDTO dto) {
        ItemChecklist existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ItemChecklist no encontrado con ID: " + id));
        ChecklistCalidad chk = checklistRepository.findById(dto.getChecklistId())
                .orElseThrow(() -> new NoSuchElementException("Checklist no encontrado con ID: " + dto.getChecklistId()));
        Usuario user = usuarioRepository.findById(dto.getRevisadoPorId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getRevisadoPorId()));
        existing.setChecklist(chk);
        existing.setDescripcionItem(dto.getDescripcionItem());
        existing.setCumple(dto.getCumple());
        existing.setObservaciones(dto.getObservaciones());
        existing.setFechaRevision(dto.getFechaRevision());
        existing.setRevisadoPor(user);
        return mapper.toDTO(repository.save(existing));
    }

    public ItemChecklistDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("ItemChecklist no encontrado con ID: " + id));
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}

