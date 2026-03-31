package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.BitacoraCambiosInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.valorizacion.AjusteValorizacionLoteRequestDTO;
import com.willyes.clemenintegra.inventario.dto.valorizacion.AjusteValorizacionLoteResponseDTO;
import com.willyes.clemenintegra.inventario.dto.valorizacion.ValorizacionElegibilidadResponseDTO;
import com.willyes.clemenintegra.inventario.model.AjusteValorizacionLote;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.repository.AjusteValorizacionLoteRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AjusteValorizacionLoteServiceImpl implements AjusteValorizacionLoteService {

    private static final String RULE_STOCK_DISPONIBLE = "STOCK_DISPONIBLE_MAYOR_A_CERO";
    private static final String RULE_COSTO_BASE = "LOTE_COSTO_BASE_EN_CERO_O_NULO";
    private static final int SCALE = 6;

    private final LoteProductoRepository loteProductoRepository;
    private final AjusteValorizacionLoteRepository ajusteRepository;
    private final UsuarioService usuarioService;
    private final BitacoraCambiosInventarioService bitacoraService;

    @Override
    @Transactional(readOnly = true)
    public ValorizacionElegibilidadResponseDTO evaluarElegibilidad(Long loteId) {
        LoteProducto lote = loteProductoRepository.findById(loteId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.LOTE_NO_ENCONTRADO, "LOTE_NO_ENCONTRADO"));

        BigDecimal stockDisponible = stockDisponible(lote);
        BigDecimal costoUnitarioActual = safeScale(lote.getCostoUnitarioMaterial());
        BigDecimal costoTotalActual = safeScale(lote.getCostoTotalMaterialIngresado());
        BigDecimal totalIngresadoActual = safeScale(lote.getTotalIngresadoMaterial());

        List<ValorizacionElegibilidadResponseDTO.ReglaEvaluada> reglas = new ArrayList<>();
        reglas.add(new ValorizacionElegibilidadResponseDTO.ReglaEvaluada(
                RULE_STOCK_DISPONIBLE,
                stockDisponible.compareTo(BigDecimal.ZERO) > 0,
                "BLOCKING",
                "El lote debe tener stock disponible mayor a cero"
        ));
        reglas.add(new ValorizacionElegibilidadResponseDTO.ReglaEvaluada(
                RULE_COSTO_BASE,
                costoUnitarioActual.compareTo(BigDecimal.ZERO) <= 0,
                "WARNING",
                "Si el costo actual es positivo se requerirá override"
        ));

        List<String> warnings = new ArrayList<>();
        if (totalIngresadoActual.compareTo(BigDecimal.ZERO) <= 0 && safeScale(lote.getStockLote()).compareTo(BigDecimal.ZERO) > 0) {
            warnings.add("TOTAL_INGRESADO_INVALIDO_USARA_STOCK_LOTE");
        }

        boolean eligible = reglas.stream()
                .filter(r -> "BLOCKING".equals(r.severity()))
                .allMatch(ValorizacionElegibilidadResponseDTO.ReglaEvaluada::passed)
                && costoUnitarioActual.compareTo(BigDecimal.ZERO) <= 0;

        ValorizacionElegibilidadResponseDTO.Snapshot snapshot = new ValorizacionElegibilidadResponseDTO.Snapshot(
                lote.getId(),
                lote.getCodigoLote(),
                lote.getProducto() != null ? lote.getProducto().getId().longValue() : null,
                lote.getProducto() != null ? lote.getProducto().getNombre() : null,
                lote.getAlmacen() != null ? lote.getAlmacen().getId() : null,
                lote.getAlmacen() != null ? lote.getAlmacen().getNombre() : null,
                lote.getEstado() != null ? lote.getEstado().name() : null,
                stockDisponible,
                costoUnitarioActual,
                costoTotalActual,
                totalIngresadoActual
        );

        return new ValorizacionElegibilidadResponseDTO(eligible, snapshot, reglas, warnings);
    }

    @Override
    @Transactional
    public AjusteValorizacionLoteResponseDTO ajustar(Long loteId,
                                                     AjusteValorizacionLoteRequestDTO request,
                                                     String idempotencyKey) {
        validarRequest(request, idempotencyKey);

        String payloadFingerprint = calcularFingerprint(loteId, request);
        AjusteValorizacionLote existente = ajusteRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existente != null) {
            if (!Objects.equals(existente.getPayloadFingerprint(), payloadFingerprint)) {
                throw new CustomBusinessException(
                        ApiErrorCode.IDEMPOTENCY_KEY_REUTILIZADA_CON_PAYLOAD_DISTINTO,
                        "IDEMPOTENCY_KEY_REUTILIZADA_CON_PAYLOAD_DISTINTO"
                );
            }
            return toResponse(existente);
        }

        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        LoteProducto lote = loteProductoRepository.findByIdForUpdate(loteId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.LOTE_NO_ENCONTRADO, "LOTE_NO_ENCONTRADO"));

        BigDecimal disponible = stockDisponible(lote);
        if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomBusinessException(ApiErrorCode.LOTE_SIN_STOCK_DISPONIBLE, "LOTE_SIN_STOCK_DISPONIBLE");
        }

        BigDecimal costoUnitarioAnterior = safeScale(lote.getCostoUnitarioMaterial());
        boolean costoPositivo = costoUnitarioAnterior.compareTo(BigDecimal.ZERO) > 0;
        boolean forzar = Boolean.TRUE.equals(request.forzarSobreCostoPositivo());
        if (costoPositivo && !forzar) {
            throw new CustomBusinessException(ApiErrorCode.LOTE_COSTO_YA_POSITIVO, "LOTE_COSTO_YA_POSITIVO");
        }
        if (costoPositivo && forzar && !tieneAutoridad("INV_COSTEO_AJUSTE_OVERRIDE")) {
            throw new CustomBusinessException(ApiErrorCode.PERMISO_INSUFICIENTE, "PERMISO_INSUFICIENTE");
        }

        BigDecimal totalIngresadoAnterior = safeScale(lote.getTotalIngresadoMaterial());
        BigDecimal totalIngresadoNuevo = totalIngresadoAnterior.compareTo(BigDecimal.ZERO) > 0
                ? totalIngresadoAnterior
                : safeScale(lote.getStockLote());

        if (totalIngresadoNuevo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomBusinessException(ApiErrorCode.AJUSTE_VALORIZACION_NO_PERMITIDO,
                    "AJUSTE_VALORIZACION_NO_PERMITIDO",
                    Map.of("motivo", "TOTAL_INGRESADO_NO_VALIDO"));
        }

        BigDecimal costoUnitarioNuevo = request.costoUnitarioNuevo().setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal costoTotalAnterior = safeScale(lote.getCostoTotalMaterialIngresado());
        BigDecimal costoTotalNuevo = costoUnitarioNuevo.multiply(totalIngresadoNuevo).setScale(SCALE, RoundingMode.HALF_UP);

        lote.setCostoUnitarioMaterial(costoUnitarioNuevo);
        lote.setTotalIngresadoMaterial(totalIngresadoNuevo);
        lote.setCostoTotalMaterialIngresado(costoTotalNuevo);
        loteProductoRepository.save(lote);

        AjusteValorizacionLote ajuste = ajusteRepository.save(AjusteValorizacionLote.builder()
                .lote(lote)
                .codigoLote(lote.getCodigoLote())
                .producto(lote.getProducto())
                .almacen(lote.getAlmacen())
                .costoUnitarioAnterior(costoUnitarioAnterior)
                .costoUnitarioNuevo(costoUnitarioNuevo)
                .costoTotalAnterior(costoTotalAnterior)
                .costoTotalNuevo(costoTotalNuevo)
                .totalIngresadoAnterior(totalIngresadoAnterior)
                .totalIngresadoNuevo(totalIngresadoNuevo)
                .motivo(normalizar(request.motivo()))
                .observacion(normalizar(request.observacion()))
                .documentoSoporte(normalizar(request.documentoSoporte()))
                .idempotencyKey(idempotencyKey)
                .payloadFingerprint(payloadFingerprint)
                .usuario(usuario)
                .fechaAjuste(LocalDateTime.now())
                .build());

        registrarBitacora(lote, usuario, ajuste, costoUnitarioAnterior, costoUnitarioNuevo,
                costoTotalAnterior, costoTotalNuevo, totalIngresadoAnterior, totalIngresadoNuevo);

        return toResponse(ajuste);
    }

    private void validarRequest(AjusteValorizacionLoteRequestDTO request, String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new CustomBusinessException(ApiErrorCode.IDEMPOTENCY_KEY_REQUERIDA, "IDEMPOTENCY_KEY_REQUERIDA");
        }
        if (!StringUtils.hasText(request.documentoSoporte())) {
            throw new CustomBusinessException(ApiErrorCode.DOCUMENTO_SOPORTE_REQUERIDO, "DOCUMENTO_SOPORTE_REQUERIDO");
        }
        if (request.costoUnitarioNuevo() == null || request.costoUnitarioNuevo().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomBusinessException(ApiErrorCode.AJUSTE_VALORIZACION_NO_PERMITIDO,
                    "AJUSTE_VALORIZACION_NO_PERMITIDO");
        }
    }

    private void registrarBitacora(LoteProducto lote,
                                   Usuario usuario,
                                   AjusteValorizacionLote ajuste,
                                   BigDecimal costoUnitarioAnterior,
                                   BigDecimal costoUnitarioNuevo,
                                   BigDecimal costoTotalAnterior,
                                   BigDecimal costoTotalNuevo,
                                   BigDecimal totalIngresadoAnterior,
                                   BigDecimal totalIngresadoNuevo) {
        String valorAnt = "cu=" + costoUnitarioAnterior + ",ct=" + costoTotalAnterior + ",ti=" + totalIngresadoAnterior;
        String valorNuevo = "cu=" + costoUnitarioNuevo + ",ct=" + costoTotalNuevo + ",ti=" + totalIngresadoNuevo;
        bitacoraService.crear(BitacoraCambiosInventarioDTO.builder()
                .tablaAfectada("lotes_productos")
                .registroId(lote.getId())
                .campoModificado("valorizacion_lote")
                .valorAnt(valorAnt)
                .valorNuevo(valorNuevo)
                .accion("AJUSTE_VALORIZACION_LOTE")
                .observacion("ajusteId=" + ajuste.getId() + " doc=" + ajuste.getDocumentoSoporte())
                .fechaCambio(LocalDateTime.now())
                .usuarioId(usuario.getId())
                .usuarioNombre(usuario.getNombreUsuario())
                .build());
    }

    private AjusteValorizacionLoteResponseDTO toResponse(AjusteValorizacionLote ajuste) {
        return new AjusteValorizacionLoteResponseDTO(
                ajuste.getId(),
                ajuste.getLote().getId(),
                ajuste.getCodigoLote(),
                ajuste.getProducto().getId().longValue(),
                ajuste.getCostoUnitarioAnterior(),
                ajuste.getCostoUnitarioNuevo(),
                ajuste.getCostoTotalAnterior(),
                ajuste.getCostoTotalNuevo(),
                ajuste.getTotalIngresadoAnterior(),
                ajuste.getTotalIngresadoNuevo(),
                ajuste.getFechaAjuste(),
                ajuste.getUsuario().getId(),
                ajuste.getIdempotencyKey()
        );
    }

    private BigDecimal stockDisponible(LoteProducto lote) {
        return safeScale(lote.getStockLote()).subtract(safeScale(lote.getStockReservado())).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal safeScale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP)
                : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private String normalizar(String text) {
        return text == null ? "" : text.trim();
    }

    private boolean tieneAutoridad(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities() != null
                && auth.getAuthorities().stream()
                .anyMatch(a -> authority.equalsIgnoreCase(a.getAuthority()));
    }

    String calcularFingerprint(Long loteId, AjusteValorizacionLoteRequestDTO req) {
        String payload = loteId + "|"
                + safeScale(req.costoUnitarioNuevo()).toPlainString() + "|"
                + normalizar(req.motivo()).toUpperCase(Locale.ROOT) + "|"
                + normalizar(req.observacion()) + "|"
                + normalizar(req.documentoSoporte()).toUpperCase(Locale.ROOT) + "|"
                + Boolean.TRUE.equals(req.forzarSobreCostoPositivo());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new CustomBusinessException(ApiErrorCode.ERROR_INTERNO, "No fue posible calcular fingerprint", ex.getMessage());
        }
    }
}
