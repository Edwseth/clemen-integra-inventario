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
    private final ChecklistEtapaTemplateService checklistTemplateService;

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
        EtapaPlantilla guardada = repository.save(etapa);
        checklistTemplateService.crearPlaceholderPorDefectoSiNoExiste(guardada);
        return guardada;
    }

    @Override
    public EtapaPlantilla actualizar(Long id, EtapaPlantilla etapa) {
        EtapaPlantilla existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa no encontrada"));
        existente.setNombre(etapa.getNombre());
        existente.setSecuencia(etapa.getSecuencia());
        existente.setActivo(etapa.getActivo());
        EtapaPlantilla guardada = repository.save(existente);
        checklistTemplateService.crearPlaceholderPorDefectoSiNoExiste(guardada);
        return guardada;
    }

    @Override
    public void eliminar(Long id) {
        repository.findById(id).ifPresent(etapa -> {
            checklistTemplateService.eliminarPorEtapaPlantilla(etapa.getId());
            repository.delete(etapa);
        });
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
