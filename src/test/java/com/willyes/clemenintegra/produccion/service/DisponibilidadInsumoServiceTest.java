package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.inventario.dto.LoteFefoDisponibleProjection;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoResult;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import org.junit.jupiter.api.BeforeEach;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @BeforeEach
    void setUp() {
        lenient().when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(null);
    }

    @Test
    @DisplayName("calcularDisponibilidad mantiene faltante y stock en preview y real")
    void calcularDisponibilidad_previewYRealIguales() {
        when(loteProductoRepository.findFefoDisponibles(10L, Integer.MAX_VALUE))
                .thenReturn(List.of(
                        lote(1L, "A", new BigDecimal("4.000000"), new BigDecimal("5.000000"), new BigDecimal("1.000000"), EstadoLote.DISPONIBLE, 5L),
                        lote(2L, "B", new BigDecimal("2.000000"), new BigDecimal("2.000000"), BigDecimal.ZERO, EstadoLote.LIBERADO, 5L)
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
                        lote(5L, "C", new BigDecimal("2.500000"), new BigDecimal("3.000000"), new BigDecimal("0.500000"), EstadoLote.DISPONIBLE, 7L)
                ));
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
                        lote(10L, "L-170925-3", new BigDecimal("125.000000"), new BigDecimal("125.000000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE, 5L),
                        lote(11L, "L-271025-02", new BigDecimal("130.000000"), new BigDecimal("500.000000"), new BigDecimal("370.000000"), EstadoLote.DISPONIBLE, 5L),
                        lote(12L, "L-060825-3", new BigDecimal("47.500000"), new BigDecimal("150.000000"), new BigDecimal("102.500000"), EstadoLote.DISPONIBLE, 5L)
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
                        lote(117L, "L-120925-3", new BigDecimal("10000"), new BigDecimal("10000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE, 5L),
                        lote(92L, "L-110925-3", new BigDecimal("30000"), new BigDecimal("30000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE, 5L),
                        lote(118L, "L-120925-2", new BigDecimal("10000"), new BigDecimal("10000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE, 5L),
                        lote(85L, "L-160925-2", new BigDecimal("25000"), new BigDecimal("25000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE, 5L),
                        lote(103L, "L-180925-8", new BigDecimal("59500"), new BigDecimal("59500"), BigDecimal.ZERO, EstadoLote.DISPONIBLE, 5L),
                        lote(119L, "L-050825-3", new BigDecimal("5000"), new BigDecimal("5000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE, 5L),
                        lote(156L, "L-151125-2", new BigDecimal("59650"), new BigDecimal("59650"), BigDecimal.ZERO, EstadoLote.DISPONIBLE, 5L),
                        lote(148L, "L-271025-00", new BigDecimal("5000"), new BigDecimal("5000"), BigDecimal.ZERO, EstadoLote.EN_CUARENTENA, 5L)
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

    @Test
    @DisplayName("calcularDisponibilidad omite FEFO cuando el insumo es SIN_CONTROL_STOCK")
    void calcularDisponibilidad_insumoSinControlStock() {
        Producto insumo = new Producto();
        insumo.setId(99);
        insumo.setModoControlInventario(ModoControlInventario.SIN_CONTROL_STOCK);
        when(productoRepository.findById(99L)).thenReturn(Optional.of(insumo));

        DistribucionFefoResult resultado = service.calcularDisponibilidad(99L, new BigDecimal("15"), List.of(7L), false);

        assertThat(resultado.isSuficiente()).isTrue();
        assertThat(resultado.getFaltante()).isEqualByComparingTo(BigDecimal.ZERO.setScale(6));
        assertThat(resultado.getStockLibreTotal()).isEqualByComparingTo(new BigDecimal("15.000000"));
        verify(loteProductoRepository, never()).findFefoDisponibles(anyLong(), anyInt());
    }

    @Test
    @DisplayName("calcularDisponibilidad PS LIBERADO cubre requerido y no bloquea")
    void calcularDisponibilidad_psLiberadoCubreRequerido() {
        when(loteProductoRepository.findFefoDisponibles(80L, Integer.MAX_VALUE))
                .thenReturn(List.of(
                        lote(401L, "PS-001", new BigDecimal("2860.000000"), new BigDecimal("2860.000000"),
                                BigDecimal.ZERO, EstadoLote.LIBERADO, 9L)
                ));

        DistribucionFefoResult resultado = service.calcularDisponibilidad(80L, new BigDecimal("90"), List.of(), true);

        assertThat(resultado.isSuficiente()).isTrue();
        assertThat(resultado.getStockLibreTotal()).isEqualByComparingTo(new BigDecimal("2860.000000"));
        assertThat(resultado.getFaltante()).isEqualByComparingTo(BigDecimal.ZERO.setScale(6));
    }

    @Test
    @DisplayName("calcularDisponibilidad PS en cuarentena o retenido no aporta stock libre")
    void calcularDisponibilidad_psNoElegiblePorEstado() {
        when(loteProductoRepository.findFefoDisponibles(81L, Integer.MAX_VALUE))
                .thenReturn(List.of(
                        lote(402L, "PS-002", new BigDecimal("2860.000000"), new BigDecimal("2860.000000"),
                                BigDecimal.ZERO, EstadoLote.EN_CUARENTENA, 9L)
                ));

        DistribucionFefoResult resultado = service.calcularDisponibilidad(81L, new BigDecimal("90"), List.of(), true);

        assertThat(resultado.isSuficiente()).isFalse();
        assertThat(resultado.getStockLibreTotal()).isEqualByComparingTo(BigDecimal.ZERO.setScale(6));
        assertThat(resultado.getFaltante()).isEqualByComparingTo(new BigDecimal("90.000000"));
    }

    @Test
    @DisplayName("calcularDisponibilidad descuenta reservas parciales sin quedar en cero")
    void calcularDisponibilidad_descuentaReservasParciales() {
        when(loteProductoRepository.findFefoDisponibles(82L, Integer.MAX_VALUE))
                .thenReturn(List.of(
                        lote(403L, "PS-003", new BigDecimal("70.000000"), new BigDecimal("100.000000"),
                                new BigDecimal("30.000000"), EstadoLote.LIBERADO, 9L)
                ));

        DistribucionFefoResult resultado = service.calcularDisponibilidad(82L, new BigDecimal("60"), List.of(), true);

        assertThat(resultado.isSuficiente()).isTrue();
        assertThat(resultado.getStockLibreTotal()).isEqualByComparingTo(new BigDecimal("70.000000"));
        assertThat(resultado.getFaltante()).isEqualByComparingTo(BigDecimal.ZERO.setScale(6));
    }

    @Test
    @DisplayName("calcularDisponibilidad excluye lotes de Pre-Bodega Producción")
    void calcularDisponibilidad_excluyePreBodega() {
        when(loteProductoRepository.findFefoDisponibles(55L, Integer.MAX_VALUE))
                .thenReturn(List.of(
                        lote(200L, "PB-001", new BigDecimal("4.000000"), new BigDecimal("4.000000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE, 6L),
                        lote(201L, "MP-001", new BigDecimal("4.000000"), new BigDecimal("4.000000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE, 5L)
                ));
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(6L);

        DistribucionFefoResult resultado = service.calcularDisponibilidad(55L, new BigDecimal("3"), List.of(), true);

        assertThat(resultado.getDetalles()).hasSize(1);
        assertThat(resultado.getDetalles().get(0).getAlmacenId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("calcularDisponibilidad no falla cuando hay stock suficiente en almacén origen válido")
    void calcularDisponibilidad_stockSuficienteOrigenValido() {
        when(loteProductoRepository.findFefoDisponibles(65L, Integer.MAX_VALUE))
                .thenReturn(List.of(
                        lote(210L, "PB-003", new BigDecimal("2.000000"), new BigDecimal("2.000000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE, 6L),
                        lote(211L, "MP-002", new BigDecimal("5.000000"), new BigDecimal("5.000000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE, 5L)
                ));
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(6L);

        DistribucionFefoResult resultado = service.calcularDisponibilidad(65L, new BigDecimal("3"), List.of(5L), false);

        assertThat(resultado.isSuficiente()).isTrue();
        assertThat(resultado.getFaltante()).isEqualByComparingTo(BigDecimal.ZERO.setScale(6));
    }

    @Test
    @DisplayName("calcularDisponibilidad falla si solo hay stock en Pre-Bodega Producción")
    void calcularDisponibilidad_preBodegaUnicoOrigen() {
        when(loteProductoRepository.findFefoDisponibles(66L, Integer.MAX_VALUE))
                .thenReturn(List.of(
                        lote(300L, "PB-002", new BigDecimal("5.000000"), new BigDecimal("5.000000"), BigDecimal.ZERO, EstadoLote.DISPONIBLE, 6L)
                ));
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(6L);

        assertThatThrownBy(() -> service.calcularDisponibilidad(66L, new BigDecimal("1"), List.of(), false))
                .isInstanceOf(CustomBusinessException.class)
                .satisfies(ex -> {
                    CustomBusinessException error = (CustomBusinessException) ex;
                    assertThat(error.getCode()).isEqualTo(ApiErrorCode.PREBODEGA_ORIGEN_INVALIDO);
                    assertThat(error.getDetails())
                            .isInstanceOfSatisfying(java.util.Map.class, details -> assertThat(details).containsKeys(
                                    "productoInsumoId",
                                    "preBodegaId",
                                    "almacenOrigenId",
                                    "requerido",
                                    "disponibleElegible",
                                    "faltante"));
                });
    }

    private LoteFefoDisponibleProjection lote(Long id, String codigo, BigDecimal stockLibre,
                                               BigDecimal stockFisico, BigDecimal stockReservado,
                                               EstadoLote estado, Long almacenId) {
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
                return almacenId;
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
