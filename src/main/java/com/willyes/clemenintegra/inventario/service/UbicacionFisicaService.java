package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.UbicacionFisicaRequestDTO;
import com.willyes.clemenintegra.inventario.dto.UbicacionFisicaResponseDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.UbicacionFisica;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.UbicacionFisicaRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UbicacionFisicaService {

    private final UbicacionFisicaRepository ubicacionFisicaRepository;
    private final AlmacenRepository almacenRepository;
    private final UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public List<UbicacionFisicaResponseDTO> buscar(Integer almacenId, String q) {
        List<UbicacionFisica> ubicaciones = StringUtils.hasText(q)
                ? ubicacionFisicaRepository.searchActivas(almacenId, q.trim())
                : ubicacionFisicaRepository.findByAlmacenIdAndActivoTrueOrderByCodigoAsc(almacenId);
        return ubicaciones.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UbicacionFisicaResponseDTO crear(UbicacionFisicaRequestDTO dto) {
        Almacen almacen = obtenerAlmacen(dto.getAlmacenId());
        validarCodigoUnico(dto.getAlmacenId(), dto.getCodigo(), null);
        UbicacionFisica ubicacion = UbicacionFisica.builder()
                .almacen(almacen)
                .codigo(dto.getCodigo().trim())
                .descripcion(dto.getDescripcion())
                .activo(dto.getActivo() == null || dto.getActivo())
                .usuario(usuarioService.obtenerUsuarioAutenticado())
                .build();
        return toResponse(ubicacionFisicaRepository.save(ubicacion));
    }

    @Transactional
    public UbicacionFisicaResponseDTO actualizar(Long id, UbicacionFisicaRequestDTO dto) {
        UbicacionFisica existente = ubicacionFisicaRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.UBICACION_NO_ENCONTRADA,
                        "La ubicación no existe",
                        Map.of("ubicacionId", id)));
        Almacen almacen = obtenerAlmacen(dto.getAlmacenId());
        validarCodigoUnico(dto.getAlmacenId(), dto.getCodigo(), id);
        existente.setAlmacen(almacen);
        existente.setCodigo(dto.getCodigo().trim());
        existente.setDescripcion(dto.getDescripcion());
        if (dto.getActivo() != null) {
            existente.setActivo(dto.getActivo());
        }
        return toResponse(ubicacionFisicaRepository.save(existente));
    }

    @Transactional(readOnly = true)
    public UbicacionFisica validarUbicacionParaAlmacen(Long ubicacionId, Integer almacenId) {
        UbicacionFisica ubicacion = ubicacionFisicaRepository.findByIdAndActivoTrue(ubicacionId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.UBICACION_NO_ENCONTRADA,
                        "La ubicación no existe o está inactiva",
                        Map.of("ubicacionId", ubicacionId)));
        if (almacenId == null || ubicacion.getAlmacen() == null
                || !almacenId.equals(ubicacion.getAlmacen().getId())) {
            throw new CustomBusinessException(ApiErrorCode.UBICACION_NO_PERTENECE_ALMACEN,
                    "La ubicación no pertenece al almacén indicado",
                    Map.of(
                            "ubicacionId", ubicacionId,
                            "almacenDestinoId", almacenId != null ? almacenId.longValue() : null,
                            "almacenUbicacionId", Optional.ofNullable(ubicacion.getAlmacen())
                                    .map(Almacen::getId)
                                    .map(Integer::longValue)
                                    .orElse(null)
                    ));
        }
        return ubicacion;
    }

    private void validarCodigoUnico(Integer almacenId, String codigo, Long ubicacionId) {
        if (!StringUtils.hasText(codigo)) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "El código de ubicación es obligatorio");
        }
        boolean existe = ubicacionId == null
                ? ubicacionFisicaRepository.existsByAlmacenIdAndCodigoIgnoreCase(almacenId, codigo.trim())
                : ubicacionFisicaRepository.existsByAlmacenIdAndCodigoIgnoreCaseAndIdNot(almacenId, codigo.trim(), ubicacionId);
        if (existe) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                    "Ya existe una ubicación con ese código en el almacén",
                    Map.of("almacenId", almacenId, "codigo", codigo.trim()));
        }
    }

    private UbicacionFisicaResponseDTO toResponse(UbicacionFisica ubicacion) {
        return UbicacionFisicaResponseDTO.builder()
                .id(ubicacion.getId())
                .almacenId(ubicacion.getAlmacen() != null ? ubicacion.getAlmacen().getId() : null)
                .codigo(ubicacion.getCodigo())
                .descripcion(ubicacion.getDescripcion())
                .activo(ubicacion.isActivo())
                .build();
    }

    private Almacen obtenerAlmacen(Integer almacenId) {
        return almacenRepository.findById(almacenId.longValue())
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Almacén no encontrado",
                        Map.of("almacenId", almacenId)));
    }
}
