package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.*;
import com.willyes.clemenintegra.calidad.mapper.EspecificacionFisicoQuimicaMapper;
import com.willyes.clemenintegra.calidad.mapper.PlantillaAnalisisMicrobiologicoMapper;
import com.willyes.clemenintegra.calidad.model.EspecificacionFisicoQuimica;
import com.willyes.clemenintegra.calidad.model.ParametroAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.PlantillaAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.repository.EspecificacionFisicoQuimicaRepository;
import com.willyes.clemenintegra.calidad.repository.PlantillaAnalisisMicrobiologicoRepository;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EspecificacionesCalidadServiceImpl implements EspecificacionesCalidadService {

    private final ProductoRepository productoRepository;
    private final EspecificacionFisicoQuimicaRepository especificacionFisicoQuimicaRepository;
    private final PlantillaAnalisisMicrobiologicoRepository plantillaRepository;
    private final EspecificacionFisicoQuimicaMapper especificacionFisicoQuimicaMapper;
    private final PlantillaAnalisisMicrobiologicoMapper plantillaMapper;
    private final UsuarioService usuarioService;

    /**
     * TODO: integrar validaciones automáticas de resultados de evaluación contra estas especificaciones.
     */
    @Transactional(readOnly = true)
    @Override
    public EspecificacionesFisicoQuimicasProductoDTO listarFisicoQuimicasPorProducto(Long productoId) {
        Producto producto = obtenerProducto(productoId);
        List<EspecificacionFisicoQuimicaDTO> parametros = especificacionFisicoQuimicaRepository
                .findByProducto_IdAndActivoTrueOrderByNombreParametroAsc(productoId)
                .stream()
                .map(especificacionFisicoQuimicaMapper::toDTO)
                .toList();

        return EspecificacionesFisicoQuimicasProductoDTO.builder()
                .productoId(producto.getId() != null ? producto.getId().longValue() : null)
                .codigoSku(producto.getCodigoSku())
                .nombreProducto(producto.getNombre())
                .parametros(parametros)
                .build();
    }

    @Transactional
    @Override
    public List<EspecificacionFisicoQuimicaDTO> crearFisicoQuimicas(Long productoId, List<EspecificacionFisicoQuimicaRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "Debe enviar al menos un parámetro para crear especificaciones físico/químicas.");
        }
        Producto producto = obtenerProducto(productoId);
        validarTipoAnalisis(producto, TipoEspecificacion.FISICO_QUIMICA);
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        List<EspecificacionFisicoQuimica> entidades = requests.stream()
                .map(this::validarEspecificacionFisicoQuimica)
                .map(req -> especificacionFisicoQuimicaMapper.toEntity(req, producto, usuario))
                .toList();

        List<EspecificacionFisicoQuimica> guardadas = especificacionFisicoQuimicaRepository.saveAll(entidades);
        return guardadas.stream().map(especificacionFisicoQuimicaMapper::toDTO).toList();
    }

    @Transactional
    @Override
    public EspecificacionFisicoQuimicaDTO actualizarFisicoQuimica(Long id, EspecificacionFisicoQuimicaRequest request) {
        EspecificacionFisicoQuimica entidad = especificacionFisicoQuimicaRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Especificación físico/química no encontrada.", Map.of("id", id)));
        validarTipoAnalisis(entidad.getProducto(), TipoEspecificacion.FISICO_QUIMICA);
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        validarEspecificacionFisicoQuimica(request);
        entidad.setNombreParametro(request.getNombreParametro());
        entidad.setUnidad(request.getUnidad());
        entidad.setLimiteInferior(request.getLimiteInferior());
        entidad.setLimiteSuperior(request.getLimiteSuperior());
        entidad.setObservaciones(request.getObservaciones());
        if (request.getActivo() != null) {
            entidad.setActivo(request.getActivo());
        }
        entidad.setActualizadoPor(usuario);

        EspecificacionFisicoQuimica guardada = especificacionFisicoQuimicaRepository.save(entidad);
        return especificacionFisicoQuimicaMapper.toDTO(guardada);
    }

    @Transactional
    @Override
    public void eliminarFisicoQuimica(Long id) {
        EspecificacionFisicoQuimica entidad = especificacionFisicoQuimicaRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Especificación físico/química no encontrada.", Map.of("id", id)));
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        entidad.setActivo(false);
        entidad.setActualizadoPor(usuario);
        especificacionFisicoQuimicaRepository.save(entidad);
    }

    @Transactional(readOnly = true)
    @Override
    public List<PlantillaAnalisisMicrobiologicoResumenDTO> listarPlantillasMicroPorProducto(Long productoId) {
        obtenerProducto(productoId);
        return plantillaRepository.findByProducto_IdOrderByVersionDesc(productoId)
                .stream()
                .map(plantillaMapper::toResumenDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public PlantillaAnalisisMicrobiologicoDetalleDTO obtenerDetallePlantillaMicro(Long plantillaId) {
        PlantillaAnalisisMicrobiologico plantilla = plantillaRepository.findById(plantillaId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Plantilla microbiológica no encontrada.", Map.of("plantillaId", plantillaId)));
        return plantillaMapper.toDetalleDTO(plantilla);
    }

    @Transactional
    @Override
    public PlantillaAnalisisMicrobiologicoDetalleDTO crearPlantillaMicro(Long productoId, PlantillaAnalisisMicrobiologicoRequest request) {
        Producto producto = obtenerProducto(productoId);
        validarTipoAnalisis(producto, TipoEspecificacion.MICROBIOLOGICA);
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        List<ParametroAnalisisMicrobiologico> parametros = construirParametrosDesdeRequest(request, productoId);
        PlantillaAnalisisMicrobiologico plantilla = construirPlantilla(producto, usuario, request, parametros);
        PlantillaAnalisisMicrobiologico guardada = plantillaRepository.save(plantilla);

        if (guardada.isVigente()) {
            marcarPlantillaVigente(producto, guardada, usuario);
        }

        return plantillaMapper.toDetalleDTO(guardada);
    }

    @Transactional
    @Override
    public PlantillaAnalisisMicrobiologicoDetalleDTO clonarPlantillaMicroComoNuevaVersion(Long plantillaId) {
        PlantillaAnalisisMicrobiologico base = plantillaRepository.findById(plantillaId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Plantilla microbiológica no encontrada.", Map.of("plantillaId", plantillaId)));
        Producto producto = base.getProducto();
        validarTipoAnalisis(producto, TipoEspecificacion.MICROBIOLOGICA);
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        PlantillaAnalisisMicrobiologico clonada = construirPlantilla(producto, usuario,
                PlantillaAnalisisMicrobiologicoRequest.builder()
                        .nombre(base.getNombre())
                        .descripcion(base.getDescripcion())
                        .fechaVigenciaDesde(base.getFechaVigenciaDesde())
                        .fechaVigenciaHasta(base.getFechaVigenciaHasta())
                        .vigente(true)
                        .build(),
                clonarParametros(base.getParametros()));

        PlantillaAnalisisMicrobiologico guardada = plantillaRepository.save(clonada);
        marcarPlantillaVigente(producto, guardada, usuario);
        return plantillaMapper.toDetalleDTO(guardada);
    }

    private Producto obtenerProducto(Long productoId) {
        return productoRepository.findById(productoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Producto no encontrado.", Map.of("productoId", productoId)));
    }

    private void validarTipoAnalisis(Producto producto, TipoEspecificacion tipo) {
        TipoAnalisisCalidad tipoAnalisis = producto.getTipoAnalisisCalidad();
        boolean permitido = switch (tipo) {
            case FISICO_QUIMICA -> tipoAnalisis == TipoAnalisisCalidad.FISICO || tipoAnalisis == TipoAnalisisCalidad.AMBOS;
            case MICROBIOLOGICA -> tipoAnalisis == TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO || tipoAnalisis == TipoAnalisisCalidad.AMBOS;
        };
        if (!permitido) {
            throw new CustomBusinessException(ApiErrorCode.CALIDAD_TIPO_ANALISIS_NO_PERMITIDO,
                    "El producto no permite el tipo de especificación solicitado.",
                    Map.of("productoId", producto.getId(), "tipoAnalisis", tipoAnalisis));
        }
    }

    private EspecificacionFisicoQuimicaRequest validarEspecificacionFisicoQuimica(EspecificacionFisicoQuimicaRequest request) {
        if (request == null || request.getNombreParametro() == null || request.getNombreParametro().isBlank()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "El nombre del parámetro físico/químico es obligatorio.");
        }
        return request;
    }

    private List<ParametroAnalisisMicrobiologico> construirParametrosDesdeRequest(PlantillaAnalisisMicrobiologicoRequest request,
                                                                                   Long productoId) {
        if (request == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "La solicitud de plantilla microbiológica es obligatoria.");
        }
        if (request.getParametros() != null && !request.getParametros().isEmpty()) {
            return request.getParametros().stream()
                    .map(this::mapParametro)
                    .toList();
        }
        if (request.getClonarDesdeId() != null) {
            PlantillaAnalisisMicrobiologico base = plantillaRepository.findById(request.getClonarDesdeId())
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                            "Plantilla base no encontrada para clonar.",
                            Map.of("plantillaId", request.getClonarDesdeId())));
            if (base.getProducto() == null || !base.getProducto().getId().equals(productoId.intValue())) {
                throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                        "La plantilla base no pertenece al producto indicado.",
                        Map.of("productoId", productoId, "plantillaId", request.getClonarDesdeId()));
            }
            return clonarParametros(base.getParametros());
        }
        throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                "Debe informar parámetros o indicar una plantilla a clonar.");
    }

    private PlantillaAnalisisMicrobiologico construirPlantilla(Producto producto,
                                                               Usuario usuario,
                                                               PlantillaAnalisisMicrobiologicoRequest request,
                                                               List<ParametroAnalisisMicrobiologico> parametros) {
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "El nombre de la plantilla microbiológica es obligatorio.");
        }
        Integer siguienteVersion = plantillaRepository.findTopByProducto_IdOrderByVersionDesc(producto.getId().longValue())
                .map(p -> p.getVersion() + 1)
                .orElse(1);
        boolean vigente = request.getVigente() == null || request.getVigente();
        LocalDate desde = request.getFechaVigenciaDesde();
        LocalDate hasta = request.getFechaVigenciaHasta();

        PlantillaAnalisisMicrobiologico plantilla = PlantillaAnalisisMicrobiologico.builder()
                .producto(producto)
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .version(siguienteVersion)
                .vigente(vigente)
                .fechaVigenciaDesde(desde)
                .fechaVigenciaHasta(hasta)
                .activo(true)
                .creadoPor(usuario)
                .parametros(new ArrayList<>())
                .build();

        parametros.forEach(parametro -> parametro.setPlantilla(plantilla));
        plantilla.getParametros().addAll(parametros);
        return plantilla;
    }

    private List<ParametroAnalisisMicrobiologico> clonarParametros(List<ParametroAnalisisMicrobiologico> parametros) {
        if (parametros == null) {
            return List.of();
        }
        return parametros.stream()
                .sorted(Comparator.comparing(ParametroAnalisisMicrobiologico::getOrden,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(param -> ParametroAnalisisMicrobiologico.builder()
                        .nombreEnsayo(param.getNombreEnsayo())
                        .metodo(param.getMetodo())
                        .unidad(param.getUnidad())
                        .especificacion(param.getEspecificacion())
                        .tipoResultado(param.getTipoResultado())
                        .orden(param.getOrden())
                        .build())
                .toList();
    }

    private ParametroAnalisisMicrobiologico mapParametro(ParametroAnalisisMicrobiologicoDTO dto) {
        if (dto.getNombreParametro() == null || dto.getNombreParametro().isBlank()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "El nombre del parámetro microbiológico es obligatorio.");
        }
        if (dto.getTipoResultado() == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "El tipo de resultado del parámetro microbiológico es obligatorio.");
        }
        return ParametroAnalisisMicrobiologico.builder()
                .nombreEnsayo(dto.getNombreParametro())
                .unidad(dto.getUnidad())
                .especificacion(dto.getCriterioAceptacion())
                .tipoResultado(dto.getTipoResultado())
                .orden(dto.getOrden())
                .build();
    }

    private void marcarPlantillaVigente(Producto producto, PlantillaAnalisisMicrobiologico nueva, Usuario usuario) {
        List<PlantillaAnalisisMicrobiologico> existentes = plantillaRepository.findByProducto_IdOrderByVersionDesc(
                producto.getId().longValue());
        for (PlantillaAnalisisMicrobiologico plantilla : existentes) {
            if (!plantilla.getId().equals(nueva.getId()) && plantilla.isVigente()) {
                plantilla.setVigente(false);
                plantilla.setActualizadoPor(usuario);
                if (plantilla.getFechaVigenciaHasta() == null) {
                    plantilla.setFechaVigenciaHasta(LocalDate.now());
                }
            }
        }
        plantillaRepository.saveAll(existentes);
        producto.setPlantillaAnalisisMicrobiologico(nueva);
        productoRepository.save(producto);
    }

    private enum TipoEspecificacion {
        FISICO_QUIMICA,
        MICROBIOLOGICA
    }
}
