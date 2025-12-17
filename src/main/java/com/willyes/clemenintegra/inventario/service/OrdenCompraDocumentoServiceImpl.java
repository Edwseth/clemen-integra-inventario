package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.DocumentoMetaDTO;
import com.willyes.clemenintegra.inventario.dto.OrdenCompraDocumentoDescargaDTO;
import com.willyes.clemenintegra.inventario.dto.OrdenCompraDocumentoResponseDTO;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDocumento;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraDocumentoRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrdenCompraDocumentoServiceImpl implements OrdenCompraDocumentoService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final OrdenCompraDocumentoRepository documentoRepository;
    private final UsuarioRepository usuarioRepository;

    private static final DateTimeFormatter NOMBRE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    @Transactional
    public List<OrdenCompraDocumentoResponseDTO> subirDocumentos(Long ordenCompraId,
                                                                 List<MultipartFile> archivos,
                                                                 List<DocumentoMetaDTO> metadata,
                                                                 Long usuarioId) {
        if (archivos == null || archivos.isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Debe adjuntar al menos un archivo válido");
        }
        if (usuarioId == null) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA, "El usuario autenticado es obligatorio");
        }

        OrdenCompra ordenCompra = ordenCompraRepository.findById(ordenCompraId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "La orden de compra no existe"));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "Usuario no encontrado"));

        List<OrdenCompraDocumento> guardados = new ArrayList<>();
        for (int i = 0; i < archivos.size(); i++) {
            MultipartFile archivo = archivos.get(i);
            if (archivo == null || archivo.isEmpty()) {
                throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Debe adjuntar un archivo válido");
            }
            DocumentoMetaDTO meta = obtenerMeta(metadata, i);
            validarMeta(meta);

            String nombreOriginal = archivo.getOriginalFilename();
            String nombreSanitizado = sanitizeFilename(nombreOriginal != null ? nombreOriginal : archivo.getName());
            String nombreInterno = construirNombreInterno(nombreSanitizado);

            Path rutaBase = Paths.get(System.getProperty("user.dir"), "uploads", "ordenes-compra",
                    ordenCompraId.toString()).toAbsolutePath().normalize();
            Path rutaUploads = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();

            try {
                Files.createDirectories(rutaBase);
                Path destino = rutaBase.resolve(nombreInterno);
                Path destinoNormalizado = destino.toAbsolutePath().normalize();

                if (!destinoNormalizado.startsWith(rutaUploads)) {
                    throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Ruta de almacenamiento inválida");
                }

                archivo.transferTo(destino.toFile());

                OrdenCompraDocumento documento = new OrdenCompraDocumento();
                documento.setOrdenCompra(ordenCompra);
                documento.setTipoDocumento(meta.getTipoDocumento());
                documento.setNombreVisible(meta.getNombreVisible());
                documento.setNombreArchivo(nombreInterno);
                documento.setContentType(archivo.getContentType());
                documento.setSize(archivo.getSize());
                documento.setStoragePath(Paths.get("uploads", "ordenes-compra", ordenCompraId.toString(), nombreInterno).toString());
                documento.setCreadoPor(usuario);
                documento.setFechaCreacion(LocalDateTime.now());

                guardados.add(documentoRepository.save(documento));
            } catch (IOException e) {
                log.error("Error al almacenar documento de orden de compra {}", ordenCompraId, e);
                throw new CustomBusinessException(ApiErrorCode.ERROR_INTERNO,
                        "No fue posible almacenar el archivo adjunto de la orden de compra");
            }
        }

        return guardados.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenCompraDocumentoResponseDTO> listar(Long ordenCompraId) {
        return documentoRepository.findByOrdenCompra_Id(ordenCompraId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenCompraDocumentoDescargaDTO descargar(Long documentoId) {
        OrdenCompraDocumento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "El documento solicitado no existe"));

        Path rutaArchivo = Paths.get(System.getProperty("user.dir"))
                .resolve(documento.getStoragePath()).toAbsolutePath().normalize();
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

            String contentType = documento.getContentType();
            if (contentType == null) {
                contentType = Files.probeContentType(rutaArchivo);
            }

            String nombreDescarga = documento.getNombreVisible() != null
                    ? documento.getNombreVisible()
                    : recurso.getFilename();

            return new OrdenCompraDocumentoDescargaDTO(recurso,
                    nombreDescarga,
                    contentType != null ? contentType : "application/octet-stream");
        } catch (IOException e) {
            log.error("Error al preparar descarga del documento {}", documentoId, e);
            throw new CustomBusinessException(ApiErrorCode.ERROR_INTERNO,
                    "No fue posible preparar el archivo para descarga");
        }
    }

    private OrdenCompraDocumentoResponseDTO toResponseDTO(OrdenCompraDocumento documento) {
        return OrdenCompraDocumentoResponseDTO.builder()
                .id(documento.getId())
                .tipoDocumento(documento.getTipoDocumento())
                .nombreVisible(documento.getNombreVisible())
                .contentType(documento.getContentType())
                .size(documento.getSize())
                .fechaCreacion(documento.getFechaCreacion())
                .creadoPorNombre(documento.getCreadoPor() != null ? documento.getCreadoPor().getNombreCompleto() : null)
                .build();
    }

    private DocumentoMetaDTO obtenerMeta(List<DocumentoMetaDTO> metadata, int index) {
        if (metadata == null || metadata.size() <= index) {
            return null;
        }
        return metadata.get(index);
    }

    private void validarMeta(DocumentoMetaDTO meta) {
        if (meta == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Falta información de metadata para el archivo");
        }
        if (meta.getNombreVisible() == null || meta.getNombreVisible().isBlank()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "El nombre visible es obligatorio");
        }
        if (meta.getTipoDocumento() == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "El tipo de documento es obligatorio");
        }
    }

    private String sanitizeFilename(String nombre) {
        return nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String construirNombreInterno(String nombreSanitizado) {
        String timestamp = LocalDateTime.now().format(NOMBRE_FORMATTER);
        return timestamp + "_" + UUID.randomUUID() + "_" + nombreSanitizado;
    }
}
