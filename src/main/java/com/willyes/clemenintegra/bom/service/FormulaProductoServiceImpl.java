package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.dto.*;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.*;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.*;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.produccion.service.DisponibilidadInsumoService;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoResult;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormulaProductoServiceImpl implements FormulaProductoService {

    private static final Logger log = LoggerFactory.getLogger(FormulaProductoServiceImpl.class);
    private static final int MAX_VERSION_MINOR = 9;
    private static final Pattern VERSION_PATTERN = Pattern.compile("^V?(\\d+)(?:\\.(\\d+))?$", Pattern.CASE_INSENSITIVE);

    private final FormulaProductoRepository formulaRepository;
    private final BomMapper bomMapper;
    private final LoteProductoRepository loteProductoRepository;
    private final DisponibilidadInsumoService disponibilidadInsumoService;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<FormulaProductoSelectorDTO> listarResumen(EstadoFormula estado, String producto, Pageable pageable) {
        String filtroProducto = producto != null ? producto.trim() : null;
        if (filtroProducto != null && filtroProducto.isEmpty()) {
            filtroProducto = null;
        }

        return formulaRepository.findAllForSelector(estado, filtroProducto, pageable);
    }

    public List<FormulaProducto> listarTodas() {
        return formulaRepository.findAll();
    }

    public Optional<FormulaProducto> buscarPorId(Long id) {
        return formulaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormulaProductoDetalleDTO> buscarDetallePorId(Long id) {
        Optional<FormulaProducto> formulaOpt = formulaRepository.findByIdConDetalles(id);
        if (formulaOpt.isEmpty()) {
            return Optional.empty();
        }

        FormulaProducto formula = formulaOpt.get();
        List<DocumentoFormula> documentos = formulaRepository.findByIdWithDocumentos(id)
                .map(FormulaProducto::getDocumentos)
                .orElse(Collections.emptyList());

        return Optional.of(mapDetalleFormula(formula, documentos));
    }

    public FormulaProducto guardar(FormulaProducto formula) {
        if (formula.getEstado() == EstadoFormula.APROBADA) {
            formula.setActivo(true);
        }
        normalizarVersion(formula);
        return formulaRepository.save(formula);
    }

    public void eliminar(Long id) {
        formulaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public FormulaProducto cambiarEstado(Long formulaId, EstadoFormula nuevoEstado, Long usuarioId) {
        FormulaProducto formula = formulaRepository.findById(formulaId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "La fórmula solicitada no existe"));

        if (nuevoEstado == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "Debe proporcionar el nuevo estado de la fórmula");
        }

        EstadoFormula estadoActual = formula.getEstado();
        boolean transicionValida =
                (estadoActual == EstadoFormula.BORRADOR && nuevoEstado == EstadoFormula.EN_REVISION) ||
                (estadoActual == EstadoFormula.EN_REVISION && (nuevoEstado == EstadoFormula.APROBADA || nuevoEstado == EstadoFormula.RECHAZADA));

        if (!transicionValida) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                    String.format("Transición de estado no permitida de %s a %s", estadoActual, nuevoEstado));
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Usuario no encontrado para actualizar la fórmula"));

        LocalDateTime fechaActualizacion = LocalDateTime.now();

        if (nuevoEstado == EstadoFormula.APROBADA) {
            if (formula.getProducto() == null) {
                throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                        "La fórmula aprobada debe contar con un producto asociado");
            }
            formulaRepository.desactivarOtrasFormulasDelProducto(formula.getProducto(),
                    formula.getId(),
                    usuario,
                    fechaActualizacion);
            formula.setActivo(true);
        } else {
            formula.setActivo(false);
        }

        formula.setEstado(nuevoEstado);
        formula.setFechaActualizacion(fechaActualizacion);
        formula.setActualizadoPor(usuario);

        return formulaRepository.save(formula);
    }

    @Override
    @Transactional
    public FormulaProducto clonarFormula(Long formulaId, Long usuarioId) {
        FormulaProducto origen = formulaRepository.findById(formulaId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "La fórmula solicitada no existe"));

        if (origen.getProducto() == null || origen.getProducto().getId() == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "La fórmula no cuenta con un producto asociado válido para clonar");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Usuario no encontrado para clonar la fórmula"));

        Long productoId = origen.getProducto().getId().longValue();
        VersionParts ultimaVersion = formulaRepository.findTopByProductoIdOrderByVersionMajorDescVersionMinorDesc(productoId)
                .map(this::resolverVersionParts)
                .orElse(null);
        VersionParts siguienteVersion = calcularSiguienteVersion(ultimaVersion);
        String nuevaVersion = formatearVersion(siguienteVersion);
        LocalDateTime ahora = LocalDateTime.now();

        FormulaProducto clon = new FormulaProducto();
        clon.setProducto(origen.getProducto());
        clon.setVersion(nuevaVersion);
        clon.setVersionMajor(siguienteVersion.major());
        clon.setVersionMinor(siguienteVersion.minor());
        clon.setEstado(EstadoFormula.BORRADOR);
        clon.setActivo(false);
        clon.setObservacion(origen.getObservacion());
        clon.setFechaCreacion(ahora);
        clon.setFechaActualizacion(ahora);
        clon.setCreadoPor(usuario);
        clon.setActualizadoPor(usuario);

        if (origen.getDetalles() != null && !origen.getDetalles().isEmpty()) {
            List<DetalleFormula> detallesClonados = origen.getDetalles().stream()
                    .map(detalle -> {
                        DetalleFormula copia = new DetalleFormula();
                        copia.setFormula(clon);
                        copia.setInsumo(detalle.getInsumo());
                        copia.setUnidadMedida(detalle.getUnidadMedida());
                        copia.setCantidadNecesaria(detalle.getCantidadNecesaria());
                        copia.setObligatorio(detalle.getObligatorio());
                        return copia;
                    })
                    .collect(Collectors.toList());
            clon.setDetalles(detallesClonados);
        }

        if (origen.getDocumentos() != null && !origen.getDocumentos().isEmpty()) {
            List<DocumentoFormula> documentosClonados = origen.getDocumentos().stream()
                    .map(documento -> DocumentoFormula.builder()
                            .tipoDocumento(documento.getTipoDocumento())
                            .nombreArchivo(documento.getNombreArchivo())
                            .rutaArchivo(documento.getRutaArchivo())
                            .fechaSubida(documento.getFechaSubida())
                            .usuario(documento.getUsuario())
                            .formula(clon)
                            .build())
                    .collect(Collectors.toList());
            clon.setDocumentos(documentosClonados);
        }

        return formulaRepository.save(clon);
    }

    static VersionParts calcularSiguienteVersion(VersionParts versionActual) {
        if (versionActual == null) {
            return new VersionParts(1, 0);
        }
        if (versionActual.minor() < MAX_VERSION_MINOR) {
            return new VersionParts(versionActual.major(), versionActual.minor() + 1);
        }
        return new VersionParts(versionActual.major() + 1, 0);
    }

    static String formatearVersion(VersionParts version) {
        if (version == null) {
            return null;
        }
        return String.format("V%d.%d", version.major(), version.minor());
    }

    static VersionParts parsearVersion(String version) {
        if (version == null || version.isBlank()) {
            return null;
        }
        var matcher = VERSION_PATTERN.matcher(version.trim());
        if (!matcher.matches()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "Formato de versión inválido para la fórmula");
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
        return new VersionParts(major, minor);
    }

    private VersionParts resolverVersionParts(FormulaProducto formula) {
        if (formula == null) {
            return null;
        }
        if (formula.getVersionMajor() != null && formula.getVersionMinor() != null) {
            return new VersionParts(formula.getVersionMajor(), formula.getVersionMinor());
        }
        return parsearVersion(formula.getVersion());
    }

    private void normalizarVersion(FormulaProducto formula) {
        if (formula == null) {
            return;
        }
        VersionParts parts = resolverVersionParts(formula);
        if (parts == null) {
            return;
        }
        formula.setVersionMajor(parts.major());
        formula.setVersionMinor(parts.minor());
        formula.setVersion(formatearVersion(parts));
    }

    static class VersionParts {
        private final int major;
        private final int minor;

        VersionParts(int major, int minor) {
            this.major = major;
            this.minor = minor;
        }

        int major() {
            return major;
        }

        int minor() {
            return minor;
        }
    }

    private FormulaProductoDetalleDTO mapDetalleFormula(FormulaProducto formula, List<DocumentoFormula> documentos) {
        FormulaProductoDetalleDTO dto = new FormulaProductoDetalleDTO();
        dto.id = formula.getId();
        dto.codigo = formula.getProducto() != null ? formula.getProducto().getCodigoSku() : null;
        dto.version = formula.getVersion();
        dto.estado = formula.getEstado() != null ? formula.getEstado().name() : null;
        dto.productoId = formula.getProducto() != null && formula.getProducto().getId() != null
                ? formula.getProducto().getId().longValue()
                : null;
        dto.productoNombre = formula.getProducto() != null ? formula.getProducto().getNombre() : null;
        dto.fechaCreacion = formula.getFechaCreacion();
        dto.creadoPorNombre = formula.getCreadoPor() != null ? formula.getCreadoPor().getNombreCompleto() : null;
        dto.observacion = formula.getObservacion();
        dto.fechaActualizacion = formula.getFechaActualizacion();
        dto.actualizadoPorNombre = formula.getActualizadoPor() != null ? formula.getActualizadoPor().getNombreCompleto() : null;
        dto.detalles = formula.getDetalles() == null ? Collections.emptyList() : formula.getDetalles().stream()
                .map(detalle -> {
                    DetalleFormulaDetalleDTO detalleDto = new DetalleFormulaDetalleDTO();
                    detalleDto.id = detalle.getId();
                    detalleDto.insumoId = detalle.getInsumo() != null && detalle.getInsumo().getId() != null
                            ? detalle.getInsumo().getId().longValue()
                            : null;
                    detalleDto.insumoNombre = detalle.getInsumo() != null ? detalle.getInsumo().getNombre() : null;
                    detalleDto.cantidad = detalle.getCantidadNecesaria();
                    detalleDto.unidad = detalle.getUnidadMedida() != null ? detalle.getUnidadMedida().getSimbolo() : null;
                    return detalleDto;
                })
                .collect(Collectors.toList());
        List<DocumentoFormula> documentosSeguro = documentos == null ? Collections.emptyList() : documentos;
        dto.documentos = documentosSeguro.stream()
                .map(bomMapper::toResponseDTO)
                .collect(Collectors.toList());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public FormulaProductoResponse obtenerFormulaActivaPorProducto(Long productoId, BigDecimal cantidad) {
        FormulaProducto formula = formulaRepository
                .findByProductoIdAndEstadoAndActivoTrue(productoId, EstadoFormula.APROBADA)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe fórmula activa aprobada para este producto."));

        if (formula.getDocumentos() != null) {
            formula.getDocumentos().size();
        }

        FormulaProductoResponse response = bomMapper.toResponseDTO(formula);
        UnidadMedida unidadProductoFabricable = formula.getProducto() != null
                ? formula.getProducto().getUnidadMedida()
                : null;
        response.unidadBaseFormula = unidadProductoFabricable != null ? unidadProductoFabricable.getSimbolo() : null;
        response.cantidadBaseFormula = BigDecimal.ONE;

        BigDecimal cantidadProduccion = (cantidad != null && cantidad.compareTo(BigDecimal.ZERO) > 0)
                ? cantidad
                : BigDecimal.ONE;

        boolean todosSuficientes = true;

        if (response.detalles != null && formula.getDetalles() != null) {
            List<DetalleFormula> detallesEntidad = formula.getDetalles();
            for (int i = 0; i < detallesEntidad.size(); i++) {
                DetalleFormula entidad = detallesEntidad.get(i);
                DetalleFormulaResponse dto = response.detalles.get(i);

                BigDecimal totalNecesaria = entidad.getCantidadNecesaria().multiply(cantidadProduccion);
                dto.cantidadTotalNecesaria = totalNecesaria;

                Long insumoId = entidad.getInsumo().getId().longValue();
                DisponibilidadInsumoDTO disponibilidad = new DisponibilidadInsumoDTO();

                java.util.Map<EstadoLote, BigDecimal> totales = new java.util.EnumMap<>(EstadoLote.class);
                for (EstadoLote e : EstadoLote.values()) {
                    totales.put(e, BigDecimal.ZERO);
                }

                java.util.List<Object[]> filas = loteProductoRepository.sumarPorEstado(insumoId);
                for (Object[] fila : filas) {
                    EstadoLote estado = (EstadoLote) fila[0];
                    BigDecimal total = (BigDecimal) fila[1];
                    if (total != null) {
                        totales.put(estado, total);
                    }
                }

                java.util.List<LoteResumenDTO> lotes = loteProductoRepository.listarLotesPorProducto(insumoId);
                BigDecimal vencidoExtra = BigDecimal.ZERO;
                java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
                for (LoteResumenDTO lote : lotes) {
                    if (lote.getFechaVencimiento() != null && lote.getFechaVencimiento().isBefore(ahora)) {
                        BigDecimal st = lote.getStockDisponible();
                        vencidoExtra = vencidoExtra.add(st);
                        totales.put(lote.getEstado(), totales.get(lote.getEstado()).subtract(st));
                    }
                }
                totales.put(EstadoLote.VENCIDO, totales.get(EstadoLote.VENCIDO).add(vencidoExtra));

                disponibilidad.setDisponible(totales.get(EstadoLote.DISPONIBLE).add(totales.get(EstadoLote.LIBERADO)));
                disponibilidad.setEnCuarentena(totales.get(EstadoLote.EN_CUARENTENA));
                disponibilidad.setRetenido(totales.get(EstadoLote.RETENIDO));
                disponibilidad.setRechazado(totales.get(EstadoLote.RECHAZADO));
                disponibilidad.setVencido(totales.get(EstadoLote.VENCIDO));
                BigDecimal totalProducto = disponibilidad.getDisponible()
                        .add(disponibilidad.getEnCuarentena())
                        .add(disponibilidad.getRetenido())
                        .add(disponibilidad.getRechazado())
                        .add(disponibilidad.getVencido());
                disponibilidad.setTotalProducto(totalProducto);

                List<Long> almacenesPreferidos = disponibilidadInsumoService
                        .resolverAlmacenesPreferidos(entidad.getInsumo());
                DistribucionFefoResult fefoResult = disponibilidadInsumoService.calcularDisponibilidad(
                        insumoId,
                        totalNecesaria,
                        almacenesPreferidos,
                        true);

                BigDecimal stockLibre = Optional.ofNullable(fefoResult.getStockLibreTotal())
                        .orElse(BigDecimal.ZERO);
                BigDecimal faltanteFefo = Optional.ofNullable(fefoResult.getFaltante())
                        .orElse(BigDecimal.ZERO);
                boolean suficiente = fefoResult.isSuficiente();

                Integer maxProducible = null;
                if (entidad.getCantidadNecesaria() != null
                        && entidad.getCantidadNecesaria().compareTo(BigDecimal.ZERO) > 0) {
                    maxProducible = stockLibre.divide(entidad.getCantidadNecesaria(), 0, RoundingMode.DOWN).intValue();
                }

                String motivo = "OK";
                if (!suficiente) {
                    motivo = "STOCK_LIBRE_INSUFICIENTE";
                    if (disponibilidad.getEnCuarentena().compareTo(BigDecimal.ZERO) > 0) {
                        motivo = "CUARENTENA";
                    } else if (disponibilidad.getRetenido().compareTo(BigDecimal.ZERO) > 0) {
                        motivo = "RETENIDO";
                    } else if (disponibilidad.getVencido().compareTo(BigDecimal.ZERO) > 0) {
                        motivo = "VENCIDO";
                    } else if (disponibilidad.getRechazado().compareTo(BigDecimal.ZERO) > 0) {
                        motivo = "RECHAZADO";
                    }
                    log.info("FORMULA_DISPONIBILIDAD insumoId={} requerido={} stockLibreFefo={} faltanteFefo={} motivo={} almacenesPreferidos={}",
                            insumoId,
                            totalNecesaria,
                            stockLibre,
                            faltanteFefo,
                            motivo,
                            almacenesPreferidos);
                }

                dto.stockDisponible = stockLibre;
                dto.stockLibreFefo = stockLibre;
                dto.faltanteFefo = faltanteFefo;
                dto.maxProducible = maxProducible;
                dto.maximoProducible = maxProducible;
                dto.estadoStock = suficiente ? "SUFICIENTE" : "INSUFICIENTE";
                dto.disponibilidad = disponibilidad;
                dto.bloqueante = new BloqueanteDTO(!suficiente, motivo);
                dto.lotes = lotes;
                UnidadMedida unidadInsumo = entidad.getInsumo() != null ? entidad.getInsumo().getUnidadMedida() : null;
                dto.unidadInsumoSimbolo = unidadInsumo != null ? unidadInsumo.getSimbolo() : null;
                dto.unidadInsumoNombre = unidadInsumo != null ? unidadInsumo.getNombre() : null;
                dto.unidadInsumoNombrePlural = unidadInsumo != null ? unidadInsumo.getNombrePlural() : null;
                dto.unidadProductoFabricableSimbolo = unidadProductoFabricable != null
                        ? unidadProductoFabricable.getSimbolo()
                        : null;
                dto.unidadProductoFabricableNombre = unidadProductoFabricable != null
                        ? unidadProductoFabricable.getNombre()
                        : null;
                dto.unidadProductoFabricableNombrePlural = unidadProductoFabricable != null
                        ? unidadProductoFabricable.getNombrePlural()
                        : null;

                todosSuficientes = todosSuficientes && suficiente;
            }
        }

        response.disponibilidadSuficiente = todosSuficientes;
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public FormulaActivaProduccionDTO obtenerFormulaActivaProduccion(Long productoId) {
        FormulaProducto formula = formulaRepository
                .findByProductoIdAndEstadoAndActivoTrue(productoId, EstadoFormula.APROBADA)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                        "El producto seleccionado no tiene una fórmula activa aprobada."));

        if (formula.getDetalles() == null) {
            formula.setDetalles(Collections.emptyList());
        } else {
            formula.getDetalles().size();
        }

        return bomMapper.toFormulaActivaProduccionDTO(formula);
    }
}
