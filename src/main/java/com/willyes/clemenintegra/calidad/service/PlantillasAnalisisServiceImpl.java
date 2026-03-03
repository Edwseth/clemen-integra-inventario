package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.*;
import com.willyes.clemenintegra.calidad.model.PlantillaAnalisis;
import com.willyes.clemenintegra.calidad.model.PlantillaCampo;
import com.willyes.clemenintegra.calidad.model.enums.TipoAnalisisPlantilla;
import com.willyes.clemenintegra.calidad.model.enums.TipoCampoPlantilla;
import com.willyes.clemenintegra.calidad.repository.PlantillaAnalisisRepository;
import com.willyes.clemenintegra.calidad.repository.PlantillaCampoRepository;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlantillasAnalisisServiceImpl implements PlantillasAnalisisService {

    private final PlantillaAnalisisRepository plantillaAnalisisRepository;
    private final PlantillaCampoRepository plantillaCampoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioService usuarioService;

    @Override
    @Transactional(readOnly = true)
    public List<PlantillaAnalisisResumenDTO> listarPorProductoYTipo(Long productoId, TipoAnalisisPlantilla tipoAnalisis) {
        return plantillaAnalisisRepository.findByProducto_IdAndTipoAnalisisOrderByVersionDesc(productoId, tipoAnalisis)
                .stream()
                .map(this::toResumenDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlantillaAnalisisDetalleDTO obtenerDetalle(Long plantillaId) {
        PlantillaAnalisis plantilla = obtenerPlantilla(plantillaId);
        return toDetalleDTO(plantilla);
    }

    @Override
    @Transactional
    public PlantillaAnalisisDetalleDTO crearPlantilla(Long productoId, TipoAnalisisPlantilla tipoAnalisis, PlantillaAnalisisCreateRequest request) {
        Producto producto = obtenerProducto(productoId);
        validarTipoAnalisis(producto, tipoAnalisis);
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        Integer nuevaVersion = plantillaAnalisisRepository.findTopByProducto_IdAndTipoAnalisisOrderByVersionDesc(productoId, tipoAnalisis)
                .map(p -> p.getVersion() + 1)
                .orElse(1);

        PlantillaAnalisis plantilla = PlantillaAnalisis.builder()
                .producto(producto)
                .tipoAnalisis(tipoAnalisis)
                .nombre(request != null ? request.getNombre() : null)
                .version(nuevaVersion)
                .vigente(request == null || request.getVigente() == null || request.getVigente())
                .activo(true)
                .creadoPor(usuario)
                .campos(new ArrayList<>())
                .build();

        List<PlantillaCampoRequest> camposRequest = request != null && request.getCampos() != null ? request.getCampos() : List.of();
        for (PlantillaCampoRequest campoRequest : camposRequest) {
            PlantillaCampo campo = construirCampo(plantilla, campoRequest, usuario, false, null);
            plantilla.getCampos().add(campo);
        }

        PlantillaAnalisis guardada = plantillaAnalisisRepository.save(plantilla);
        if (guardada.isVigente()) {
            desmarcarVigentesExistentes(guardada, usuario);
        }
        return toDetalleDTO(guardada);
    }

    @Override
    @Transactional
    public PlantillaAnalisisDetalleDTO clonarComoNuevaVersion(Long plantillaId) {
        PlantillaAnalisis base = obtenerPlantilla(plantillaId);
        validarTipoAnalisis(base.getProducto(), base.getTipoAnalisis());
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        Integer nuevaVersion = plantillaAnalisisRepository
                .findTopByProducto_IdAndTipoAnalisisOrderByVersionDesc(base.getProducto().getId().longValue(), base.getTipoAnalisis())
                .map(p -> p.getVersion() + 1)
                .orElse(1);

        PlantillaAnalisis clon = PlantillaAnalisis.builder()
                .producto(base.getProducto())
                .tipoAnalisis(base.getTipoAnalisis())
                .nombre(base.getNombre())
                .version(nuevaVersion)
                .vigente(false)
                .activo(base.isActivo())
                .creadoPor(usuario)
                .campos(new ArrayList<>())
                .build();

        base.getCampos().stream()
                .sorted(Comparator.comparing(PlantillaCampo::getOrden, Comparator.nullsLast(Integer::compareTo)))
                .forEach(c -> clon.getCampos().add(PlantillaCampo.builder()
                        .plantilla(clon)
                        .codigo(c.getCodigo())
                        .label(c.getLabel())
                        .tipoCampo(c.getTipoCampo())
                        .requerido(c.isRequerido())
                        .orden(c.getOrden())
                        .configJson(c.getConfigJson())
                        .activo(c.isActivo())
                        .creadoPor(usuario)
                        .build()));

        return toDetalleDTO(plantillaAnalisisRepository.save(clon));
    }

    @Override
    @Transactional
    public PlantillaAnalisisDetalleDTO marcarVigente(Long plantillaId) {
        PlantillaAnalisis plantilla = obtenerPlantilla(plantillaId);
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        plantilla.setVigente(true);
        plantilla.setActualizadoPor(usuario);
        desmarcarVigentesExistentes(plantilla, usuario);
        return toDetalleDTO(plantillaAnalisisRepository.save(plantilla));
    }

    @Override
    @Transactional
    public PlantillaCampoDTO crearCampo(Long plantillaId, PlantillaCampoRequest request) {
        PlantillaAnalisis plantilla = obtenerPlantilla(plantillaId);
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        PlantillaCampo campo = construirCampo(plantilla, request, usuario, true, null);
        return toCampoDTO(plantillaCampoRepository.save(campo));
    }

    @Override
    @Transactional
    public PlantillaCampoDTO editarCampo(Long plantillaId, Long campoId, PlantillaCampoRequest request) {
        PlantillaCampo campo = obtenerCampo(campoId);
        if (!campo.getPlantilla().getId().equals(plantillaId)) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA, "El campo no pertenece a la plantilla.");
        }
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        if (request.getCodigo() != null) {
            String codigo = normalizarCodigo(request.getCodigo());
            if (!codigo.equals(campo.getCodigo())
                    && plantillaCampoRepository.existsByPlantilla_IdAndCodigo(plantillaId, codigo)) {
                throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                        "Ya existe un campo con ese código en la plantilla.",
                        Map.of("code", "CAMPO_DUPLICADO", "codigo", codigo));
            }
            campo.setCodigo(codigo);
        }
        if (request.getLabel() != null) {
            validarTexto(request.getLabel(), "El label del campo es obligatorio.");
            campo.setLabel(request.getLabel().trim());
        }
        if (request.getTipoCampo() != null) {
            campo.setTipoCampo(request.getTipoCampo());
        }
        if (request.getOrden() != null) {
            validarOrden(request.getOrden());
            campo.setOrden(request.getOrden());
        }
        if (request.getRequerido() != null) {
            campo.setRequerido(request.getRequerido());
        }
        if (request.getActivo() != null) {
            campo.setActivo(request.getActivo());
        }
        campo.setConfigJson(request.getConfigJson());
        campo.setActualizadoPor(usuario);

        return toCampoDTO(plantillaCampoRepository.save(campo));
    }

    @Override
    @Transactional
    public void eliminarCampo(Long plantillaId, Long campoId) {
        PlantillaCampo campo = obtenerCampo(campoId);
        if (!campo.getPlantilla().getId().equals(plantillaId)) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA, "El campo no pertenece a la plantilla.");
        }
        plantillaCampoRepository.delete(campo);
    }

    private PlantillaCampo construirCampo(PlantillaAnalisis plantilla,
                                          PlantillaCampoRequest request,
                                          Usuario usuario,
                                          boolean validarDuplicado,
                                          Long campoIdActual) {
        if (request == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "La definición de campo es obligatoria.", Map.of("code", "CAMPO_INVALIDO"));
        }
        String codigo = normalizarCodigo(request.getCodigo());
        validarTexto(request.getLabel(), "El label del campo es obligatorio.");
        TipoCampoPlantilla tipoCampo = request.getTipoCampo();
        if (tipoCampo == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "El tipo de campo es obligatorio.", Map.of("code", "CAMPO_INVALIDO"));
        }
        validarOrden(request.getOrden());

        if (validarDuplicado && plantillaCampoRepository.existsByPlantilla_IdAndCodigo(plantilla.getId(), codigo)) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                    "Ya existe un campo con ese código en la plantilla.",
                    Map.of("code", "CAMPO_DUPLICADO", "codigo", codigo));
        }

        return PlantillaCampo.builder()
                .plantilla(plantilla)
                .codigo(codigo)
                .label(request.getLabel().trim())
                .tipoCampo(tipoCampo)
                .requerido(request.getRequerido() == null || request.getRequerido())
                .orden(request.getOrden())
                .configJson(request.getConfigJson())
                .activo(request.getActivo() == null || request.getActivo())
                .creadoPor(usuario)
                .build();
    }

    private void validarOrden(Integer orden) {
        if (orden == null || orden < 1) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "El orden del campo debe ser mayor o igual a 1.", Map.of("code", "CAMPO_INVALIDO", "orden", orden));
        }
    }

    private String normalizarCodigo(String codigo) {
        validarTexto(codigo, "El código del campo es obligatorio.");
        return codigo.trim().toUpperCase();
    }

    private void validarTexto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    mensaje, Map.of("code", "CAMPO_INVALIDO"));
        }
    }

    private Producto obtenerProducto(Long productoId) {
        return productoRepository.findById(productoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Producto no encontrado.", Map.of("code", "PRODUCTO_NO_ENCONTRADO", "productoId", productoId)));
    }

    private PlantillaAnalisis obtenerPlantilla(Long plantillaId) {
        return plantillaAnalisisRepository.findById(plantillaId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Plantilla no encontrada.", Map.of("code", "PLANTILLA_NO_ENCONTRADA", "plantillaId", plantillaId)));
    }

    private PlantillaCampo obtenerCampo(Long campoId) {
        return plantillaCampoRepository.findById(campoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Campo no encontrado.", Map.of("code", "CAMPO_NO_ENCONTRADO", "campoId", campoId)));
    }

    private void validarTipoAnalisis(Producto producto, TipoAnalisisPlantilla tipoAnalisisPlantilla) {
        TipoAnalisisCalidad tipo = producto.getTipoAnalisisCalidad();
        boolean permitido = switch (tipoAnalisisPlantilla) {
            case FISICO -> tipo == TipoAnalisisCalidad.FISICO || tipo == TipoAnalisisCalidad.AMBOS;
            case MICRO -> tipo == TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO || tipo == TipoAnalisisCalidad.AMBOS;
            case QUIMICO -> tipo == TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO || tipo == TipoAnalisisCalidad.AMBOS;
        };

        if (!permitido) {
            throw new CustomBusinessException(ApiErrorCode.CALIDAD_TIPO_ANALISIS_NO_PERMITIDO,
                    "El producto no permite el tipo de análisis solicitado.",
                    Map.of("code", "PRODUCTO_NO_PERMITE_TIPO_ANALISIS", "productoId", producto.getId(), "tipoAnalisis", tipo));
        }
    }

    private void desmarcarVigentesExistentes(PlantillaAnalisis vigente, Usuario usuario) {
        List<PlantillaAnalisis> vigentes = plantillaAnalisisRepository.findByProducto_IdAndTipoAnalisisAndVigenteTrue(
                vigente.getProducto().getId().longValue(), vigente.getTipoAnalisis());
        for (PlantillaAnalisis item : vigentes) {
            if (!item.getId().equals(vigente.getId())) {
                item.setVigente(false);
                item.setActualizadoPor(usuario);
            }
        }
        plantillaAnalisisRepository.saveAll(vigentes);
    }

    private PlantillaAnalisisResumenDTO toResumenDTO(PlantillaAnalisis plantilla) {
        return PlantillaAnalisisResumenDTO.builder()
                .id(plantilla.getId())
                .productoId(plantilla.getProducto() != null ? plantilla.getProducto().getId().longValue() : null)
                .nombre(plantilla.getNombre())
                .tipoAnalisis(plantilla.getTipoAnalisis())
                .version(plantilla.getVersion())
                .vigente(plantilla.isVigente())
                .activo(plantilla.isActivo())
                .cantidadCampos(plantilla.getCampos() != null ? plantilla.getCampos().size() : 0)
                .createdAt(plantilla.getCreatedAt())
                .updatedAt(plantilla.getUpdatedAt())
                .build();
    }

    private PlantillaAnalisisDetalleDTO toDetalleDTO(PlantillaAnalisis plantilla) {
        List<PlantillaCampoDTO> campos = plantilla.getCampos() == null ? List.of() : plantilla.getCampos().stream()
                .sorted(Comparator.comparing(PlantillaCampo::getOrden, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toCampoDTO)
                .toList();

        return PlantillaAnalisisDetalleDTO.builder()
                .id(plantilla.getId())
                .productoId(plantilla.getProducto() != null ? plantilla.getProducto().getId().longValue() : null)
                .nombre(plantilla.getNombre())
                .tipoAnalisis(plantilla.getTipoAnalisis())
                .version(plantilla.getVersion())
                .vigente(plantilla.isVigente())
                .activo(plantilla.isActivo())
                .createdAt(plantilla.getCreatedAt())
                .updatedAt(plantilla.getUpdatedAt())
                .campos(campos)
                .build();
    }

    private PlantillaCampoDTO toCampoDTO(PlantillaCampo campo) {
        return PlantillaCampoDTO.builder()
                .id(campo.getId())
                .codigo(campo.getCodigo())
                .label(campo.getLabel())
                .tipoCampo(campo.getTipoCampo())
                .requerido(campo.isRequerido())
                .orden(campo.getOrden())
                .configJson(campo.getConfigJson())
                .activo(campo.isActivo())
                .build();
    }
}
