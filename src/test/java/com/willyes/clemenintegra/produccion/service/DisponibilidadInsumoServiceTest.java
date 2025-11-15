package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.inventario.dto.LoteFefoDisponibleProjection;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadInsumoServiceTest {

    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private InventoryCatalogResolver catalogResolver;
    @Mock
    private ProductoRepository productoRepository;

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

    @Test
    @DisplayName("calcularDisponibilidad detecta faltante por reservas aun con stock físico suficiente")
    void calcularDisponibilidad_detectaFaltantePorReservas() {
        when(loteProductoRepository.findFefoDisponibles(30L, Integer.MAX_VALUE))
                .thenReturn(List.of(
                        lote(10L, "L-170925-3", new BigDecimal("125.000000"), new BigDecimal("125.000000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE),
                        lote(11L, "L-271025-02", new BigDecimal("130.000000"), new BigDecimal("500.000000"), new BigDecimal("370.000000"), EstadoLote.DISPONIBLE),
                        lote(12L, "L-060825-3", new BigDecimal("47.500000"), new BigDecimal("150.000000"), new BigDecimal("102.500000"), EstadoLote.DISPONIBLE)
                ));
        when(productoRepository.findById(30L)).thenReturn(Optional.empty());

        DistribucionFefoResult resultado = service.calcularDisponibilidad(30L, new BigDecimal("350"), List.of(5L), true);

        assertThat(resultado.isSuficiente()).isFalse();
        assertThat(resultado.getStockFisicoTotal()).isEqualByComparingTo(new BigDecimal("775.000000"));
        assertThat(resultado.getStockLibreTotal()).isEqualByComparingTo(new BigDecimal("302.500000"));
        assertThat(resultado.getFaltante()).isEqualByComparingTo(new BigDecimal("47.500000"));
    }

    @Test
    @DisplayName("calcularDisponibilidad mantiene stock suficiente para Jarabe Base excluyendo cuarentena")
    void calcularDisponibilidad_jarabeBaseSinFaltantes() {
        when(loteProductoRepository.findFefoDisponibles(38L, Integer.MAX_VALUE))
                .thenReturn(List.of(
                        lote(117L, "L-120925-3", new BigDecimal("10000"), new BigDecimal("10000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE),
                        lote(92L, "L-110925-3", new BigDecimal("30000"), new BigDecimal("30000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE),
                        lote(118L, "L-120925-2", new BigDecimal("10000"), new BigDecimal("10000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE),
                        lote(85L, "L-160925-2", new BigDecimal("25000"), new BigDecimal("25000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE),
                        lote(103L, "L-180925-8", new BigDecimal("59500"), new BigDecimal("59500"), BigDecimal.ZERO, EstadoLote.DISPONIBLE),
                        lote(119L, "L-050825-3", new BigDecimal("5000"), new BigDecimal("5000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE),
                        lote(156L, "L-151125-2", new BigDecimal("59650"), new BigDecimal("59650"), BigDecimal.ZERO, EstadoLote.DISPONIBLE),
                        lote(148L, "L-271025-00", new BigDecimal("5000"), new BigDecimal("5000"), BigDecimal.ZERO, EstadoLote.EN_CUARENTENA)
                ));

        DistribucionFefoResult resultado = service.calcularDisponibilidad(38L, new BigDecimal("139650"), List.of(), true);

        assertThat(resultado.isSuficiente()).isTrue();
        assertThat(resultado.getStockLibreTotal()).isEqualByComparingTo(new BigDecimal("199150.000000"));
        assertThat(resultado.getFaltante()).isEqualByComparingTo(BigDecimal.ZERO.setScale(6));
        assertThat(resultado.getDetalles()).hasSize(7);
        BigDecimal totalDistribuido = resultado.getDetalles().stream()
                .map(det -> det.getCantidadReserva() == null ? BigDecimal.ZERO : det.getCantidadReserva())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDistribuido).isEqualByComparingTo(new BigDecimal("139650.000000"));
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
