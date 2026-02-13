package com.willyes.clemenintegra.inventario.regularizacion.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.regularizacion.dto.AjusteLoteDTO;
import com.willyes.clemenintegra.inventario.regularizacion.dto.MovimientoCreadoDTO;
import com.willyes.clemenintegra.inventario.regularizacion.dto.RegularizacionTrazabilidadRequestDTO;
import com.willyes.clemenintegra.inventario.regularizacion.dto.RegularizacionTrazabilidadResponseDTO;
import com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidadOperacion;
import com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadOperacionRepository;
import com.willyes.clemenintegra.inventario.regularizacion.service.RegularizacionTrazabilidadService;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RegularizacionTrazabilidadServiceImpl implements RegularizacionTrazabilidadService {

    private static final String ESTADO_CREADA = "CREADA";
    private static final String ESTADO_APLICADA = "APLICADA";
    private static final String TIPO_OPERACION = "REGULARIZACION_TRAZABILIDAD_OP";

    private final OrdenProduccionRepository ordenProduccionRepository;
    private final ProductoRepository productoRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final MovimientoInventarioService movimientoInventarioService;
    private final RegularizacionTrazabilidadOperacionRepository operacionRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.inventario.regularizacion.allow-negative-stock:false}")
    private boolean allowNegativeStock;

    @Value("${app.inventario.regularizacion.adjunto-obligatorio:false}")
    private boolean adjuntoObligatorio;

    @Value("${app.inventario.regularizacion.adjunto-obligatorio-desde-cantidad:1000}")
    private BigDecimal umbralAdjunto;

    @Override
    @Transactional
    public RegularizacionTrazabilidadResponseDTO regularizarPorOP(RegularizacionTrazabilidadRequestDTO request,
                                                                  String idempotencyKey,
                                                                  Usuario usuarioAuth) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Idempotency-Key es obligatorio");
        }
        if (usuarioAuth == null) {
            throw new CustomBusinessException(ApiErrorCode.SESION_INVALIDA, "Usuario autenticado requerido");
        }

        validarRequest(request, usuarioAuth);
        String hash = calcularHashRequest(request);

        var existenteOpt = operacionRepository.findByIdempotencyKey(idempotencyKey);
        if (existenteOpt.isPresent()) {
            RegularizacionTrazabilidadOperacion existente = existenteOpt.get();
            if (!Objects.equals(existente.getRequestHash(), hash)) {
                throw new CustomBusinessException(ApiErrorCode.IDEMPOTENCY_KEY_REUSADA_CON_OTRO_PAYLOAD,
                        "La Idempotency-Key ya fue usada con otro payload");
            }
            if (ESTADO_APLICADA.equals(existente.getEstado()) && StringUtils.hasText(existente.getResultadoJson())) {
                return deserializeResultado(existente.getResultadoJson());
            }
            throw new CustomBusinessException(ApiErrorCode.OPERACION_EN_PROCESO,
                    "Existe una operación en proceso para la misma Idempotency-Key");
        }

        RegularizacionTrazabilidadOperacion operacion = operacionRepository.save(RegularizacionTrazabilidadOperacion.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(hash)
                .estado(ESTADO_CREADA)
                .ordenProduccionId(request.ordenProduccionId())
                .productoId(request.productoId())
                .creadoPor(usuarioAuth)
                .createdAt(LocalDateTime.now())
                .build());

        List<MovimientoCreadoDTO> movimientos = new ArrayList<>();
        int index = 0;
        for (AjusteLoteDTO ajuste : request.ajustes()) {
            index++;
            boolean positivo = "POSITIVO".equalsIgnoreCase(ajuste.tipo());
            var clasificacion = positivo
                    ? ClasificacionMovimientoInventario.AJUSTE_POSITIVO
                    : ClasificacionMovimientoInventario.AJUSTE_NEGATIVO;

            MovimientoInventarioDTO movimientoDTO = new MovimientoInventarioDTO(
                    null,
                    ajuste.cantidad(),
                    TipoMovimiento.AJUSTE,
                    clasificacion,
                    request.docReferencia(),
                    buildObservaciones(request),
                    null,
                    null,
                    null,
                    Math.toIntExact(request.productoId()),
                    ajuste.loteProductoId(),
                    null,
                    null,
                    null,
                    null,
                    request.motivoMovimientoId(),
                    request.tipoMovimientoDetalleId(),
                    null,
                    null,
                    request.ordenProduccionId(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            MovimientoInventarioResponseDTO response = movimientoInventarioService.registrarMovimiento(
                    movimientoDTO,
                    idempotencyKey + ":" + index
            );
            movimientos.add(MovimientoCreadoDTO.builder()
                    .movimientoId(response.getId())
                    .tipoMovimiento(TipoMovimiento.AJUSTE.name())
                    .clasificacion(clasificacion.name())
                    .loteProductoId(ajuste.loteProductoId())
                    .cantidad(ajuste.cantidad())
                    .build());
        }

        RegularizacionTrazabilidadResponseDTO resultado = RegularizacionTrazabilidadResponseDTO.builder()
                .operacionId(operacion.getId())
                .idempotencyKey(idempotencyKey)
                .ordenProduccionId(request.ordenProduccionId())
                .productoId(request.productoId())
                .tipoOperacion(TIPO_OPERACION)
                .movimientos(movimientos)
                .registradoPorId(usuarioAuth.getId())
                .fecha(LocalDateTime.now())
                .build();

        operacion.setEstado(ESTADO_APLICADA);
        operacion.setResultadoJson(toJson(resultado));
        operacionRepository.save(operacion);

        return resultado;
    }

    private void validarRequest(RegularizacionTrazabilidadRequestDTO request, Usuario usuarioAuth) {
        if (request == null || request.ajustes() == null || request.ajustes().isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Debe enviar ajustes");
        }
        if (!ordenProduccionRepository.existsById(request.ordenProduccionId())) {
            throw new CustomBusinessException(ApiErrorCode.ORDEN_PRODUCCION_NO_ENCONTRADA, "Orden de producción no encontrada");
        }
        if (!productoRepository.existsById(request.productoId())) {
            throw new CustomBusinessException(ApiErrorCode.PRODUCTO_NO_ENCONTRADO, "Producto no encontrado");
        }
        if (request.motivoMovimientoId() == null || request.tipoMovimientoDetalleId() == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "motivoMovimientoId y tipoMovimientoDetalleId son requeridos");
        }
        if (!StringUtils.hasText(request.observaciones()) || request.observaciones().trim().length() < 15
                || request.observaciones().trim().length() > 500) {
            throw new CustomBusinessException(ApiErrorCode.OBSERVACION_REQUERIDA,
                    "observaciones debe tener entre 15 y 500 caracteres");
        }

        BigDecimal sumaNegativos = BigDecimal.ZERO;
        for (AjusteLoteDTO ajuste : request.ajustes()) {
            if (ajuste.loteProductoId() == null || ajuste.cantidad() == null || ajuste.cantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Ajustes inválidos");
            }
            String tipo = ajuste.tipo() == null ? "" : ajuste.tipo().trim().toUpperCase(Locale.ROOT);
            if (!"POSITIVO".equals(tipo) && !"NEGATIVO".equals(tipo)) {
                throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Tipo de ajuste inválido");
            }
            LoteProducto lote = loteProductoRepository.findById(ajuste.loteProductoId())
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.LOTE_NO_ENCONTRADO, "Lote no encontrado"));
            if (lote.getProducto() == null || lote.getProducto().getId() == null
                    || lote.getProducto().getId().longValue() != request.productoId()) {
                throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                        "El lote no pertenece al producto indicado");
            }
            if ("NEGATIVO".equals(tipo)) {
                sumaNegativos = sumaNegativos.add(ajuste.cantidad());
            }
        }

        if (adjuntoObligatorio && sumaNegativos.compareTo(umbralAdjunto) >= 0 && !StringUtils.hasText(request.soporteId())) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "soporteId es obligatorio para regularizaciones de este monto");
        }

        if (Boolean.TRUE.equals(request.permitirStockNegativo())) {
            boolean esSuperAdmin = usuarioAuth.getRol() == RolUsuario.ROL_SUPER_ADMIN;
            if (!allowNegativeStock || !esSuperAdmin) {
                throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                        "No está permitido generar stock negativo");
            }
        }
    }

    private String buildObservaciones(RegularizacionTrazabilidadRequestDTO request) {
        if (StringUtils.hasText(request.soporteId())) {
            return request.observaciones().trim() + " | soporteId=" + request.soporteId().trim();
        }
        return request.observaciones().trim();
    }

    String calcularHashRequest(RegularizacionTrazabilidadRequestDTO request) {
        List<String> ajustesNormalizados = request.ajustes().stream()
                .sorted(Comparator.comparing(AjusteLoteDTO::loteProductoId)
                        .thenComparing(a -> a.tipo().toUpperCase(Locale.ROOT))
                        .thenComparing(AjusteLoteDTO::cantidad))
                .map(a -> a.loteProductoId() + "|" + a.tipo().trim().toUpperCase(Locale.ROOT) + "|" + a.cantidad().stripTrailingZeros().toPlainString())
                .toList();

        String normalized = String.join(";",
                String.valueOf(request.ordenProduccionId()),
                String.valueOf(request.productoId()),
                String.valueOf(request.motivoMovimientoId()),
                String.valueOf(request.tipoMovimientoDetalleId()),
                nullSafe(request.docReferencia()),
                nullSafe(request.observaciones()),
                nullSafe(request.soporteId()),
                String.valueOf(Boolean.TRUE.equals(request.permitirStockNegativo())),
                String.join(",", ajustesNormalizados)
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new CustomBusinessException(ApiErrorCode.ERROR_INTERNO, "No fue posible calcular hash de idempotencia");
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }

    private String toJson(RegularizacionTrazabilidadResponseDTO response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new CustomBusinessException(ApiErrorCode.ERROR_INTERNO, "No fue posible serializar resultado");
        }
    }

    private RegularizacionTrazabilidadResponseDTO deserializeResultado(String resultadoJson) {
        try {
            return objectMapper.readValue(resultadoJson, RegularizacionTrazabilidadResponseDTO.class);
        } catch (JsonProcessingException e) {
            throw new CustomBusinessException(ApiErrorCode.ERROR_INTERNO, "No fue posible deserializar resultado idempotente");
        }
    }
}
