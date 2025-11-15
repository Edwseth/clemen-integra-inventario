package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.inventario.dto.LoteFefoDisponibleProjection;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadInsumoServiceTest {

    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private InventoryCatalogResolver catalogResolver;

    @InjectMocks
    private DisponibilidadInsumoService service;

    @Test
    @DisplayName("calcularDisponibilidad mantiene faltante y stock en preview y real")
    void calcularDisponibilidad_previewYRealIguales() {
        when(loteProductoRepository.findFefoDisponibles(10L, Integer.MAX_VALUE))
                .thenReturn(List.of(
                        lote(1L, "A", new BigDecimal("4.000000"), new BigDecimal("5.000000"), new BigDecimal("1.000000"), EstadoLote.DISPONIBLE),
                        lote(2L, "B", new BigDecimal("2.000000"), new BigDecimal("2.000000"), BigDecimal.ZERO, EstadoLote.LIBERADO)
                ));

        DistribucionFefoResult preview = service.calcularDisponibilidad(10L, new BigDecimal("5"), List.of(), true);
        DistribucionFefoResult real = service.calcularDisponibilidad(10L, new BigDecimal("5"), List.of(), false);

        assertThat(preview.getStockLibreTotal()).isEqualByComparingTo(new BigDecimal("6.000000"));
        assertThat(real.getStockLibreTotal()).isEqualByComparingTo(preview.getStockLibreTotal());
        assertThat(preview.getFaltante()).isEqualByComparingTo(BigDecimal.ZERO.setScale(6));
        assertThat(real.getFaltante()).isEqualByComparingTo(preview.getFaltante());
        assertThat(preview.isSuficiente()).isTrue();
        assertThat(real.isSuficiente()).isTrue();
        assertThat(preview.getDetalles()).hasSize(2);
    }

    @Test
    @DisplayName("resolverAlmacenesPreferidos respeta catálogo y faltante se calcula")
    void calcularDisponibilidad_insuficienteCalculaFaltante() {
        when(loteProductoRepository.findFefoDisponibles(20L, Integer.MAX_VALUE))
                .thenReturn(List.of(
                        lote(5L, "C", new BigDecimal("2.500000"), new BigDecimal("3.000000"), new BigDecimal("0.500000"), EstadoLote.DISPONIBLE)
                ));
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(99L);
        when(catalogResolver.getAlmacenOrigenMateriaPrimaId()).thenReturn(7L);

        Producto insumo = new Producto();
        insumo.setCategoriaProducto(new com.willyes.clemenintegra.inventario.model.CategoriaProducto());
        insumo.getCategoriaProducto().setTipo(TipoCategoria.MATERIA_PRIMA);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("LITRO");
        insumo.setUnidadMedida(unidad);

        List<Long> preferidos = service.resolverAlmacenesPreferidos(insumo);
        assertThat(preferidos).containsExactly(7L);

        DistribucionFefoResult resultado = service.calcularDisponibilidad(20L, new BigDecimal("5"), preferidos, true);
        assertThat(resultado.isSuficiente()).isFalse();
        assertThat(resultado.getFaltante()).isEqualByComparingTo(new BigDecimal("2.500000"));
    }

    private LoteFefoDisponibleProjection lote(Long id, String codigo, BigDecimal stockLibre,
                                               BigDecimal stockFisico, BigDecimal stockReservado,
                                               EstadoLote estado) {
        return new LoteFefoDisponibleProjection() {
            @Override
            public Long getLoteProductoId() {
                return id;
            }

            @Override
            public String getCodigoLote() {
                return codigo;
            }

            @Override
            public BigDecimal getStockLote() {
                return stockLibre;
            }

            @Override
            public BigDecimal getStockFisico() {
                return stockFisico;
            }

            @Override
            public BigDecimal getStockReservado() {
                return stockReservado;
            }

            @Override
            public java.time.LocalDateTime getFechaVencimiento() {
                return java.time.LocalDateTime.now().plusDays(10);
            }

            @Override
            public Long getAlmacenId() {
                return 5L;
            }

            @Override
            public String getNombreAlmacen() {
                return "ALM";
            }

            @Override
            public String getEstado() {
                return estado.name();
            }
        };
    }
}
