package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadDTO;
import com.willyes.clemenintegra.calidad.mapper.EvaluacionCalidadMapper;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UsuarioRepository;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.shared.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluacionCalidadService {

    private final EvaluacionCalidadRepository repository;
    private final LoteProductoRepository loteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EvaluacionCalidadMapper mapper;

    public List<EvaluacionCalidadDTO> listarTodos() {
        return repository.findAll().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public EvaluacionCalidadDTO crear(EvaluacionCalidadDTO dto) {
        LoteProducto lote = loteRepository.findById(dto.getLoteProductoId())
                .orElseThrow(() -> new NoSuchElementException("Lote no encontrado con ID: " + dto.getLoteProductoId()));
        Usuario user = usuarioRepository.findById(dto.getUsuarioEvaluadorId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getUsuarioEvaluadorId()));
        EvaluacionCalidad entidad = mapper.toEntity(dto, lote, user);
        return mapper.toDTO(repository.save(entidad));
    }

    public EvaluacionCalidadDTO actualizar(Long id, EvaluacionCalidadDTO dto) {
        EvaluacionCalidad existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Evaluación no encontrada con ID: " + id));
        LoteProducto lote = loteRepository.findById(dto.getLoteProductoId())
                .orElseThrow(() -> new NoSuchElementException("Lote no encontrado con ID: " + dto.getLoteProductoId()));
        Usuario user = usuarioRepository.findById(dto.getUsuarioEvaluadorId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getUsuarioEvaluadorId()));
        existing.setResultado(dto.getResultado());
        existing.setFechaEvaluacion(dto.getFechaEvaluacion());
        existing.setObservaciones(dto.getObservaciones());
        existing.setArchivoAdjunto(dto.getArchivoAdjunto());
        existing.setLoteProducto(lote);
        existing.setUsuarioEvaluador(user);
        return mapper.toDTO(repository.save(existing));
    }

    public EvaluacionCalidadDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("Evaluación no encontrada con ID: " + id));
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}

