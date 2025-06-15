package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.RetencionLoteDTO;
import com.willyes.clemenintegra.calidad.mapper.RetencionLoteMapper;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.repository.RetencionLoteRepository;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RetencionLoteServiceImpl implements RetencionLoteService {

    private final RetencionLoteRepository repository;
    private final LoteProductoRepository loteRepository;
    private final UsuarioRepository usuarioRepository;
    private final RetencionLoteMapper mapper;

    public List<RetencionLoteDTO> listarTodos() {
        return repository.findAll()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public RetencionLoteDTO crear(RetencionLoteDTO dto) {
        LoteProducto lote = loteRepository.findById(dto.getLoteId())
                .orElseThrow(() -> new NoSuchElementException("Lote no encontrado con ID: " + dto.getLoteId()));
        Usuario user = usuarioRepository.findById(dto.getAprobadoPorId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getAprobadoPorId()));
        RetencionLote entity = mapper.toEntity(dto, lote, user);
        return mapper.toDTO(repository.save(entity));
    }

    public RetencionLoteDTO actualizar(Long id, RetencionLoteDTO dto) {
        RetencionLote existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Retención no encontrada con ID: " + id));
        LoteProducto lote = loteRepository.findById(dto.getLoteId())
                .orElseThrow(() -> new NoSuchElementException("Lote no encontrado con ID: " + dto.getLoteId()));
        Usuario user = usuarioRepository.findById(dto.getAprobadoPorId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getAprobadoPorId()));
        existing.setLote(lote);
        existing.setCausa(dto.getCausa());
        existing.setFechaRetencion(dto.getFechaRetencion());
        existing.setFechaLiberacion(dto.getFechaLiberacion());
        existing.setEstado(dto.getEstado());
        existing.setAprobadoPor(user);
        return mapper.toDTO(repository.save(existing));
    }

    public RetencionLoteDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("Retención no encontrada con ID: " + id));
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}

