package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.ChecklistEtapaTemplateRequest;
import com.willyes.clemenintegra.produccion.dto.ChecklistEtapaTemplateResponse;
import com.willyes.clemenintegra.produccion.model.ChecklistEtapaTemplate;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.repository.ChecklistEtapaTemplateRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaPlantillaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChecklistEtapaTemplateAdminServiceImpl implements ChecklistEtapaTemplateAdminService {

    private final ChecklistEtapaTemplateRepository repository;
    private final EtapaPlantillaRepository etapaPlantillaRepository;

    @Override
    public List<ChecklistEtapaTemplateResponse> listar(Long etapaPlantillaId) {
        EtapaPlantilla etapa = obtenerEtapa(etapaPlantillaId);
        return repository.findByEtapaPlantillaIdOrderByOrdenAsc(etapa.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ChecklistEtapaTemplateResponse crear(Long etapaPlantillaId, ChecklistEtapaTemplateRequest request) {
        EtapaPlantilla etapa = obtenerEtapa(etapaPlantillaId);
        validarRequest(request);
        ChecklistEtapaTemplate entidad = ChecklistEtapaTemplate.builder()
                .etapaPlantilla(etapa)
                .nombreItem(request.getNombreItem().trim())
                .obligatorio(Boolean.TRUE.equals(request.getObligatorio()))
                .permitirNoAplica(Boolean.TRUE.equals(request.getPermitirNoAplica()))
                .orden(request.getOrden())
                .activo(request.getActivo() == null || Boolean.TRUE.equals(request.getActivo()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return toResponse(repository.save(entidad));
    }

    @Override
    public ChecklistEtapaTemplateResponse actualizar(Long id, ChecklistEtapaTemplateRequest request) {
        validarRequest(request);
        ChecklistEtapaTemplate existente = obtenerTemplate(id);
        existente.setNombreItem(request.getNombreItem().trim());
        existente.setObligatorio(Boolean.TRUE.equals(request.getObligatorio()));
        existente.setPermitirNoAplica(Boolean.TRUE.equals(request.getPermitirNoAplica()));
        existente.setOrden(request.getOrden());
        existente.setActivo(request.getActivo() == null || Boolean.TRUE.equals(request.getActivo()));
        return toResponse(repository.save(existente));
    }

    @Override
    public void eliminar(Long id) {
        ChecklistEtapaTemplate existente = obtenerTemplate(id);
        existente.setActivo(false);
        existente.setUpdatedAt(LocalDateTime.now());
        repository.save(existente);
    }

    @Override
    public List<ChecklistEtapaTemplateResponse> copiarDesde(Long etapaPlantillaId, Long origenEtapaPlantillaId) {
        if (origenEtapaPlantillaId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Etapa plantilla origen requerida");
        }
        EtapaPlantilla destino = obtenerEtapa(etapaPlantillaId);
        EtapaPlantilla origen = obtenerEtapa(origenEtapaPlantillaId);

        boolean destinoTieneChecklist = repository.existsOperativosActivosByEtapaPlantillaId(
                destino.getId(), ChecklistEtapaTemplateService.PLACEHOLDER_NOMBRE);
        if (destinoTieneChecklist) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CHECKLIST_YA_EXISTE");
        }

        List<ChecklistEtapaTemplate> origenItems = repository.findOperativosActivosByEtapaPlantillaId(
                origen.getId(), ChecklistEtapaTemplateService.PLACEHOLDER_NOMBRE);
        if (origenItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Checklist origen vacío");
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<ChecklistEtapaTemplate> copias = origenItems.stream()
                .map(item -> ChecklistEtapaTemplate.builder()
                        .etapaPlantilla(destino)
                        .nombreItem(item.getNombreItem())
                        .obligatorio(Boolean.TRUE.equals(item.getObligatorio()))
                        .permitirNoAplica(Boolean.TRUE.equals(item.getPermitirNoAplica()))
                        .orden(item.getOrden())
                        .activo(true)
                        .createdAt(ahora)
                        .updatedAt(ahora)
                        .build())
                .toList();
        repository.saveAll(copias);
        return repository.findByEtapaPlantillaIdOrderByOrdenAsc(destino.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private ChecklistEtapaTemplateResponse toResponse(ChecklistEtapaTemplate template) {
        return ChecklistEtapaTemplateResponse.builder()
                .id(template.getId())
                .etapaPlantillaId(template.getEtapaPlantilla().getId())
                .nombreItem(template.getNombreItem())
                .obligatorio(template.getObligatorio())
                .permitirNoAplica(template.getPermitirNoAplica())
                .orden(template.getOrden())
                .activo(template.getActivo())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    private void validarRequest(ChecklistEtapaTemplateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Request inválido");
        }
        if (!StringUtils.hasText(request.getNombreItem())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Nombre de item requerido");
        }
        if (request.getOrden() == null || request.getOrden() < 1) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Orden debe ser mayor o igual a 1");
        }
    }

    private EtapaPlantilla obtenerEtapa(Long etapaPlantillaId) {
        return etapaPlantillaRepository.findById(etapaPlantillaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa plantilla no encontrada"));
    }

    private ChecklistEtapaTemplate obtenerTemplate(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Checklist template no encontrado"));
    }
}
