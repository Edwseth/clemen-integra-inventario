package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.CapaArchivoDTO;
import com.willyes.clemenintegra.calidad.dto.CapaArchivoDescargaDTO;
import com.willyes.clemenintegra.calidad.dto.CapaDTO;
import com.willyes.clemenintegra.calidad.mapper.CapaMapper;
import com.willyes.clemenintegra.calidad.model.Capa;
import com.willyes.clemenintegra.calidad.model.CapaArchivo;
import com.willyes.clemenintegra.calidad.model.enums.EstadoCapa;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.calidad.repository.CapaArchivoRepository;
import com.willyes.clemenintegra.calidad.repository.CapaRepository;
import com.willyes.clemenintegra.calidad.repository.NoConformidadRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CapaServiceImpl implements CapaService {

    private final CapaRepository capaRepository;
    private final NoConformidadRepository noConformidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final CapaArchivoRepository capaArchivoRepository;
    private final CapaMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<CapaDTO> listar(EstadoCapa estado, SeveridadNoConformidad severidad, Pageable pageable) {
        Page<Capa> page;
        if (estado != null && severidad != null) {
            page = capaRepository.findByNoConformidad_SeveridadAndEstado(severidad, estado, pageable);
        } else if (estado != null) {
            page = capaRepository.findByEstado(estado, pageable);
        } else if (severidad != null) {
            page = capaRepository.findByNoConformidad_Severidad(severidad, pageable);
        } else {
            page = capaRepository.findAll(pageable);
        }

        Map<Long, List<CapaArchivo>> adjuntos = agruparAdjuntos(page.getContent());

        return page.map(capa -> mapper.toDTO(
                capa,
                adjuntos.getOrDefault(capa.getId(), List.of())));
    }

    @Override
    public CapaDTO crear(CapaDTO dto) {
        validarCamposObligatorios(dto);
        var nc = obtenerNoConformidad(dto.getNoConformidadId());
        var user = obtenerResponsable(dto.getResponsableId());
        Capa entity = mapper.toEntity(dto, nc, user);
        Capa guardada = capaRepository.save(entity);
        return mapper.toDTO(guardada, List.of());
    }

    @Override
    public CapaDTO actualizar(Long id, CapaDTO dto) {
        validarCamposObligatorios(dto);
        Capa existing = obtenerCapa(id);
        var nc = obtenerNoConformidad(dto.getNoConformidadId());
        var user = obtenerResponsable(dto.getResponsableId());

        existing.setNoConformidad(nc);
        existing.setTipo(dto.getTipo());
        existing.setResponsable(user);
        existing.setFechaInicio(dto.getFechaInicio());
        if (dto.getFechaCierre() != null) {
            existing.setFechaCierre(dto.getFechaCierre());
        }
        existing.setFechaLimite(dto.getFechaLimite());
        if (dto.getEstado() != null) {
            existing.setEstado(dto.getEstado());
            if (EstadoCapa.CERRADA.equals(dto.getEstado()) && existing.getFechaCierre() == null) {
                existing.setFechaCierre(LocalDateTime.now());
            }
        }
        existing.setObservaciones(dto.getObservaciones());

        Capa guardada = capaRepository.save(existing);
        return mapper.toDTO(guardada, capaArchivoRepository.findByCapa_Id(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CapaDTO obtenerPorId(Long id) {
        Capa capa = obtenerCapa(id);
        return mapper.toDTO(capa, capaArchivoRepository.findByCapa_Id(id));
    }

    @Override
    public void eliminar(Long id) {
        capaRepository.deleteById(id);
    }

    @Override
    public CapaDTO cerrar(Long id) {
        Capa capa = obtenerCapa(id);
        if (EstadoCapa.CERRADA.equals(capa.getEstado()) && capa.getFechaCierre() != null) {
            return mapper.toDTO(capa, capaArchivoRepository.findByCapa_Id(id));
        }
        capa.setEstado(EstadoCapa.CERRADA);
        if (capa.getFechaCierre() == null) {
            capa.setFechaCierre(LocalDateTime.now());
        }
        Capa guardada = capaRepository.save(capa);
        return mapper.toDTO(guardada, capaArchivoRepository.findByCapa_Id(id));
    }

    @Override
    public CapaArchivoDTO adjuntarArchivo(Long capaId, MultipartFile archivo, String nombreVisible, Long usuarioId) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo adjunto es obligatorio");
        }
        Capa capa = obtenerCapa(capaId);
        try {
            Path uploadRoot = obtenerDirectorioAdjuntos();
            Files.createDirectories(uploadRoot);

            String nombreOriginal = archivo.getOriginalFilename();
            String nombreSanitizado = sanitizarNombreArchivo(nombreOriginal != null ? nombreOriginal : "archivo");
            String nombreArchivo = UUID.randomUUID() + "_" + nombreSanitizado;

            Path destino = uploadRoot.resolve(nombreArchivo);
            archivo.transferTo(destino.toFile());

            CapaArchivo entity = CapaArchivo.builder()
                    .capa(capa)
                    .nombreArchivo(nombreArchivo)
                    .nombreVisible(nombreVisible != null && !nombreVisible.isBlank() ? nombreVisible : nombreOriginal)
                    .contentType(archivo.getContentType())
                    .tamanoBytes(archivo.getSize())
                    .creadoPor(usuarioId)
                    .fechaCreacion(LocalDateTime.now())
                    .build();

            CapaArchivo guardado = capaArchivoRepository.save(entity);
            return mapper.toArchivoDTO(guardado);
        } catch (IOException e) {
            throw new CustomBusinessException(
                    ApiErrorCode.ERROR_INTERNO,
                    "No se pudo guardar el archivo de la CAPA.",
                    e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CapaArchivoDTO> listarArchivos(Long capaId) {
        validarExistenciaCapa(capaId);
        return capaArchivoRepository.findByCapa_Id(capaId).stream()
                .map(mapper::toArchivoDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CapaArchivoDescargaDTO descargarArchivo(Long capaId, Long archivoId) {
        validarExistenciaCapa(capaId);
        CapaArchivo archivo = capaArchivoRepository.findByIdAndCapa_Id(archivoId, capaId)
                .orElseThrow(() -> new CustomBusinessException(
                        ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Archivo no encontrado para la CAPA.",
                        Map.of("capaId", capaId, "archivoId", archivoId)));
        Path ruta = obtenerDirectorioAdjuntos().resolve(archivo.getNombreArchivo()).normalize();
        if (!Files.exists(ruta)) {
            throw new CustomBusinessException(
                    ApiErrorCode.RECURSO_NO_ENCONTRADO,
                    "El archivo físico no existe.",
                    Map.of("capaId", capaId, "archivoId", archivoId, "archivo", archivo.getNombreArchivo()));
        }
        try {
            byte[] contenido = Files.readAllBytes(ruta);
            String nombreDescarga = archivo.getNombreVisible() != null && !archivo.getNombreVisible().isBlank()
                    ? archivo.getNombreVisible()
                    : archivo.getNombreArchivo();
            return CapaArchivoDescargaDTO.builder()
                    .contenido(contenido)
                    .nombreArchivo(nombreDescarga)
                    .contentType(archivo.getContentType())
                    .build();
        } catch (IOException e) {
            throw new CustomBusinessException(
                    ApiErrorCode.ERROR_INTERNO,
                    "No se pudo leer el archivo adjunto.",
                    e.getMessage());
        }
    }

    private void validarCamposObligatorios(CapaDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("La CAPA es obligatoria");
        }
        if (dto.getNoConformidadId() == null) {
            throw new IllegalArgumentException("La no conformidad es obligatoria");
        }
        if (dto.getTipo() == null) {
            throw new IllegalArgumentException("El tipo de CAPA es obligatorio");
        }
        if (dto.getResponsableId() == null) {
            throw new IllegalArgumentException("El responsable es obligatorio");
        }
        if (dto.getFechaInicio() == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }
    }

    private com.willyes.clemenintegra.calidad.model.NoConformidad obtenerNoConformidad(Long id) {
        return noConformidadRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(
                        ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "No conformidad no encontrada.",
                        Map.of("noConformidadId", id)));
    }

    private Usuario obtenerResponsable(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(
                        ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Usuario no encontrado.",
                        Map.of("usuarioId", id)));
    }

    private Capa obtenerCapa(Long id) {
        return capaRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(
                        ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "CAPA no encontrada.",
                        Map.of("capaId", id)));
    }

    private Map<Long, List<CapaArchivo>> agruparAdjuntos(List<Capa> capas) {
        List<Long> ids = capas == null ? List.of() : capas.stream()
                .map(Capa::getId)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return capaArchivoRepository.findByCapa_IdIn(ids).stream()
                .collect(Collectors.groupingBy(ca -> ca.getCapa().getId()));
    }

    private void validarExistenciaCapa(Long capaId) {
        if (capaId == null || !capaRepository.existsById(capaId)) {
            throw new CustomBusinessException(
                    ApiErrorCode.RECURSO_NO_ENCONTRADO,
                    "CAPA no encontrada.",
                    Map.of("capaId", capaId));
        }
    }

    private Path obtenerDirectorioAdjuntos() {
        return Paths.get(System.getProperty("user.dir"), "uploads", "capas");
    }

    private String sanitizarNombreArchivo(String nombreOriginal) {
        return nombreOriginal.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
