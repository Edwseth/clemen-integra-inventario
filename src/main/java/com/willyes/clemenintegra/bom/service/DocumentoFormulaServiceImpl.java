package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.dto.DocumentoFormulaDescargaDTO;
import com.willyes.clemenintegra.bom.dto.DocumentoFormulaMetadataDTO;
import com.willyes.clemenintegra.bom.dto.DocumentoFormulaResponseDTO;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.DocumentoFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.model.enums.TipoDocumento;
import com.willyes.clemenintegra.bom.repository.DocumentoFormulaRepository;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentoFormulaServiceImpl implements DocumentoFormulaService {

    private final DocumentoFormulaRepository documentoRepository;
    private final FormulaProductoRepository formulaRepository;
    private final UsuarioRepository usuarioRepository;
    private final BomMapper bomMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoFormulaResponseDTO> listarDocumentos(Long formulaId) {
        FormulaProducto formula = formulaRepository.findById(formulaId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "La fórmula solicitada no existe"));
        return documentoRepository.findByFormula_Id(formula.getId()).stream()
                .map(bomMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DocumentoFormulaResponseDTO guardarDocumento(Long formulaId,
                                                         MultipartFile archivo,
                                                         DocumentoFormulaMetadataDTO metadata,
                                                         Long usuarioId) {
        if (archivo == null || archivo.isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Debe adjuntar un archivo válido");
        }

        FormulaProducto formula = formulaRepository.findById(formulaId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "La fórmula solicitada no existe"));

        validarFormulaEditable(formula);

        if (usuarioId == null) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                    "El usuario autenticado es obligatorio para registrar documentos.");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Usuario no encontrado"));

        String nombreOriginal = archivo.getOriginalFilename();
        String nombreSanitizado = sanitizeFilename(nombreOriginal != null ? nombreOriginal : archivo.getName());
        String nombreVisible = metadata != null && metadata.getNombreVisible() != null
                ? metadata.getNombreVisible()
                : nombreOriginal != null ? nombreOriginal : nombreSanitizado;

        String nombreInterno = System.currentTimeMillis() + "_" + nombreSanitizado;
        Path rutaBase = Paths.get(System.getProperty("user.dir"), "uploads", "bom", "formulas",
                formula.getId().toString()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(rutaBase);
            Path destino = rutaBase.resolve(nombreInterno);
            archivo.transferTo(destino.toFile());
        } catch (IOException e) {
            log.error("Error al almacenar documento de fórmula {}", formulaId, e);
            throw new CustomBusinessException(ApiErrorCode.ERROR_INTERNO,
                    "No fue posible almacenar el archivo adjunto de la fórmula");
        }

        DocumentoFormula documento = new DocumentoFormula();
        documento.setFormula(formula);
        documento.setUsuario(usuario);
        documento.setTipoDocumento(resolverTipoDocumento(metadata, nombreOriginal));
        documento.setNombreArchivo(nombreVisible);
        documento.setRutaArchivo(Paths.get("uploads", "bom", "formulas", formula.getId().toString(), nombreInterno)
                .toString());
        documento.setFechaSubida(LocalDateTime.now());

        DocumentoFormula guardado = documentoRepository.save(documento);
        return bomMapper.toResponseDTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoFormulaDescargaDTO descargarDocumento(Long documentoId) {
        DocumentoFormula documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "El documento solicitado no existe"));

        Path rutaArchivo = Paths.get(System.getProperty("user.dir"))
                .resolve(documento.getRutaArchivo()).toAbsolutePath().normalize();
        Path rutaUploads = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();

        if (!rutaArchivo.startsWith(rutaUploads) || !Files.exists(rutaArchivo)) {
            throw new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                    "El archivo físico del documento no está disponible");
        }

        try {
            Resource recurso = new UrlResource(rutaArchivo.toUri());
            if (!recurso.exists() || !recurso.isReadable()) {
                throw new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "El archivo físico del documento no está disponible");
            }
            String contentType = Files.probeContentType(rutaArchivo);
            String nombreDescarga = documento.getNombreArchivo() != null
                    ? documento.getNombreArchivo()
                    : recurso.getFilename();
            return new DocumentoFormulaDescargaDTO(recurso,
                    nombreDescarga,
                    contentType != null ? contentType : "application/octet-stream");
        } catch (IOException e) {
            log.error("Error al cargar el documento {}", documentoId, e);
            throw new CustomBusinessException(ApiErrorCode.ERROR_INTERNO,
                    "No fue posible preparar el archivo para descarga");
        }
    }

    @Override
    @Transactional
    public void eliminarDocumento(Long documentoId, Long usuarioId) {
        DocumentoFormula documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "El documento solicitado no existe"));

        FormulaProducto formula = documento.getFormula();
        if (formula == null || formula.getId() == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "El documento no tiene una fórmula asociada válida");
        }

        validarFormulaEditable(formulaRepository.findById(formula.getId())
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "La fórmula asociada al documento no existe")));

        if (usuarioId == null) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                    "El usuario autenticado es obligatorio para eliminar documentos.");
        }

        Path rutaArchivo = Paths.get(System.getProperty("user.dir"))
                .resolve(documento.getRutaArchivo()).toAbsolutePath().normalize();
        Path rutaUploads = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();

        try {
            if (rutaArchivo.startsWith(rutaUploads) && Files.exists(rutaArchivo)) {
                Files.delete(rutaArchivo);
            }
        } catch (IOException e) {
            log.warn("No fue posible eliminar el archivo físico del documento {}", documentoId, e);
        }

        documentoRepository.delete(documento);
    }

    private void validarFormulaEditable(FormulaProducto formula) {
        EstadoFormula estado = formula.getEstado();
        if (estado == null) {
            return;
        }
        if (!(estado == EstadoFormula.BORRADOR || estado == EstadoFormula.EN_REVISION)) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                    String.format("La fórmula en estado %s no permite agregar o eliminar documentos.", estado));
        }
    }

    private TipoDocumento resolverTipoDocumento(DocumentoFormulaMetadataDTO metadata, String nombreOriginal) {
        if (metadata != null && metadata.getTipoDocumento() != null) {
            try {
                return TipoDocumento.valueOf(metadata.getTipoDocumento().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                        "Tipo de documento inválido: " + metadata.getTipoDocumento());
            }
        }
        if (nombreOriginal != null) {
            String lower = nombreOriginal.toLowerCase(Locale.ROOT);
            if (lower.contains("msds")) {
                return TipoDocumento.MSDS;
            } else if (lower.contains("instructivo")) {
                return TipoDocumento.INSTRUCTIVO;
            } else if (lower.contains("procedimiento")) {
                return TipoDocumento.PROCEDIMIENTO;
            }
        }
        return TipoDocumento.PROCEDIMIENTO;
    }

    private String sanitizeFilename(String nombre) {
        return nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
