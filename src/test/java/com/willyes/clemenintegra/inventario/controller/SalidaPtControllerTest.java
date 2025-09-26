package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.LotePtDisponibleDTO;
import com.willyes.clemenintegra.inventario.dto.SalidaPtConfigResponse;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalidaPtControllerTest {

    @Mock
    private InventoryCatalogResolver catalogResolver;
    @Mock
    private LoteProductoRepository loteProductoRepository;

    @InjectMocks
    private SalidaPtController controller;

    @Test
    void obtenerConfigSalidaPt_devuelveValoresDeResolver() {
        when(catalogResolver.isSalidaPtEnabled()).thenReturn(true);
        when(catalogResolver.getAlmacenPtId()).thenReturn(2L);
        when(catalogResolver.getTipoDetalleSalidaPtId()).thenReturn(11L);

        ResponseEntity<SalidaPtConfigResponse> response = controller.obtenerConfigSalidaPt();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().enabled()).isTrue();
        assertThat(response.getBody().almacenPtId()).isEqualTo(2L);
        assertThat(response.getBody().tipoDetalleSalidaPtId()).isEqualTo(11L);
    }

    @Test
    void listarLotesDisponiblesPt_filtraPorCantidadYEstado() {
        when(catalogResolver.isSalidaPtEnabled()).thenReturn(true);
        when(catalogResolver.getAlmacenPtId()).thenReturn(5L);

        Producto producto = new Producto();
        producto.setId(99);

        Almacen almacenPt = new Almacen();
        almacenPt.setId(5);

        LoteProducto apto = new LoteProducto();
        apto.setId(10L);
        apto.setCodigoLote("PT-001");
        apto.setProducto(producto);
        apto.setAlmacen(almacenPt);
        apto.setEstado(EstadoLote.DISPONIBLE);
        apto.setStockLote(new BigDecimal("8.000"));
        apto.setStockReservado(new BigDecimal("1.000000"));
        apto.setFechaVencimiento(LocalDateTime.of(2025, 1, 1, 0, 0));

        LoteProducto insuficiente = new LoteProducto();
        insuficiente.setId(11L);
        insuficiente.setCodigoLote("PT-002");
        insuficiente.setProducto(producto);
        insuficiente.setAlmacen(almacenPt);
        insuficiente.setEstado(EstadoLote.LIBERADO);
        insuficiente.setStockLote(new BigDecimal("1.000"));
        insuficiente.setStockReservado(BigDecimal.ZERO.setScale(6));

        when(loteProductoRepository.findFefoSalidaPt(eq(99L), eq(5L), any()))
                .thenReturn(List.of(apto, insuficiente));

        ResponseEntity<List<LotePtDisponibleDTO>> response = controller.listarLotesDisponiblesPt(99L, new BigDecimal("5"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        LotePtDisponibleDTO dto = response.getBody().get(0);
        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.codigoLote()).isEqualTo("PT-001");
        assertThat(dto.stockDisponible()).isEqualByComparingTo(new BigDecimal("7.000"));
        assertThat(dto.fechaVencimiento()).isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0));
    }
}
