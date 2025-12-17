package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoDetalleRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoLoteResponseDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.mapper.ConteoCiclicoMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoConteoCiclico;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConteoCiclicoService {

    private static final EnumSet<EstadoLote> LOTES_OPERABLES = EnumSet.of(EstadoLote.DISPONIBLE, EstadoLote.LIBERADO);
    private static final EnumSet<EstadoLote> LOTES_VISIBLES_EN_CONTEO = EnumSet.allOf(EstadoLote.class);

    private final ConteoCiclicoRepository conteoRepository;
    private final ConteoCiclicoDetalleRepository detalleRepository;
    private final AlmacenRepository almacenRepository;
    private final ProductoRepository productoRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final UbicacionFisicaRepository ubicacionFisicaRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    private final MovimientoInventarioService movimientoInventarioService;
    private final UsuarioService usuarioService;
    private final ConteoCiclicoMapper mapper;

    public Page<ConteoCiclicoResponseDTO> listar(Integer almacenId, String estado, Pageable pageable) {
        EstadoConteoCiclico estadoEnum = parseEstado(estado);
        Page<ConteoCiclico> conteos = conteoRepository.buscar(almacenId, estadoEnum, pageable);
        return conteos.map(mapper::toResponse);
    }

    public ConteoCiclicoResponseDTO obtenerPorId(Long conteoId) {
        ConteoCiclico conteo = conteoRepository.findByIdWithDetalles(conteoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Conteo no encontrado"));
        return mapper.toResponse(conteo);
    }

    public List<ConteoCiclicoLoteResponseDTO> listarLotesParaConteo(Long conteoId,
                                                                   Long productoId,
                                                                   Long ubicacionFisicaId,
                                                                   String q) {
        if (productoId == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Debe especificar el producto a contar");
        }

        ConteoCiclico conteo = conteoRepository.findById(conteoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Conteo no encontrado"));

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Producto no encontrado"));

        Integer almacenId = conteo.getAlmacen() != null ? conteo.getAlmacen().getId() : null;
        if (almacenId == null) {
            throw new CustomBusinessException(ApiErrorCode.ERROR_INTERNO, "El conteo no tiene almacén asociado");
        }

        UbicacionFisica ubicacion = null;
        if (ubicacionFisicaId != null) {
            ubicacion = ubicacionFisicaRepository.findByIdAndActivoTrue(ubicacionFisicaId)
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.UBICACION_NO_ENCONTRADA,
                            "Ubicación no encontrada"));
            if (ubicacion.getAlmacen() == null || !Objects.equals(ubicacion.getAlmacen().getId(), almacenId)) {
                throw new CustomBusinessException(ApiErrorCode.UBICACION_NO_PERTENECE_ALMACEN,
                        "La ubicación no pertenece al almacén del conteo");
            }
        }

        String filtroTexto = (q == null || q.isBlank()) ? null : q.trim();

        log.debug("[ConteoCiclico] listarLotes conteoId={}, almacenId={}, productoId={}, ubicacionFisicaId={}, search={}",
                conteoId, almacenId, producto.getId(), ubicacion != null ? ubicacion.getId() : null, filtroTexto);

        List<LoteProducto> lotes = loteProductoRepository.buscarParaConteo(
                producto.getId().longValue(),
                almacenId,
                ubicacion != null ? ubicacion.getId() : null,
                filtroTexto,
                LOTES_VISIBLES_EN_CONTEO);

        log.debug("[ConteoCiclico] lotes encontrados antes de mapear: {}", lotes.size());
        lotes.stream()
                .limit(3)
                .forEach(lp -> log.debug(
                        "[ConteoCiclico] lote id={} codigo={} ubicacionId={} estado={} stockLote={}",
                        lp.getId(),
                        lp.getCodigoLote(),
                        lp.getUbicacionFisica() != null ? lp.getUbicacionFisica().getId() : null,
                        lp.getEstado(),
                        lp.getStockLote()));

        return lotes.stream()
                .map(lp -> ConteoCiclicoLoteResponseDTO.builder()
                        .id(lp.getId())
                        .codigoLote(lp.getCodigoLote())
                        .stockLote(Optional.ofNullable(lp.getStockLote()).orElse(BigDecimal.ZERO))
                        .fechaVencimiento(lp.getFechaVencimiento())
                        .ubicacionFisicaId(lp.getUbicacionFisica() != null ? lp.getUbicacionFisica().getId() : null)
                        .ubicacionCodigo(lp.getUbicacionFisica() != null ? lp.getUbicacionFisica().getCodigo() : null)
                        .build())
                .toList();
    }

    @Transactional
    public ConteoCiclicoResponseDTO crearConteo(ConteoCiclicoRequestDTO request) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        Almacen almacen = almacenRepository.findById(request.getAlmacenId().longValue())
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Almacén no encontrado"));

        ConteoCiclico conteo = ConteoCiclico.builder()
                .almacen(almacen)
                .estado(EstadoConteoCiclico.BORRADOR)
                .creadoPor(usuario)
                .build();

        ConteoCiclico guardado = conteoRepository.save(conteo);
        return mapper.toResponse(guardado);
    }

    @Transactional
    public ConteoCiclicoResponseDTO agregarDetalles(Long conteoId, List<ConteoCiclicoDetalleRequestDTO> detallesRequest) {
        if (CollectionUtils.isEmpty(detallesRequest)) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Debe enviar al menos un detalle");
        }

        ConteoCiclico conteo = conteoRepository.findByIdWithDetallesForUpdate(conteoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Conteo no encontrado"));

        if (conteo.getEstado() == EstadoConteoCiclico.CERRADO || conteo.getEstado() == EstadoConteoCiclico.APLICADO) {
            throw new CustomBusinessException(ApiErrorCode.CONTEO_ESTADO_INVALIDO,
                    "No se pueden modificar detalles en el estado actual");
        }

        Integer almacenId = conteo.getAlmacen() != null ? conteo.getAlmacen().getId() : null;
        for (ConteoCiclicoDetalleRequestDTO req : detallesRequest) {
            ConteoCiclicoDetalle detalle = construirDetalle(conteo, req, almacenId);
            conteo.getDetalles().add(detalle);
        }

        ConteoCiclico actualizado = conteoRepository.save(conteo);
        return mapper.toResponse(actualizado);
    }

    @Transactional
    public ConteoCiclicoResponseDTO actualizarConteo(Long conteoId, List<ConteoCiclicoDetalleRequestDTO> detallesRequest) {
        if (CollectionUtils.isEmpty(detallesRequest)) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Debe enviar al menos un detalle");
        }

        ConteoCiclico conteo = conteoRepository.findByIdWithDetallesForUpdate(conteoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Conteo no encontrado"));

        if (conteo.getEstado() == EstadoConteoCiclico.CERRADO || conteo.getEstado() == EstadoConteoCiclico.APLICADO) {
            throw new CustomBusinessException(ApiErrorCode.CONTEO_ESTADO_INVALIDO,
                    "No se pueden modificar detalles en el estado actual");
        }

        conteo.getDetalles().clear();

        Integer almacenId = conteo.getAlmacen() != null ? conteo.getAlmacen().getId() : null;
        for (ConteoCiclicoDetalleRequestDTO req : detallesRequest) {
            ConteoCiclicoDetalle detalle = construirDetalle(conteo, req, almacenId);
            conteo.getDetalles().add(detalle);
        }

        ConteoCiclico actualizado = conteoRepository.save(conteo);
        return mapper.toResponse(actualizado);
    }

    @Transactional
    public ConteoCiclicoResponseDTO marcarEnConteo(Long conteoId) {
        ConteoCiclico conteo = cambiarEstado(conteoId, EstadoConteoCiclico.EN_CONTEO);
        return mapper.toResponse(conteo);
    }

    @Transactional
    public ConteoCiclicoResponseDTO cerrar(Long conteoId) {
        ConteoCiclico conteo = cambiarEstado(conteoId, EstadoConteoCiclico.CERRADO);
        return mapper.toResponse(conteo);
    }

    @Transactional
    public ConteoCiclicoResponseDTO aplicar(Long conteoId, String idempotencyKey) {
        ConteoCiclico conteo = conteoRepository.findByIdWithDetallesForUpdate(conteoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Conteo no encontrado"));

        if (conteo.getEstado() != EstadoConteoCiclico.CERRADO) {
            throw new CustomBusinessException(ApiErrorCode.CONTEO_ESTADO_INVALIDO,
                    "El conteo debe estar cerrado para aplicarse");
        }
        if (conteo.getAplicadoEn() != null || conteo.getEstado() == EstadoConteoCiclico.APLICADO) {
            throw new CustomBusinessException(ApiErrorCode.CONTEO_YA_APLICADO,
                    "El conteo ya fue aplicado previamente");
        }
        if (StringUtils.hasText(idempotencyKey)) {
            if (conteo.getIdempotencyKeyAplicar() != null && !idempotencyKey.equals(conteo.getIdempotencyKeyAplicar())) {
                throw new CustomBusinessException(ApiErrorCode.CONTEO_APLICACION_DUPLICADA,
                        "La llave de idempotencia no coincide con la solicitud previa");
            }
            conteo.setIdempotencyKeyAplicar(idempotencyKey);
        }
        if (CollectionUtils.isEmpty(conteo.getDetalles())) {
            throw new CustomBusinessException(ApiErrorCode.CONTEO_SIN_DETALLES,
                    "No hay detalles para aplicar");
        }

        Integer almacenId = conteo.getAlmacen() != null ? conteo.getAlmacen().getId() : null;
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        for (ConteoCiclicoDetalle detalle : conteo.getDetalles()) {
            LoteProducto lote = resolverLoteParaDetalle(detalle, almacenId);
            BigDecimal stockActual = obtenerStockActual(detalle, lote, almacenId);
            BigDecimal diferencia = detalle.getConteoFisico().subtract(stockActual)
                    .setScale(2, RoundingMode.HALF_UP);
            detalle.setDiferencia(diferencia);
            detalleRepository.save(detalle);

            if (diferencia.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            registrarAjuste(conteo, detalle, lote, diferencia, idempotencyKey);
        }

        conteo.setEstado(EstadoConteoCiclico.APLICADO);
        conteo.setAplicadoEn(LocalDateTime.now());
        conteo.setAplicadoPor(usuario);
        ConteoCiclico aplicado = conteoRepository.save(conteo);
        return mapper.toResponse(aplicado);
    }

    private ConteoCiclico cambiarEstado(Long conteoId, EstadoConteoCiclico destino) {
        ConteoCiclico conteo = conteoRepository.findByIdWithDetallesForUpdate(conteoId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Conteo no encontrado"));
        EstadoConteoCiclico estadoActual = conteo.getEstado();
        if (estadoActual == EstadoConteoCiclico.APLICADO) {
            throw new CustomBusinessException(ApiErrorCode.CONTEO_YA_APLICADO,
                    "El conteo ya fue aplicado");
        }
        if (estadoActual == destino) {
            return conteo;
        }
        if (destino == EstadoConteoCiclico.EN_CONTEO && estadoActual == EstadoConteoCiclico.BORRADOR) {
            conteo.setEstado(destino);
        } else if (destino == EstadoConteoCiclico.CERRADO && estadoActual == EstadoConteoCiclico.EN_CONTEO) {
            conteo.setEstado(destino);
        } else {
            throw new CustomBusinessException(ApiErrorCode.CONTEO_ESTADO_INVALIDO,
                    "Transición de estado no permitida");
        }
        return conteoRepository.save(conteo);
    }

    private ConteoCiclicoDetalle construirDetalle(ConteoCiclico conteo,
                                                  ConteoCiclicoDetalleRequestDTO req,
                                                  Integer almacenId) {
        Producto producto = productoRepository.findById(req.getProductoId())
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Producto no encontrado"));

        LoteProducto lote = null;
        if (req.getLoteProductoId() != null) {
            lote = loteProductoRepository.findById(req.getLoteProductoId())
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                            "Lote no encontrado"));
            validarLoteEnConteo(lote, producto, almacenId);
        }

        UbicacionFisica ubicacion = null;
        if (req.getUbicacionFisicaId() != null) {
            ubicacion = ubicacionFisicaRepository.findByIdAndActivoTrue(req.getUbicacionFisicaId())
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.UBICACION_NO_ENCONTRADA,
                            "Ubicación no encontrada"));
            if (ubicacion.getAlmacen() == null || !Objects.equals(ubicacion.getAlmacen().getId(), almacenId)) {
                throw new CustomBusinessException(ApiErrorCode.UBICACION_NO_PERTENECE_ALMACEN,
                        "La ubicación no pertenece al almacén del conteo");
            }
            if (lote != null && (lote.getUbicacionFisica() == null
                    || !Objects.equals(lote.getUbicacionFisica().getId(), ubicacion.getId()))) {
                throw new CustomBusinessException(ApiErrorCode.CONTEO_DETALLE_INVALIDO,
                        "La ubicación del lote difiere de la del detalle");
            }
        }

        BigDecimal stockSistema;
        if (lote != null) {
            stockSistema = obtenerStockSnapshot(producto.getId().longValue(), lote, almacenId, ubicacion);
        } else {
            stockSistema = req.getStockSistema() != null
                    ? req.getStockSistema()
                    : obtenerStockSnapshot(producto.getId().longValue(), null, almacenId, ubicacion);
        }

        BigDecimal conteoFisico = req.getConteoFisico().setScale(2, RoundingMode.HALF_UP);
        BigDecimal diferencia = conteoFisico.subtract(stockSistema).setScale(2, RoundingMode.HALF_UP);

        return ConteoCiclicoDetalle.builder()
                .conteo(conteo)
                .producto(producto)
                .loteProducto(lote)
                .ubicacionFisica(ubicacion)
                .stockSistema(stockSistema)
                .conteoFisico(conteoFisico)
                .diferencia(diferencia)
                .build();
    }

    private void validarLoteEnConteo(LoteProducto lote, Producto producto, Integer almacenId) {
        if (lote.getProducto() == null || lote.getProducto().getId() == null
                || !Objects.equals(lote.getProducto().getId(), producto.getId())) {
            throw new CustomBusinessException(ApiErrorCode.CONTEO_DETALLE_INVALIDO,
                    "El lote no pertenece al producto indicado");
        }
        if (lote.getAlmacen() == null || lote.getAlmacen().getId() == null
                || !Objects.equals(lote.getAlmacen().getId(), almacenId)) {
            throw new CustomBusinessException(ApiErrorCode.UBICACION_NO_PERTENECE_ALMACEN,
                    "El lote no pertenece al almacén del conteo");
        }
    }

    private BigDecimal obtenerStockSnapshot(Long productoId, LoteProducto lote, Integer almacenId, UbicacionFisica ubicacion) {
        if (lote != null) {
            return Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal stock = loteProductoRepository.sumarStockPorProductoYAlmacen(productoId, almacenId,
                ubicacion != null ? ubicacion.getId() : null);
        return Optional.ofNullable(stock).orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal obtenerStockActual(ConteoCiclicoDetalle detalle, LoteProducto lote, Integer almacenId) {
        if (lote != null) {
            return Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }
        Long productoId = detalle.getProducto() != null && detalle.getProducto().getId() != null
                ? detalle.getProducto().getId().longValue()
                : null;
        Long ubicacionId = detalle.getUbicacionFisica() != null ? detalle.getUbicacionFisica().getId() : null;
        BigDecimal stock = loteProductoRepository.sumarStockPorProductoYAlmacen(productoId, almacenId, ubicacionId);
        return Optional.ofNullable(stock).orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private LoteProducto resolverLoteParaDetalle(ConteoCiclicoDetalle detalle, Integer almacenId) {
        if (detalle.getLoteProducto() != null) {
            LoteProducto lote = loteProductoRepository.findByIdForUpdate(detalle.getLoteProducto().getId())
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                            "Lote no encontrado"));
            validarLoteEnConteo(lote, detalle.getProducto(), almacenId);
            return lote;
        }

        Long productoId = detalle.getProducto() != null ? detalle.getProducto().getId().longValue() : null;
        if (productoId == null) {
            throw new CustomBusinessException(ApiErrorCode.CONTEO_DETALLE_INVALIDO, "Producto inválido en detalle");
        }

        List<LoteProducto> candidatos = loteProductoRepository
                .findByProductoIdAndAlmacenIdAndEstadoInOrderByFechaVencimientoAscIdAsc(
                        productoId, almacenId, LOTES_OPERABLES);

        Long ubicacionFiltro = detalle.getUbicacionFisica() != null ? detalle.getUbicacionFisica().getId() : null;
        return candidatos.stream()
                .filter(Objects::nonNull)
                .filter(lp -> ubicacionFiltro == null || (lp.getUbicacionFisica() != null
                        && Objects.equals(lp.getUbicacionFisica().getId(), ubicacionFiltro)))
                .findFirst()
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.CONTEO_DETALLE_INVALIDO,
                        "Debe especificar un lote para aplicar el conteo"));
    }

    private void registrarAjuste(ConteoCiclico conteo,
                                 ConteoCiclicoDetalle detalle,
                                 LoteProducto lote,
                                 BigDecimal diferencia,
                                 String idempotencyKey) {
        ClasificacionMovimientoInventario clasificacion = diferencia.compareTo(BigDecimal.ZERO) > 0
                ? ClasificacionMovimientoInventario.AJUSTE_POSITIVO
                : ClasificacionMovimientoInventario.AJUSTE_NEGATIVO;

        BigDecimal cantidad = diferencia.abs();
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                cantidad,
                TipoMovimiento.AJUSTE,
                clasificacion,
                "CONTEO-" + conteo.getId(),
                null,
                detalle.getProducto().getId(),
                lote.getId(),
                clasificacion == ClasificacionMovimientoInventario.AJUSTE_NEGATIVO ? conteo.getAlmacen().getId() : null,
                clasificacion == ClasificacionMovimientoInventario.AJUSTE_POSITIVO ? conteo.getAlmacen().getId() : null,
                null,
                null,
                resolverMotivo(clasificacion),
                resolverTipoDetalle(clasificacion),
                null,
                null,
                null,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                null,
                clasificacion == ClasificacionMovimientoInventario.AJUSTE_POSITIVO
                        && detalle.getUbicacionFisica() != null ? detalle.getUbicacionFisica().getId() : null
        );

        String movimientoKey = "conteo-" + conteo.getId() + "-detalle-" + detalle.getId();
        if (StringUtils.hasText(idempotencyKey)) {
            movimientoKey = movimientoKey + "-" + idempotencyKey;
        }
        movimientoInventarioService.registrarMovimiento(dto, movimientoKey);
    }

    private EstadoConteoCiclico parseEstado(String estado) {
        if (!StringUtils.hasText(estado)) {
            return null;
        }
        try {
            return EstadoConteoCiclico.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Estado de conteo inválido");
        }
    }

    private Long resolverMotivo(ClasificacionMovimientoInventario clasificacion) {
        return motivoMovimientoRepository.findByMotivo(clasificacion)
                .map(MotivoMovimiento::getId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.ERROR_INTERNO,
                        "No se encontró motivo de movimiento para la clasificación"));
    }

    private Long resolverTipoDetalle(ClasificacionMovimientoInventario clasificacion) {
        String descripcion = clasificacion.name();
        return tipoMovimientoDetalleRepository.findByDescripcion(descripcion)
                .map(TipoMovimientoDetalle::getId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.ERROR_INTERNO,
                        "No se encontró el tipo de detalle para ajustes"));
    }
}
