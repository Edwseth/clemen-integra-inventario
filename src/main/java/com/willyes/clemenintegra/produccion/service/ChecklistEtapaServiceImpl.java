package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.ChecklistEtapaDTO;
import com.willyes.clemenintegra.produccion.dto.ChecklistItemDTO;
import com.willyes.clemenintegra.produccion.model.ChecklistEtapaItem;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoChecklistItem;
import com.willyes.clemenintegra.produccion.repository.ChecklistEtapaItemRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaPlantillaRepository;
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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChecklistEtapaServiceImpl implements ChecklistEtapaService {

    private final ChecklistEtapaItemRepository repository;
    private final EtapaProduccionRepository etapaProduccionRepository;
    private final ChecklistEtapaTemplateService templateService;
    private final EtapaPlantillaRepository etapaPlantillaRepository;
    private final UsuarioService usuarioService;

    @Override
    @Transactional(readOnly = true)
    public ChecklistEtapaDTO obtenerPorEtapa(Long etapaId) {
        EtapaProduccion etapa = obtenerEtapa(etapaId);
        List<ChecklistEtapaItem> items = repository.findByEtapaProduccionIdOrderByIdAsc(etapaId);
        if (items.isEmpty()) {
            generarChecklistDesdeTemplateSiNoExiste(etapaId);
            items = repository.findByEtapaProduccionIdOrderByIdAsc(etapaId);
        }
        return buildDto(etapa, items);
    }

    @Override
    @Transactional(readOnly = true)
    public ChecklistEtapaDTO obtenerPorOrdenYEtapa(Long ordenId, Long etapaId) {
        log.info("ChecklistEtapa - obtenerPorOrdenYEtapa ordenId={}, etapaId={}", ordenId, etapaId);
        EtapaProduccion etapa = obtenerEtapaValidada(ordenId, etapaId);
        List<ChecklistEtapaItem> items = repository.findByEtapaProduccionIdOrderByIdAsc(etapaId);
        log.debug("ChecklistEtapa - items recuperados: {}", items.size());
        return buildDto(etapa, items);
    }

    @Override
    @Transactional
    public ChecklistEtapaDTO actualizar(Long etapaId, List<ChecklistItemDTO> itemsDto) {
        EtapaProduccion etapa = obtenerEtapa(etapaId);
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        List<ChecklistEtapaItem> guardados = guardarItems(etapa, usuario, itemsDto);
        return buildDto(etapa, guardados);
    }

    @Override
    @Transactional
    public ChecklistEtapaDTO actualizarEnOrden(Long ordenId, Long etapaId, List<ChecklistItemDTO> itemsDto) {
        EtapaProduccion etapa = obtenerEtapaValidada(ordenId, etapaId);
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        List<ChecklistEtapaItem> guardados = guardarItems(etapa, usuario, itemsDto);
        return buildDto(etapa, guardados);
    }

    @Override
    @Transactional
    public void generarChecklistDesdeTemplateSiNoExiste(Long etapaProduccionId) {
        if (repository.countByEtapaProduccionId(etapaProduccionId) > 0) {
            return;
        }
        EtapaProduccion etapa = obtenerEtapa(etapaProduccionId);
        Long etapaPlantillaId = resolverEtapaPlantillaId(etapa);
        if (etapaPlantillaId == null) {
            log.warn("No se pudo resolver plantilla para etapa {}, checklist no generado", etapaProduccionId);
            return;
        }
        List<com.willyes.clemenintegra.produccion.model.ChecklistEtapaTemplate> templates =
                templateService.listarActivosPorEtapaPlantilla(etapaPlantillaId);
        if (templates.isEmpty()) {
            log.info("Etapa {} sin plantilla de checklist, no se generan items", etapaProduccionId);
            return;
        }
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        List<ChecklistEtapaItem> nuevos = templates.stream()
                .map(t -> ChecklistEtapaItem.builder()
                        .etapaProduccion(etapa)
                        .nombrePaso(t.getNombreItem())
                        .obligatorio(Boolean.TRUE.equals(t.getObligatorio()))
                        .permitirNoAplica(Boolean.TRUE.equals(t.getPermitirNoAplica()))
                        .estado(EstadoChecklistItem.PENDIENTE)
                        .completado(false)
                        .noAplica(false)
                        .createdBy(usuario)
                        .updatedBy(usuario)
                        .build())
                .toList();
        repository.saveAll(nuevos);
        log.info("Checklist generado desde plantilla para etapa {} con {} items", etapaProduccionId, nuevos.size());
    }

    @Override
    @Transactional
    public ChecklistItemDTO completarItem(Long ordenId, Long etapaId, Long itemId, String observacion) {
        ChecklistEtapaItem item = obtenerItemValidado(ordenId, etapaId, itemId);
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        item.setEstado(EstadoChecklistItem.COMPLETADO);
        item.setCompletado(true);
        item.setNoAplica(false);
        item.setObservacion(observacion);
        item.setCompletedAt(LocalDateTime.now());
        item.setCompletedBy(usuario);
        item.setUpdatedBy(usuario);
        return toDto(repository.save(item));
    }

    @Override
    @Transactional
    public ChecklistItemDTO marcarNoAplica(Long ordenId, Long etapaId, Long itemId, String observacion) {
        ChecklistEtapaItem item = obtenerItemValidado(ordenId, etapaId, itemId);
        if (!Boolean.TRUE.equals(item.getPermitirNoAplica())) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "CHECKLIST_ITEM_NO_APLICA_NO_PERMITIDO",
                    Map.of("itemId", itemId));
        }
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        item.setEstado(EstadoChecklistItem.NO_APLICA);
        item.setCompletado(false);
        item.setNoAplica(true);
        item.setObservacion(observacion);
        item.setCompletedAt(LocalDateTime.now());
        item.setCompletedBy(usuario);
        item.setUpdatedBy(usuario);
        return toDto(repository.save(item));
    }

    @Override
    @Transactional
    public ChecklistItemDTO reabrirItem(Long ordenId, Long etapaId, Long itemId) {
        ChecklistEtapaItem item = obtenerItemValidado(ordenId, etapaId, itemId);
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        item.setEstado(EstadoChecklistItem.PENDIENTE);
        item.setCompletado(false);
        item.setNoAplica(false);
        item.setCompletedAt(null);
        item.setCompletedBy(null);
        item.setUpdatedBy(usuario);
        return toDto(repository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsvPorOrden(Long ordenId) {
        List<EtapaProduccion> etapas = etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(ordenId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String header = "ordenId,etapaId,etapaNombre,paso,obligatorio,estado,noAplica,permitirNoAplica,observacion,completedAt,completedBy\n";
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
                            item.getEstado() != null ? item.getEstado().name() : "",
                            safeBool(item.getNoAplica()),
                            safeBool(item.getPermitirNoAplica()),
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
                .filter(i -> !(EstadoChecklistItem.COMPLETADO.equals(i.getEstado())
                        || (EstadoChecklistItem.NO_APLICA.equals(i.getEstado())
                        && Boolean.TRUE.equals(i.getPermitirNoAplica()))))
                .map(ChecklistEtapaItem::getNombrePaso)
                .toList();
        if (!faltantes.isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.CHECKLIST_ETAPA_INCOMPLETO,
                    "CHECKLIST_ETAPA_INCOMPLETO",
                    Map.of("etapaId", etapaId, "faltantes", faltantes));
        }
    }

    private List<ChecklistEtapaItem> guardarItems(EtapaProduccion etapa, Usuario usuario, List<ChecklistItemDTO> itemsDto) {
        repository.deleteByEtapaProduccionId(etapa.getId());
        List<ChecklistEtapaItem> items = (itemsDto != null ? itemsDto : List.<ChecklistItemDTO>of()).stream()
                .map(dto -> toEntity(dto, etapa, usuario))
                .sorted(Comparator.comparing(ChecklistEtapaItem::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
        return repository.saveAll(items);
    }

    private EtapaProduccion obtenerEtapa(Long etapaId) {
        return etapaProduccionRepository.findById(etapaId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "ETAPA_NO_ENCONTRADA"));
    }

    private EtapaProduccion obtenerEtapaValidada(Long ordenId, Long etapaId) {
        EtapaProduccion etapa = obtenerEtapa(etapaId);
        if (etapa.getOrdenProduccion() == null || !Objects.equals(etapa.getOrdenProduccion().getId(), ordenId)) {
            log.warn("ChecklistEtapa - etapa {} no pertenece a la orden {}", etapaId, ordenId);
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "ETAPA_NO_PERTENECE_A_ORDEN",
                    Map.of("ordenId", ordenId, "etapaId", etapaId));
        }
        log.debug("ChecklistEtapa - etapa {} validada para orden {}", etapaId, ordenId);
        return etapa;
    }

    private ChecklistEtapaDTO buildDto(EtapaProduccion etapa, List<ChecklistEtapaItem> items) {
        List<ChecklistItemDTO> dtoItems = items.stream()
                .map(this::toDto)
                .toList();
        long faltantes = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getObligatorio()))
                .filter(i -> !(EstadoChecklistItem.COMPLETADO.equals(i.getEstado())
                        || (EstadoChecklistItem.NO_APLICA.equals(i.getEstado())
                        && Boolean.TRUE.equals(i.getPermitirNoAplica()))))
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
        EstadoChecklistItem estado = Boolean.TRUE.equals(dto.getNoAplica())
                ? EstadoChecklistItem.NO_APLICA
                : Boolean.TRUE.equals(dto.getCompletado())
                ? EstadoChecklistItem.COMPLETADO
                : EstadoChecklistItem.PENDIENTE;
        LocalDateTime completedAt = estado != EstadoChecklistItem.PENDIENTE ? LocalDateTime.now() : null;
        return ChecklistEtapaItem.builder()
                .id(dto.getId())
                .etapaProduccion(etapa)
                .nombrePaso(dto.getNombrePaso())
                .obligatorio(Boolean.TRUE.equals(dto.getObligatorio()))
                .completado(Boolean.TRUE.equals(dto.getCompletado()))
                .noAplica(Boolean.TRUE.equals(dto.getNoAplica()))
                .permitirNoAplica(Boolean.TRUE.equals(dto.getPermitirNoAplica()))
                .estado(estado)
                .observacion(dto.getObservacion())
                .completedAt(completedAt)
                .completedBy(estado != EstadoChecklistItem.PENDIENTE ? usuario : null)
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
                .noAplica(item.getNoAplica())
                .permitirNoAplica(item.getPermitirNoAplica())
                .estado(item.getEstado() != null ? item.getEstado().name() : null)
                .observacion(item.getObservacion())
                .completedAt(item.getCompletedAt())
                .completedByNombre(item.getCompletedBy() != null ? item.getCompletedBy().getNombreCompleto() : null)
                .build();
    }

    private ChecklistEtapaItem obtenerItemValidado(Long ordenId, Long etapaId, Long itemId) {
        EtapaProduccion etapa = obtenerEtapaValidada(ordenId, etapaId);
        ChecklistEtapaItem item = repository.findById(itemId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "CHECKLIST_ITEM_NO_ENCONTRADO"));
        if (!Objects.equals(item.getEtapaProduccion() != null ? item.getEtapaProduccion().getId() : null, etapa.getId())) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "ITEM_NO_PERTENECE_A_ETAPA",
                    Map.of("etapaId", etapaId, "itemId", itemId));
        }
        return item;
    }

    private Long resolverEtapaPlantillaId(EtapaProduccion etapa) {
        if (etapa.getOrdenProduccion() == null || etapa.getOrdenProduccion().getProducto() == null
                || etapa.getOrdenProduccion().getProducto().getId() == null) {
            return null;
        }
        Integer productoId = etapa.getOrdenProduccion().getProducto().getId();
        Optional<com.willyes.clemenintegra.produccion.model.EtapaPlantilla> plantillaOpt =
                etapaPlantillaRepository.findFirstByProductoIdAndNombreIgnoreCase(productoId, etapa.getNombre());
        return plantillaOpt.map(com.willyes.clemenintegra.produccion.model.EtapaPlantilla::getId).orElse(null);
    }

    private String csv(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        // CSV estándar: escapar comillas dobles duplicándolas
        String escaped = value.replace("\"", "\"\"");
        // Encapsular siempre con comillas para evitar problemas con comas/saltos de línea
        return "\"" + escaped + "\"";
    }

    private String safe(Object value) {
        return value != null ? value.toString() : "";
    }

    private String safeBool(Boolean value) {
        return Boolean.TRUE.equals(value) ? "true" : "false";
    }
}
