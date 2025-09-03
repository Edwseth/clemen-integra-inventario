package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.EtapaPlantillaReordenRequest;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.repository.EtapaPlantillaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EtapaPlantillaServiceImpl implements EtapaPlantillaService {

    private final EtapaPlantillaRepository repository;

    private void validarUnicidad(Integer productoId, String nombre, Integer secuencia, Long id) {
        if (id == null) {
            if (repository.existsByProductoIdAndSecuencia(productoId, secuencia)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Secuencia ya usada");
            }
            if (repository.existsByProductoIdAndNombreIgnoreCase(productoId, nombre)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Nombre ya usado");
            }
        } else {
            if (repository.existsByProductoIdAndSecuenciaAndIdNot(productoId, secuencia, id)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Secuencia ya usada");
            }
            if (repository.existsByProductoIdAndNombreIgnoreCaseAndIdNot(productoId, nombre, id)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Nombre ya usado");
            }
        }
    }

    @Override
    public List<EtapaPlantilla> listarPorProducto(Integer productoId) {
        return repository.findByProductoIdOrderBySecuenciaAsc(productoId);
    }

    @Override
    public List<EtapaPlantilla> preview(Integer productoId) {
        return repository.findByProductoIdAndActivoTrueOrderBySecuenciaAsc(productoId);
    }

    @Override
    public EtapaPlantilla crear(EtapaPlantilla etapa) {
        Integer productoId = etapa.getProducto().getId();
        validarUnicidad(productoId, etapa.getNombre(), etapa.getSecuencia(), null);
        return repository.save(etapa);
    }

    @Override
    public EtapaPlantilla actualizar(Long id, EtapaPlantilla etapa) {
        EtapaPlantilla existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa no encontrada"));
        Integer productoId = existente.getProducto().getId();
        validarUnicidad(productoId, etapa.getNombre(), etapa.getSecuencia(), id);
        existente.setNombre(etapa.getNombre());
        existente.setSecuencia(etapa.getSecuencia());
        existente.setActivo(etapa.getActivo());
        return repository.save(existente);
    }

    @Override
    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public void reordenar(Integer productoId, List<EtapaPlantillaReordenRequest> cambios) {
        List<EtapaPlantilla> existentes = repository.findByProductoIdOrderBySecuenciaAsc(productoId);
        Map<Long, EtapaPlantilla> mapa = existentes.stream()
                .collect(Collectors.toMap(EtapaPlantilla::getId, Function.identity()));
        Map<Long, Integer> originales = existentes.stream()
                .collect(Collectors.toMap(EtapaPlantilla::getId, EtapaPlantilla::getSecuencia));

        for (EtapaPlantillaReordenRequest c : cambios) {
            EtapaPlantilla e = mapa.get(c.id);
            if (e == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa no encontrada");
            }
            e.setSecuencia(c.secuencia);
        }

        Set<Integer> secuencias = new HashSet<>();
        for (EtapaPlantilla e : existentes) {
            if (!secuencias.add(e.getSecuencia())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Secuencia duplicada");
            }
        }

        List<EtapaPlantilla> modificadas = existentes.stream()
                .filter(e -> !Objects.equals(originales.get(e.getId()), e.getSecuencia()))
                .toList();
        repository.saveAll(modificadas);
    }
}
