package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.KardexFiltro;
import com.willyes.clemenintegra.inventario.dto.KardexItemDTO;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KardexServiceImpl implements KardexService {

    private final ProductoRepository productoRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final MovimientoSignosResolver movimientoSignosResolver;

    @Override
    public List<KardexItemDTO> obtenerKardex(KardexFiltro filtro) {
        if ((filtro.getProductoId() == null) && !StringUtils.hasText(filtro.getCodigoSku())) {
            throw new IllegalArgumentException("Se requiere productoId o codigoSku");
        }

        Producto producto = resolverProducto(filtro.getProductoId(), filtro.getCodigoSku());
        Long productoId = producto.getId() != null ? producto.getId().longValue() : null;
        LoteProducto lote = resolverLote(
                filtro.getLoteId(),
                filtro.getCodigoLote(),
                productoId,
                filtro.getAlmacenId(),
                filtro.getOrdenProduccionId());

        List<MovimientoInventario> movimientos = movimientoInventarioRepository.buscarParaKardex(
                filtro.getFechaDesde(),
                filtro.getFechaHasta(),
                productoId,
                lote != null ? lote.getId() : null,
                filtro.getAlmacenId(),
                filtro.getOrdenProduccionId(),
                filtro.getEtapaProduccionId()
        );

        return calcularSaldo(movimientos, producto, lote, filtro.getAlmacenId());
    }

    private Producto resolverProducto(Long productoId, String codigoSku) {
        if (productoId != null) {
            return productoRepository.findById(productoId)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        }
        return productoRepository.findByCodigoSku(codigoSku)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
    }

    private LoteProducto resolverLote(Long loteId,
                                      String codigoLote,
                                      Long productoId,
                                      Long almacenId,
                                      Long ordenProduccionId) {
        if (loteId != null) {
            Optional<LoteProducto> lote = loteProductoRepository.findById(loteId);
            return lote.filter(lp -> Objects.equals(lp.getProducto() != null ? lp.getProducto().getId().longValue() : null, productoId))
                    .orElseThrow(() -> new IllegalArgumentException("Lote no pertenece al producto"));
        }
        if (StringUtils.hasText(codigoLote)) {
            List<LoteProducto> lotes = loteProductoRepository.findAllByCodigoLoteAndProductoId(codigoLote, productoId);
            if (lotes.isEmpty()) {
                log.warn("Kardex: lote no encontrado para codigoLote={}, productoId={}, almacenId={}, ordenProduccionId={}",
                        codigoLote, productoId, almacenId, ordenProduccionId);
                throw new CustomBusinessException(ApiErrorCode.KARDEX_PARAM_INVALIDO,
                        "Lote no encontrado para el producto",
                        detallesLote(codigoLote, productoId, almacenId, ordenProduccionId, lotes));
            }

            List<LoteProducto> candidatos = lotes;
            if (almacenId != null) {
                candidatos = candidatos.stream()
                        .filter(lp -> lp.getAlmacen() != null && Objects.equals(lp.getAlmacen().getId().longValue(), almacenId))
                        .toList();
            }
            if (ordenProduccionId != null) {
                candidatos = candidatos.stream()
                        .filter(lp -> lp.getOrdenProduccion() != null && Objects.equals(lp.getOrdenProduccion().getId(), ordenProduccionId))
                        .toList();
            }

            if (candidatos.size() == 1) {
                return candidatos.get(0);
            }

            if (candidatos.isEmpty()) {
                log.warn("Kardex: lote no encontrado tras aplicar filtros para codigoLote={}, productoId={}, almacenId={}, ordenProduccionId={}",
                        codigoLote, productoId, almacenId, ordenProduccionId);
                throw new CustomBusinessException(ApiErrorCode.KARDEX_PARAM_INVALIDO,
                        "Lote no encontrado con los filtros proporcionados; envíe loteId para precisión",
                        detallesLote(codigoLote, productoId, almacenId, ordenProduccionId, lotes));
            }

            log.warn("Kardex: múltiples lotes con código {}, productoId {}, sin criterios suficientes para desambiguar (almacenId={}, ordenProduccionId={})",
                    codigoLote, productoId, almacenId, ordenProduccionId);
            throw new CustomBusinessException(ApiErrorCode.KARDEX_PARAM_INVALIDO,
                    "Existen múltiples lotes con el mismo código; envíe loteId, almacenId u ordenProduccionId para desambiguar",
                    detallesLote(codigoLote, productoId, almacenId, ordenProduccionId, lotes));
        }
        return null;
    }

    private Object detallesLote(String codigoLote,
                                 Long productoId,
                                 Long almacenId,
                                 Long ordenProduccionId,
                                 List<LoteProducto> lotes) {
        java.util.Map<String, Object> detalles = new java.util.LinkedHashMap<>();
        detalles.put("codigoLote", codigoLote);
        detalles.put("productoId", productoId);
        detalles.put("almacenId", almacenId);
        detalles.put("ordenProduccionId", ordenProduccionId);
        detalles.put("loteIds", lotes != null ? lotes.stream()
                .filter(Objects::nonNull)
                .map(LoteProducto::getId)
                .toList() : List.of());
        return detalles;
    }

    private List<KardexItemDTO> calcularSaldo(List<MovimientoInventario> movimientos,
                                              Producto producto,
                                              LoteProducto lote,
                                              Long almacenId) {
        BigDecimal saldo = BigDecimal.ZERO;
        List<MovimientoConTotales> items = movimientos.stream()
                .map(mov -> new MovimientoConTotales(mov, movimientoSignosResolver.calcularEntrada(mov, almacenId), movimientoSignosResolver.calcularSalida(mov, almacenId)))
                .collect(Collectors.toList());

        List<KardexItemDTO> resultado = new java.util.ArrayList<>(items.size());
        for (MovimientoConTotales item : items) {
            saldo = saldo.add(item.entrada()).subtract(item.salida());
            MovimientoInventario mov = item.movimiento();
            resultado.add(KardexItemDTO.builder()
                    .fechaMovimiento(mov.getFechaIngreso())
                    .tipoMovimiento(mov.getTipoMovimiento() != null ? mov.getTipoMovimiento().name() : null)
                    .clasificacion(mov.getClasificacion() != null ? mov.getClasificacion().name() : null)
                    .referencia(mov.getDocReferencia())
                    .almacenOrigen(mov.getAlmacenOrigen() != null ? mov.getAlmacenOrigen().getNombre() : null)
                    .almacenDestino(mov.getAlmacenDestino() != null ? mov.getAlmacenDestino().getNombre() : null)
                    .codigoLote(mov.getLote() != null ? mov.getLote().getCodigoLote() : (lote != null ? lote.getCodigoLote() : null))
                    .codigoSku(producto.getCodigoSku())
                    .nombreProducto(producto.getNombre())
                    .cantidadEntrada(item.entrada())
                    .cantidadSalida(item.salida())
                    .saldo(saldo)
                    .usuario(mov.getRegistradoPor() != null ? mov.getRegistradoPor().getNombreCompleto() : null)
                    .build());
        }
        return resultado;
    }

    private record MovimientoConTotales(MovimientoInventario movimiento, BigDecimal entrada, BigDecimal salida) {}
}
