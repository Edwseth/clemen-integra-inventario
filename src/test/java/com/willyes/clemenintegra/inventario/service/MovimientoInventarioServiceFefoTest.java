package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.LoteConsumoDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioServiceFefoTest {

    @Mock
    private AlmacenRepository almacenRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private OrdenCompraRepository ordenCompraRepository;
    @Mock
    private OrdenCompraService ordenCompraService;
    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private MovimientoInventarioMapper mapper;
    @Mock
    private BitacoraCambiosInventarioService bitacoraCambiosInventarioService;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Mock
    private SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    @Mock
    private InventoryCatalogResolver catalogResolver;
    @Mock
    private ReservaLoteService reservaLoteService;
    @Mock
    private ReservaLoteRepository reservaLoteRepository;
    @Mock
    private RecepcionOCService recepcionOCService;
    @Mock
    private LoteCalidadValidator loteCalidadValidator;
    @Mock
    private EntityManager entityManager;
    @Mock
    private EtapaProduccionRepository etapaProduccionRepository;
    @Mock
    private CosteoInventarioService costeoInventarioService;
    @Mock
    private UbicacionFisicaRepository ubicacionFisicaRepository;

    @InjectMocks
    private MovimientoInventarioServiceImpl service;

    @org.junit.jupiter.api.BeforeEach
    void defaultCosteoInventario() {
        lenient().when(costeoInventarioService.calcularCostoUnitarioRecepcion(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO.setScale(6));
        lenient().when(costeoInventarioService.calcularCostoTotalLineaRecepcion(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO.setScale(6));
        lenient().when(costeoInventarioService.calcularCostoUnitarioPromedioPorIngreso(any(), any()))
                .thenReturn(BigDecimal.ZERO.setScale(6));
        lenient().when(costeoInventarioService.calcularCostoTotalMovimiento(any(), any()))
                .thenReturn(BigDecimal.ZERO.setScale(6));
    }

    @Test
    void simulateFefo_ordenaPorVencimientoYDescartaEstadosNoElegibles() {
        Producto producto = new Producto();
        producto.setId(1);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(10L);
        producto.setUnidadMedida(unidad);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(catalogResolver.decimals(unidad)).thenReturn(2);

        LoteProducto primero = crearLote(1001L, "L1", EstadoLote.DISPONIBLE,
                LocalDateTime.now().plusDays(5), new BigDecimal("100"), BigDecimal.ZERO, 1);
        LoteProducto segundo = crearLote(1002L, "L2", EstadoLote.LIBERADO,
                LocalDateTime.now().plusDays(10), new BigDecimal("80"), new BigDecimal("10"), 1);

        List<LoteProducto> elegibles = new ArrayList<>(List.of(primero, segundo));
        when(loteProductoRepository.findByProductoIdAndEstadoInOrderByFechaVencimientoAscIdAsc(eq(1L), any()))
                .thenReturn(elegibles);
        lenient().when(loteProductoRepository.findByProductoIdAndEstadoIn(eq(1L), any()))
                .thenReturn(List.of());

        List<LoteConsumoDTO> resultado = service.simulateFefo(1L, new BigDecimal("150"), null);

        assertThat(resultado).hasSize(2);
        LoteConsumoDTO primeroDto = resultado.get(0);
        LoteConsumoDTO segundoDto = resultado.get(1);

        assertThat(primeroDto.getLoteId()).isEqualTo(1001L);
        assertThat(primeroDto.getTomar()).isEqualByComparingTo(new BigDecimal("100.000000"));
        assertThat(primeroDto.getDisponibleDespues()).isEqualByComparingTo(BigDecimal.ZERO.setScale(6));

        assertThat(segundoDto.getLoteId()).isEqualTo(1002L);
        assertThat(segundoDto.getTomar()).isEqualByComparingTo(new BigDecimal("50.000000"));
        assertThat(segundoDto.getDisponibleAntes()).isEqualByComparingTo(new BigDecimal("70.000000"));
        assertThat(segundoDto.getDisponibleDespues()).isEqualByComparingTo(new BigDecimal("20.000000"));
    }


    @Test
    void simulateFefo_excluyeLotesVencidosYQueVencenHoy() {
        Producto producto = new Producto();
        producto.setId(2);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(10L);
        producto.setUnidadMedida(unidad);

        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));
        when(catalogResolver.decimals(unidad)).thenReturn(2);

        LoteProducto venceHoy = crearLote(2001L, "L-HOY", EstadoLote.LIBERADO,
                LocalDateTime.now().withHour(0).withMinute(0), new BigDecimal("50"), BigDecimal.ZERO, 1);
        LoteProducto futuro = crearLote(2002L, "L-FUT", EstadoLote.LIBERADO,
                LocalDateTime.now().plusDays(3), new BigDecimal("50"), BigDecimal.ZERO, 1);

        when(loteProductoRepository.findByProductoIdAndEstadoInOrderByFechaVencimientoAscIdAsc(eq(2L), any()))
                .thenReturn(List.of(venceHoy, futuro));
        lenient().when(loteProductoRepository.findByProductoIdAndEstadoIn(eq(2L), any()))
                .thenReturn(List.of());

        List<LoteConsumoDTO> resultado = service.simulateFefo(2L, new BigDecimal("25"), null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getLoteId()).isEqualTo(2002L);
    }

    private LoteProducto crearLote(Long id,
                                   String codigo,
                                   EstadoLote estado,
                                   LocalDateTime vencimiento,
                                   BigDecimal stock,
                                   BigDecimal reservado,
                                   Integer almacenId) {
        LoteProducto lote = new LoteProducto();
        lote.setId(id);
        lote.setCodigoLote(codigo);
        lote.setEstado(estado);
        lote.setFechaVencimiento(vencimiento);
        lote.setStockLote(stock);
        lote.setStockReservado(reservado);
        lote.setAlmacen(new Almacen(almacenId));
        lote.setAgotado(false);
        return lote;
    }
}
