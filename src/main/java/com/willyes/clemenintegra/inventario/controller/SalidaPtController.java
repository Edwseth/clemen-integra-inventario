package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.LotePtDisponibleDTO;
import com.willyes.clemenintegra.inventario.dto.SalidaPtConfigResponse;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventario")
public class SalidaPtController {

    private final InventoryCatalogResolver catalogResolver;
    private final LoteProductoRepository loteProductoRepository;

    public SalidaPtController(InventoryCatalogResolver catalogResolver,
                              LoteProductoRepository loteProductoRepository) {
        this.catalogResolver = catalogResolver;
        this.loteProductoRepository = loteProductoRepository;
    }

    @GetMapping("/salida-pt/config")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_SUPER_ADMIN')")
    public ResponseEntity<SalidaPtConfigResponse> obtenerConfigSalidaPt() {
        return ResponseEntity.ok(new SalidaPtConfigResponse(
                catalogResolver.isSalidaPtEnabled(),
                catalogResolver.getAlmacenPtId(),
                catalogResolver.getTipoDetalleSalidaPtId()
        ));
    }

    @GetMapping("/lotes/pt-disponibles")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_SUPER_ADMIN')")
    public ResponseEntity<List<LotePtDisponibleDTO>> listarLotesDisponiblesPt(
            @RequestParam("productoId") Long productoId,
            @RequestParam(value = "minCantidad", required = false) BigDecimal minCantidad
    ) {
        if (productoId == null || productoId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PRODUCTO_ID_REQUERIDO");
        }
        Long almacenPtId = catalogResolver.getAlmacenPtId();
        if (catalogResolver.isSalidaPtEnabled() && (almacenPtId == null || almacenPtId <= 0)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CONFIG_FALTANTE");
        }
        if (almacenPtId == null || almacenPtId <= 0) {
            return ResponseEntity.ok(List.of());
        }

        EnumSet<EstadoLote> estadosElegibles = EnumSet.of(EstadoLote.DISPONIBLE, EstadoLote.LIBERADO);
        List<LoteProducto> candidatos = loteProductoRepository.findFefoSalidaPt(
                productoId,
                almacenPtId,
                estadosElegibles
        );

        BigDecimal umbral = minCantidad != null ? minCantidad : BigDecimal.ZERO;
        List<LotePtDisponibleDTO> respuesta = candidatos.stream()
                .map(lote -> {
                    BigDecimal stock = lote.getStockLote() != null ? lote.getStockLote() : BigDecimal.ZERO;
                    BigDecimal reservado = lote.getStockReservado() != null ? lote.getStockReservado() : BigDecimal.ZERO;
                    BigDecimal disponible = stock.subtract(reservado);
                    if (disponible.compareTo(BigDecimal.ZERO) < 0) {
                        disponible = BigDecimal.ZERO;
                    }
                    return new LotePtDisponibleDTO(
                            lote.getId(),
                            lote.getCodigoLote(),
                            disponible,
                            lote.getFechaVencimiento()
                    );
                })
                .filter(dto -> dto.stockDisponible() != null && dto.stockDisponible().compareTo(umbral) >= 0)
                .collect(Collectors.toList());

        return ResponseEntity.ok(respuesta);
    }
}
