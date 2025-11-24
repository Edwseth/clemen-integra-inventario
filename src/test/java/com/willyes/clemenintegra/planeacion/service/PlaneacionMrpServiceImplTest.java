package com.willyes.clemenintegra.planeacion.service;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.planeacion.dto.CorridaMrpResponseDTO;
import com.willyes.clemenintegra.planeacion.dto.MrpSimpleRequestDTO;
import com.willyes.clemenintegra.planeacion.mapper.PlaneacionMrpMapper;
import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.repository.CorridaMrpRepository;
import com.willyes.clemenintegra.planeacion.repository.SugerenciaAbastecimientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaneacionMrpServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private OrdenCompraDetalleRepository ordenCompraDetalleRepository;
    @Mock
    private CorridaMrpRepository corridaMrpRepository;
    @Mock
    private SugerenciaAbastecimientoRepository sugerenciaAbastecimientoRepository;

    private PlaneacionMrpServiceImpl service;

    @BeforeEach
    void setUp() {
        PlaneacionMrpMapper mapper = Mappers.getMapper(PlaneacionMrpMapper.class);
        service = new PlaneacionMrpServiceImpl(
                productoRepository,
                loteProductoRepository,
                ordenCompraDetalleRepository,
                corridaMrpRepository,
                sugerenciaAbastecimientoRepository,
                mapper
        );

        when(corridaMrpRepository.save(any(CorridaMrp.class))).thenAnswer(invocation -> {
            CorridaMrp corrida = invocation.getArgument(0);
            corrida.setId(1L);
            if (corrida.getSugerencias() != null) {
                long idx = 1;
                for (var sugerencia : corrida.getSugerencias()) {
                    sugerencia.setId(idx++);
                }
            }
            return corrida;
        });
    }

    @Test
    void generarSugerenciaCuandoStockPorDebajoDePuntoAccion() {
        Producto producto = Producto.builder()
                .id(1)
                .codigoSku("SKU-1")
                .nombre("Producto 1")
                .stockMinimo(new BigDecimal("10"))
                .stockSeguridad(new BigDecimal("2"))
                .modoControlInventario(ModoControlInventario.CONTROL_STOCK)
                .categoriaProducto(CategoriaProducto.builder().nombre("CAT").build())
                .activo(true)
                .build();

        when(productoRepository.findAll(any(Specification.class))).thenReturn(List.of(producto));
        List<Object[]> sumas = Collections.singletonList(new Object[]{EstadoLote.DISPONIBLE, new BigDecimal("5")});
        when(loteProductoRepository.sumarPorEstado(1L)).thenReturn(sumas);
        when(ordenCompraDetalleRepository.sumarCantidadPendientePorProductoYEstados(eq(1L), anyList()))
                .thenReturn(new BigDecimal("1"));

        MrpSimpleRequestDTO request = MrpSimpleRequestDTO.builder()
                .horizonteDesde(LocalDate.of(2024, 1, 10))
                .build();

        CorridaMrpResponseDTO response = service.ejecutarMrpSimple(request, 10L);

        assertThat(response.getTotalSugerencias()).isEqualTo(1);
        assertThat(response.getSugerencias()).hasSize(1);
        assertThat(response.getSugerencias().get(0).getCantidadSugerida())
                .isEqualByComparingTo(new BigDecimal("6.000000"));
    }

    @Test
    void noGenerarSugerenciaCuandoStockCubierto() {
        Producto producto = Producto.builder()
                .id(2)
                .codigoSku("SKU-2")
                .nombre("Producto 2")
                .stockMinimo(new BigDecimal("5"))
                .modoControlInventario(ModoControlInventario.CONTROL_STOCK)
                .activo(true)
                .build();

        when(productoRepository.findAll(any(Specification.class))).thenReturn(List.of(producto));
        List<Object[]> sumas = Collections.singletonList(new Object[]{EstadoLote.DISPONIBLE, new BigDecimal("10")});
        when(loteProductoRepository.sumarPorEstado(2L)).thenReturn(sumas);
        when(ordenCompraDetalleRepository.sumarCantidadPendientePorProductoYEstados(eq(2L), anyList()))
                .thenReturn(BigDecimal.ZERO);

        CorridaMrpResponseDTO response = service.ejecutarMrpSimple(new MrpSimpleRequestDTO(), 20L);

        assertThat(response.getTotalSugerencias()).isZero();
        assertThat(response.getSugerencias()).isEmpty();
    }
}
