package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.CondicionUsoCreateDTO;
import com.willyes.clemenintegra.calidad.dto.CondicionUsoResponseDTO;
import com.willyes.clemenintegra.calidad.mapper.CondicionUsoMapper;
import com.willyes.clemenintegra.calidad.model.CondicionUso;
import com.willyes.clemenintegra.calidad.model.enums.EstadoCondicionUso;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.calidad.repository.CondicionUsoRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CondicionUsoServiceImpl implements CondicionUsoService {

    private final CondicionUsoRepository repository;
    private final LoteProductoRepository loteProductoRepository;
    private final CondicionUsoMapper mapper;

    @Override
    @Transactional
    public CondicionUso create(CondicionUsoCreateDTO dto, Usuario usuarioAutenticado) {
        if (usuarioAutenticado == null || usuarioAutenticado.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USUARIO_NO_AUTENTICADO");
        }
        Long loteId = dto.getLoteId();
        CondicionUso existente = repository
                .findFirstByLote_IdAndEstadoAndTipo(loteId, EstadoCondicionUso.ACTIVA, dto.getTipo())
                .orElse(null);
        if (existente != null) {
            return existente;
        }

        var lote = loteProductoRepository.findById(loteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LOTE_NO_ENCONTRADO"));

        CondicionUso nueva = mapper.toEntity(dto, lote, usuarioAutenticado.getId());
        nueva.setCreadoEn(LocalDateTime.now());
        return repository.save(nueva);
    }

    @Override
    @Transactional
    public CondicionUso levantar(Long condicionId, Usuario usuarioAutenticado) {
        if (usuarioAutenticado == null || usuarioAutenticado.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USUARIO_NO_AUTENTICADO");
        }
        CondicionUso condicion = repository.findById(condicionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CONDICION_USO_NO_ENCONTRADA"));
        if (condicion.getEstado() == EstadoCondicionUso.LEVANTADA) {
            return condicion;
        }
        condicion.setEstado(EstadoCondicionUso.LEVANTADA);
        condicion.setActualizadoPor(usuarioAutenticado.getId());
        condicion.setActualizadoEn(LocalDateTime.now());
        return repository.save(condicion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CondicionUsoResponseDTO> getActivasByLote(Long loteId) {
        var entidades = repository.findByLote_IdAndEstado(loteId, EstadoCondicionUso.ACTIVA);
        if (entidades == null || entidades.isEmpty()) return java.util.Collections.emptyList();
        return entidades.stream().map(mapper::toResponseDTO).toList();
    }
}
