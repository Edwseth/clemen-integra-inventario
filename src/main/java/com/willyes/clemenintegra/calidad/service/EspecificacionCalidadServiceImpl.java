package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.EspecificacionCalidadDTO;
import com.willyes.clemenintegra.calidad.mapper.EspecificacionCalidadMapper;
import com.willyes.clemenintegra.calidad.model.EspecificacionCalidad;
import com.willyes.clemenintegra.calidad.repository.EspecificacionCalidadRepository;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EspecificacionCalidadServiceImpl implements EspecificacionCalidadService {

    private final EspecificacionCalidadRepository repository;
    private final ProductoRepository productoRepository;
    private final EspecificacionCalidadMapper mapper;

    public List<EspecificacionCalidadDTO> listarTodos() {
        return repository.findAll().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public EspecificacionCalidadDTO crear(EspecificacionCalidadDTO dto) {
        Producto prod = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado con ID: " + dto.getProductoId()));
        EspecificacionCalidad entidad = mapper.toEntity(dto, prod);
        return mapper.toDTO(repository.save(entidad));
    }

    public EspecificacionCalidadDTO actualizar(Long id, EspecificacionCalidadDTO dto) {
        EspecificacionCalidad existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Especificación no encontrada con ID: " + id));
        Producto prod = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado con ID: " + dto.getProductoId()));
        existing.setParametro(dto.getParametro());
        existing.setValorMinimo(dto.getValorMinimo());
        existing.setValorMaximo(dto.getValorMaximo());
        existing.setMetodoEnsayo(dto.getMetodoEnsayo());
        existing.setProducto(prod);
        return mapper.toDTO(repository.save(existing));
    }

    public EspecificacionCalidadDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("Especificación no encontrada con ID: " + id));
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}

