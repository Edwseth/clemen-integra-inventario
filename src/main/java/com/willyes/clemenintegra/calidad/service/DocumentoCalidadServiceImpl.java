package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadCreateRequest;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadDTO;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadResumenDTO;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadVersionDTO;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadVersionDownloadDTO;
import com.willyes.clemenintegra.calidad.mapper.DocumentoCalidadMapper;
import com.willyes.clemenintegra.calidad.model.DocumentoCalidad;
import com.willyes.clemenintegra.calidad.model.DocumentoCalidadVersion;
import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadEstado;
import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadTipo;
import com.willyes.clemenintegra.calidad.repository.DocumentoCalidadRepository;
import com.willyes.clemenintegra.calidad.repository.DocumentoCalidadVersionRepository;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DocumentoCalidadServiceImpl implements DocumentoCalidadService {

    private final DocumentoCalidadRepository documentoRepository;
    private final DocumentoCalidadVersionRepository versionRepository;
    private final LoteProductoRepository loteProductoRepository;

    @Value("${app.calidad.documentos.upload-dir:${user.dir}/uploads/documentos-calidad}")
    private String uploadDir;

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentoCalidadDTO> listar(DocumentoCalidadTipo tipo, DocumentoCalidadEstado estado, String q, Pageable pageable) {
        return documentoRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tipo != null) {
                predicates.add(cb.equal(root.get("tipo"), tipo));
            }
            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("codigo")), like),
                        cb.like(cb.lower(root.get("nombre")), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable).map(DocumentoCalidadMapper::toDTO);
    }

    @Override
    @Transactional
    public DocumentoCalidadDTO crear(DocumentoCalidadCreateRequest request, Long usuarioId) {
        if (request == null || request.getTipo() == null || request.getNombre() == null || request.getNombre().isBlank()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "Tipo y nombre son obligatorios para crear un documento.");
        }

        LoteProducto lote = null;
        if (request.getLoteId() != null) {
            lote = loteProductoRepository.findById(request.getLoteId())
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                            "Lote no encontrado para asociar documento.",
                            Map.of("loteId", request.getLoteId())));
        }

        DocumentoCalidad documento = DocumentoCalidad.builder()
                .tipo(request.getTipo())
                .codigo(request.getCodigo())
                .nombre(request.getNombre().trim())
                .estado(DocumentoCalidadEstado.VIGENTE)
                .lote(lote)
                .creadoPorId(usuarioId)
                .fechaCreacion(LocalDateTime.now())
                .build();

        DocumentoCalidad guardado = documentoRepository.save(documento);
        return DocumentoCalidadMapper.toDTO(guardado);
    }

    @Override
    @Transactional
    public DocumentoCalidadVersionDTO subirVersion(Long documentoId, MultipartFile archivo, String nombreVisible, Long usuarioId) {
        DocumentoCalidad documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.DOCUMENTO_NO_ENCONTRADO,
                        "Documento no encontrado.",
                        Map.of("documentoId", documentoId)));

        if (documento.getEstado() == DocumentoCalidadEstado.OBSOLETO) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                    "No se pueden cargar versiones sobre documentos obsoletos.",
                    Map.of("documentoId", documentoId));
        }

        if (archivo == null || archivo.isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "Debe adjuntar un archivo para crear la versión.");
        }

        int siguiente = versionRepository.findTopByDocumento_IdOrderByVersionDesc(documentoId)
                .map(v -> v.getVersion() + 1)
                .orElse(1);

        String nombreOriginal = Optional.ofNullable(archivo.getOriginalFilename()).orElse("documento");
        String nombreSanitizado = nombreOriginal.replaceAll("[^a-zA-Z0-9._-]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String nombreArchivo = documentoId + "_v" + siguiente + "_" + timestamp + "_" + nombreSanitizado;

        Path ruta = obtenerUploadRoot().resolve(nombreArchivo);
        try {
            Files.createDirectories(ruta.getParent());
            archivo.transferTo(ruta.toFile());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo almacenar el documento de calidad", e);
        }

        DocumentoCalidadVersion version = DocumentoCalidadVersion.builder()
                .documento(documento)
                .version(siguiente)
                .nombreArchivo(nombreArchivo)
                .nombreVisible(nombreVisible != null && !nombreVisible.isBlank() ? nombreVisible : nombreOriginal)
                .contentType(archivo.getContentType())
                .sizeBytes(archivo.getSize())
                .storagePath(ruta.toString())
                .creadoPorId(usuarioId)
                .fechaCreacion(LocalDateTime.now())
                .build();

        DocumentoCalidadVersion guardada = versionRepository.save(version);

        documento.setActualizadoPorId(usuarioId);
        documento.setFechaActualizacion(LocalDateTime.now());
        documentoRepository.save(documento);

        return DocumentoCalidadMapper.toVersionDTO(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoCalidadVersionDTO> listarVersiones(Long documentoId) {
        return versionRepository.findByDocumento_IdOrderByVersionDesc(documentoId).stream()
                .map(DocumentoCalidadMapper::toVersionDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoCalidadVersionDownloadDTO descargarVersion(Long versionId) {
        DocumentoCalidadVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.DOCUMENTO_NO_ENCONTRADO,
                        "Versión de documento no encontrada.",
                        Map.of("versionId", versionId)));

        Path archivo = Paths.get(version.getStoragePath());
        if (!Files.exists(archivo)) {
            throw new CustomBusinessException(ApiErrorCode.DOCUMENTO_NO_ENCONTRADO,
                    "No se encontró el archivo en almacenamiento.",
                    Map.of("versionId", versionId, "storagePath", version.getStoragePath()));
        }

        try {
            byte[] contenido = Files.readAllBytes(archivo);
            return DocumentoCalidadVersionDownloadDTO.builder()
                    .nombreArchivo(Optional.ofNullable(version.getNombreVisible()).orElse(version.getNombreArchivo()))
                    .contentType(version.getContentType())
                    .contenido(contenido)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer el documento de calidad", e);
        }
    }

    @Override
    @Transactional
    public DocumentoCalidadDTO obsoletar(Long documentoId, Long usuarioId) {
        DocumentoCalidad documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.DOCUMENTO_NO_ENCONTRADO,
                        "Documento no encontrado.",
                        Map.of("documentoId", documentoId)));

        documento.setEstado(DocumentoCalidadEstado.OBSOLETO);
        documento.setActualizadoPorId(usuarioId);
        documento.setFechaActualizacion(LocalDateTime.now());
        return DocumentoCalidadMapper.toDTO(documentoRepository.save(documento));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoCalidadResumenDTO> listarVigentesPorLote(Long loteId) {
        if (loteId == null) {
            return List.of();
        }
        List<DocumentoCalidad> documentos = documentoRepository.findByLote_IdAndEstado(loteId, DocumentoCalidadEstado.VIGENTE);
        return construirResumen(documentos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoCalidadResumenDTO> listarVigentesPorTipo(DocumentoCalidadTipo tipo) {
        if (tipo == null) {
            return List.of();
        }
        List<DocumentoCalidad> documentos = documentoRepository.findByTipoAndEstado(tipo, DocumentoCalidadEstado.VIGENTE);
        return construirResumen(documentos);
    }

    private List<DocumentoCalidadResumenDTO> construirResumen(List<DocumentoCalidad> documentos) {
        if (documentos == null || documentos.isEmpty()) {
            return List.of();
        }
        List<Long> ids = documentos.stream()
                .map(DocumentoCalidad::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, DocumentoCalidadVersion> ultimas = new HashMap<>();
        if (!ids.isEmpty()) {
            for (DocumentoCalidadVersion version : versionRepository.findByDocumento_IdInOrderByVersionDesc(ids)) {
                Long docId = version.getDocumento() != null ? version.getDocumento().getId() : null;
                if (docId != null && !ultimas.containsKey(docId)) {
                    ultimas.put(docId, version);
                }
            }
        }

        return documentos.stream()
                .map(doc -> {
                    DocumentoCalidadVersion version = ultimas.get(doc.getId());
                    return DocumentoCalidadResumenDTO.builder()
                            .documentoId(doc.getId())
                            .tipo(doc.getTipo())
                            .codigo(doc.getCodigo())
                            .nombre(doc.getNombre())
                            .version(version != null ? version.getVersion() : null)
                            .nombreVisible(version != null ? version.getNombreVisible() : null)
                            .fechaVersion(version != null ? version.getFechaCreacion() : null)
                            .build();
                })
                .toList();
    }

    private Path obtenerUploadRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }
}
