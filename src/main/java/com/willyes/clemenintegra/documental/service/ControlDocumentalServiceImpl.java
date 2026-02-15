package com.willyes.clemenintegra.documental.service;

import com.willyes.clemenintegra.documental.dto.DocumentoCreateRequest;
import com.willyes.clemenintegra.documental.dto.DocumentoDTO;
import com.willyes.clemenintegra.documental.dto.DocumentoDetalleDTO;
import com.willyes.clemenintegra.documental.dto.DocumentoVersionCreateRequest;
import com.willyes.clemenintegra.documental.dto.DocumentoVersionDTO;
import com.willyes.clemenintegra.documental.dto.DocumentoVersionDownloadDTO;
import com.willyes.clemenintegra.documental.mapper.DocumentoMapper;
import com.willyes.clemenintegra.documental.model.Documento;
import com.willyes.clemenintegra.documental.model.DocumentoVersion;
import com.willyes.clemenintegra.documental.model.enums.AreaDocumento;
import com.willyes.clemenintegra.documental.model.enums.EstadoDocumento;
import com.willyes.clemenintegra.documental.model.enums.TipoDocumento;
import com.willyes.clemenintegra.documental.repository.DocumentoRepository;
import com.willyes.clemenintegra.documental.repository.DocumentoVersionRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ControlDocumentalServiceImpl implements ControlDocumentalService {

    private static final DateTimeFormatter NOMBRE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final DocumentoRepository documentoRepository;
    private final DocumentoVersionRepository versionRepository;

    @Value("${app.documental.upload-dir:${user.dir}/uploads/documentos}")
    private String uploadDir;

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentoDTO> buscarDocumentos(TipoDocumento tipo,
                                              AreaDocumento area,
                                              EstadoDocumento estado,
                                              String texto,
                                              Pageable pageable) {
        Page<Documento> documentos = documentoRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            if (tipo != null) {
                predicates.add(cb.equal(root.get("tipo"), tipo));
            }
            if (area != null) {
                predicates.add(cb.equal(root.get("area"), area));
            }
            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }
            if (texto != null && !texto.isBlank()) {
                String like = "%" + texto.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("codigo")), like),
                        cb.like(cb.lower(root.get("nombre")), like),
                        cb.like(cb.lower(root.get("descripcion")), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        return documentos.map(documento -> {
            DocumentoVersion vigente = versionRepository
                    .findTopByDocumentoIdOrderByNumeroVersionDesc(documento.getId())
                    .orElse(null);
            return DocumentoMapper.toDTO(documento, vigente);
        });
    }

    @Override
    @Transactional
    public DocumentoDTO crearDocumento(DocumentoCreateRequest request, Usuario creadoPor) {
        if (request == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "La solicitud es obligatoria.");
        }
        if (creadoPor == null || creadoPor.getId() == null) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                    "El usuario autenticado es obligatorio.");
        }
        if (request.getCodigo() == null || request.getCodigo().isBlank()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "El código del documento es obligatorio.");
        }
        if (documentoRepository.existsByCodigoIgnoreCase(request.getCodigo().trim())) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                    "Ya existe un documento con ese código.",
                    Map.of("codigo", request.getCodigo()));
        }

        Documento documento = Documento.builder()
                .codigo(request.getCodigo().trim())
                .nombre(request.getNombre() != null ? request.getNombre().trim() : null)
                .tipo(request.getTipo())
                .area(request.getArea())
                .estado(EstadoDocumento.EN_ELABORACION)
                .descripcion(request.getDescripcion())
                .creadoPor(creadoPor)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Documento guardado = documentoRepository.save(documento);
        return DocumentoMapper.toDTO(guardado, null);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoDetalleDTO obtenerDetalleDocumento(Long documentoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Documento no encontrado.",
                        Map.of("documentoId", documentoId)));

        DocumentoVersion vigente = versionRepository.findTopByDocumentoIdOrderByNumeroVersionDesc(documentoId)
                .orElse(null);

        List<DocumentoVersionDTO> versiones = versionRepository.findByDocumentoIdOrderByNumeroVersionDesc(documentoId)
                .stream()
                .map(DocumentoMapper::toVersionDTO)
                .toList();

        return DocumentoDetalleDTO.builder()
                .documento(DocumentoMapper.toDTO(documento, vigente))
                .versiones(versiones)
                .build();
    }

    @Override
    @Transactional
    public DocumentoVersionDTO agregarVersion(Long documentoId,
                                              DocumentoVersionCreateRequest request,
                                              MultipartFile archivo,
                                              Usuario emitidoPor) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Documento no encontrado.",
                        Map.of("documentoId", documentoId)));

        if (emitidoPor == null || emitidoPor.getId() == null) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                    "El usuario autenticado es obligatorio.");
        }

        if (archivo == null || archivo.isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "Debe adjuntar un archivo válido para registrar la versión.");
        }

        int siguiente = versionRepository.findMaxNumeroVersionByDocumentoId(documentoId)
                .map(max -> max + 1)
                .orElse(1);

        String nombreOriginal = Optional.ofNullable(archivo.getOriginalFilename()).orElse("documento");
        String nombreSanitizado = sanitizarNombreArchivo(nombreOriginal);
        String timestamp = LocalDateTime.now().format(NOMBRE_FORMATTER);
        String nombreArchivo = documentoId + "_v" + siguiente + "_" + timestamp + "_" + nombreSanitizado;

        Path uploadRoot = obtenerDirectorioBase();
        Path rutaBase = uploadRoot.resolve(documentoId.toString()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(rutaBase);
            Path destino = rutaBase.resolve(nombreArchivo).toAbsolutePath().normalize();
            if (!destino.startsWith(uploadRoot)) {
                throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                        "Ruta de almacenamiento inválida.");
            }
            archivo.transferTo(destino.toFile());
        } catch (CustomBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error almacenando versión documental documentoId={}", documentoId, e);
            throw new CustomBusinessException(ApiErrorCode.ERROR_INTERNO,
                    "No fue posible almacenar el archivo del documento.");
        }

        List<DocumentoVersion> existentes = versionRepository.findByDocumentoIdOrderByNumeroVersionDesc(documentoId);
        for (DocumentoVersion version : existentes) {
            if (version.isVigente()) {
                version.setVigente(false);
            }
        }

        DocumentoVersion nueva = DocumentoVersion.builder()
                .documento(documento)
                .numeroVersion(siguiente)
                .fechaEmision(request != null && request.getFechaEmision() != null
                        ? request.getFechaEmision()
                        : LocalDateTime.now())
                .rutaArchivo(rutaBase.resolve(nombreArchivo).toString())
                .nombreArchivoOriginal(nombreOriginal)
                .nombreVisible(obtenerNombreVisible(request, nombreOriginal))
                .contentType(archivo.getContentType())
                .tamanoBytes(archivo.getSize())
                .comentarios(request != null ? request.getComentarios() : null)
                .emitidoPor(emitidoPor)
                .vigente(true)
                .build();

        DocumentoVersion guardada = versionRepository.save(nueva);

        if (documento.getEstado() == EstadoDocumento.EN_ELABORACION) {
            documento.setEstado(EstadoDocumento.VIGENTE);
            documentoRepository.save(documento);
        }

        return DocumentoMapper.toVersionDTO(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoVersionDownloadDTO descargarArchivoVersion(Long documentoId, Long versionId) {
        DocumentoVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Versión no encontrada.",
                        Map.of("versionId", versionId)));

        if (version.getDocumento() == null || !documentoId.equals(version.getDocumento().getId())) {
            throw new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                    "La versión no pertenece al documento solicitado.",
                    Map.of("documentoId", documentoId, "versionId", versionId));
        }

        Path rutaArchivo = Paths.get(version.getRutaArchivo()).toAbsolutePath().normalize();
        Path uploadRoot = obtenerDirectorioBase();

        if (!rutaArchivo.startsWith(uploadRoot) || !Files.exists(rutaArchivo)) {
            throw new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                    "El archivo físico del documento no está disponible.",
                    Map.of("versionId", versionId, "rutaArchivo", version.getRutaArchivo()));
        }

        try {
            Resource recurso = new UrlResource(rutaArchivo.toUri());
            if (!recurso.exists() || !recurso.isReadable()) {
                throw new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "El archivo físico del documento no está disponible.");
            }

            String contentType = version.getContentType();
            if (contentType == null) {
                contentType = Files.probeContentType(rutaArchivo);
            }

            return new DocumentoVersionDownloadDTO(
                    recurso,
                    version.getNombreArchivoOriginal(),
                    contentType != null ? contentType : "application/octet-stream"
            );
        } catch (CustomBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error preparando descarga de versión {}", versionId, e);
            throw new CustomBusinessException(ApiErrorCode.ERROR_INTERNO,
                    "No fue posible preparar el archivo para descarga.");
        }
    }



    @Override
    @Transactional
    public void eliminarDocumento(Long documentoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Documento no encontrado.",
                        Map.of("documentoId", documentoId)));
        documentoRepository.delete(documento);
    }

    @Override
    @Transactional
    public void cambiarEstadoDocumento(Long documentoId, EstadoDocumento nuevoEstado) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Documento no encontrado.",
                        Map.of("documentoId", documentoId)));

        if (nuevoEstado == EstadoDocumento.OBSOLETO) {
            versionRepository.findFirstByDocumentoIdAndVigenteTrue(documentoId)
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                            "No se puede obsoletar un documento sin versión vigente.",
                            Map.of("documentoId", documentoId)));
        }

        documento.setEstado(nuevoEstado);
        documentoRepository.save(documento);
    }

    private Path obtenerDirectorioBase() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private String sanitizarNombreArchivo(String nombreOriginal) {
        return nombreOriginal.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String obtenerNombreVisible(DocumentoVersionCreateRequest request, String nombreOriginal) {
        if (request == null || request.getNombreVisible() == null || request.getNombreVisible().isBlank()) {
            return nombreOriginal;
        }
        return request.getNombreVisible().trim();
    }
}
