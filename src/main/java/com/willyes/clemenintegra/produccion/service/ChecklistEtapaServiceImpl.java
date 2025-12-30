package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.ChecklistEtapaDTO;
import com.willyes.clemenintegra.produccion.dto.ChecklistItemDTO;
import com.willyes.clemenintegra.produccion.model.ChecklistEtapaItem;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.repository.ChecklistEtapaItemRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChecklistEtapaServiceImpl implements ChecklistEtapaService {

    private final ChecklistEtapaItemRepository repository;
    private final EtapaProduccionRepository etapaProduccionRepository;
    private final UsuarioService usuarioService;

    @Override
    @Transactional(readOnly = true)
    public ChecklistEtapaDTO obtenerPorEtapa(Long etapaId) {
        EtapaProduccion etapa = etapaProduccionRepository.findById(etapaId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "ETAPA_NO_ENCONTRADA"));
        List<ChecklistEtapaItem> items = repository.findByEtapaProduccionIdOrderByIdAsc(etapaId);
        return buildDto(etapa, items);
    }

    @Override
    @Transactional
    public ChecklistEtapaDTO actualizar(Long etapaId, List<ChecklistItemDTO> itemsDto) {
        EtapaProduccion etapa = etapaProduccionRepository.findById(etapaId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "ETAPA_NO_ENCONTRADA"));
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        repository.deleteByEtapaProduccionId(etapaId);
        List<ChecklistEtapaItem> items = (itemsDto != null ? itemsDto : List.<ChecklistItemDTO>of()).stream()
                .map(dto -> toEntity(dto, etapa, usuario))
                .sorted(Comparator.comparing(ChecklistEtapaItem::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
        List<ChecklistEtapaItem> guardados = repository.saveAll(items);
        return buildDto(etapa, guardados);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsvPorOrden(Long ordenId) {
        List<EtapaProduccion> etapas = etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(ordenId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String header = "ordenId,etapaId,etapaNombre,paso,obligatorio,completado,observacion,completedAt,completedBy\n";
        try {
            out.write(header.getBytes(StandardCharsets.UTF_8));
            for (EtapaProduccion etapa : etapas) {
                List<ChecklistEtapaItem> items = repository.findByEtapaProduccionIdOrderByIdAsc(etapa.getId());
                for (ChecklistEtapaItem item : items) {
                    String line = String.join(",",
                            safe(ordenId),
                            safe(etapa.getId()),
                            csv(etapa.getNombre()),
                            csv(item.getNombrePaso()),
                            safeBool(item.getObligatorio()),
                            safeBool(item.getCompletado()),
                            csv(item.getObservacion()),
                            safe(item.getCompletedAt()),
                            csv(item.getCompletedBy() != null ? item.getCompletedBy().getNombreCompleto() : null)
                    );
                    out.write((line + "\\n").getBytes(StandardCharsets.UTF_8));
                }
            }
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error exportando checklist de OP {}", ordenId, e);
            throw new CustomBusinessException(ApiErrorCode.ERROR_INTERNO, "ERROR_EXPORTAR_CHECKLIST", Map.of("ordenId", ordenId));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validarChecklistCompleto(Long etapaId) {
        List<ChecklistEtapaItem> items = repository.findByEtapaProduccionIdOrderByIdAsc(etapaId);
        List<String> faltantes = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getObligatorio()))
                .filter(i -> !Boolean.TRUE.equals(i.getCompletado()))
                .map(ChecklistEtapaItem::getNombrePaso)
                .toList();
        if (!faltantes.isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.CHECKLIST_ETAPA_INCOMPLETO,
                    "CHECKLIST_ETAPA_INCOMPLETO",
                    Map.of("etapaId", etapaId, "faltantes", faltantes));
        }
    }

    private ChecklistEtapaDTO buildDto(EtapaProduccion etapa, List<ChecklistEtapaItem> items) {
        List<ChecklistItemDTO> dtoItems = items.stream()
                .map(this::toDto)
                .toList();
        long faltantes = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getObligatorio()))
                .filter(i -> !Boolean.TRUE.equals(i.getCompletado()))
                .count();
        boolean completo = faltantes == 0 && !items.isEmpty();
        return ChecklistEtapaDTO.builder()
                .etapaId(etapa.getId())
                .ordenProduccionId(etapa.getOrdenProduccion() != null ? etapa.getOrdenProduccion().getId() : null)
                .items(dtoItems)
                .completo(completo)
                .faltantesObligatorios((int) faltantes)
                .build();
    }

    private ChecklistEtapaItem toEntity(ChecklistItemDTO dto, EtapaProduccion etapa, Usuario usuario) {
        LocalDateTime completedAt = Boolean.TRUE.equals(dto.getCompletado()) ? LocalDateTime.now() : null;
        return ChecklistEtapaItem.builder()
                .id(dto.getId())
                .etapaProduccion(etapa)
                .nombrePaso(dto.getNombrePaso())
                .obligatorio(Boolean.TRUE.equals(dto.getObligatorio()))
                .completado(Boolean.TRUE.equals(dto.getCompletado()))
                .observacion(dto.getObservacion())
                .completedAt(completedAt)
                .completedBy(Boolean.TRUE.equals(dto.getCompletado()) ? usuario : null)
                .createdBy(usuario)
                .updatedBy(usuario)
                .build();
    }

    private ChecklistItemDTO toDto(ChecklistEtapaItem item) {
        return ChecklistItemDTO.builder()
                .id(item.getId())
                .nombrePaso(item.getNombrePaso())
                .obligatorio(item.getObligatorio())
                .completado(item.getCompletado())
                .observacion(item.getObservacion())
                .completedAt(item.getCompletedAt())
                .completedByNombre(item.getCompletedBy() != null ? item.getCompletedBy().getNombreCompleto() : null)
                .build();
    }

    private String csv(String value) {
        if (!StringUtils.hasText(value)) {
            return \"\";
        }
        return \\\"\\\" + value.replace(\\\"\\\\\\\", \\\"\\\\\\\\\\\").replace(\\\"\\\"\\\", \\\"\\\"\\\"\\\") + \\\"\\\"\\\";
    }

    private String safe(Object value) {
        return value != null ? value.toString() : \"\";
    }

    private String safeBool(Boolean value) {
        return Boolean.TRUE.equals(value) ? \"true\" : \"false\";
    }
}
