package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.PicklistPtCreateRequest;
import com.willyes.clemenintegra.inventario.dto.PicklistPtLineaRequest;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.PicklistPt;
import com.willyes.clemenintegra.inventario.model.PicklistPtAsignacion;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.PicklistPtEstado;
import com.willyes.clemenintegra.inventario.model.enums.PicklistPtModoAsignacion;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.PicklistPtRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PicklistPtServiceImplTest {

    @Mock
    private PicklistPtRepository picklistRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private InventoryCatalogResolver catalogResolver;
    @Mock
    private MovimientoInventarioService movimientoInventarioService;
    @Mock
    private LoteCalidadValidator loteCalidadValidator;
    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private PicklistPtServiceImpl service;

    @Test
    void resolverAsignacionesAutoFefoRespetaVidaUtilMinima() {
        Producto producto = crearProducto(10);
        LoteProducto loteInvalido = crearLote(1L, producto, LocalDateTime.now().plusDays(5),
                new BigDecimal("10"), BigDecimal.ZERO, 10);
        LoteProducto loteValidoUno = crearLote(2L, producto, LocalDateTime.now().plusDays(20),
                new BigDecimal("2"), BigDecimal.ZERO, 10);
        LoteProducto loteValidoDos = crearLote(3L, producto, LocalDateTime.now().plusDays(40),
                new BigDecimal("4"), BigDecimal.ZERO, 10);

        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(new Usuario(1L));
        given(catalogResolver.getAlmacenPtId()).willReturn(10L);
        given(catalogResolver.getTipoDetalleSalidaPtId()).willReturn(20L);
        given(picklistRepository.findTopByCodigoStartingWithOrderByCodigoDesc(any()))
                .willReturn(Optional.empty());
        given(productoRepository.findById(10L)).willReturn(Optional.of(producto));
        given(loteProductoRepository.findFefoSalidaPt(eq(10L), eq(10L), any()))
                .willReturn(List.of(loteInvalido, loteValidoUno, loteValidoDos));
        given(picklistRepository.save(any(PicklistPt.class))).willAnswer(invocation -> invocation.getArgument(0));

        PicklistPtCreateRequest request = new PicklistPtCreateRequest(
                "Cliente X",
                10,
                null,
                "DOC-1",
                null,
                List.of(new PicklistPtLineaRequest(10L, new BigDecimal("5"),
                        PicklistPtModoAsignacion.AUTO_FEFO, null))
        );

        var response = service.crear(request);

        assertThat(response.asignaciones()).hasSize(2);
        assertThat(response.asignaciones().get(0).loteProductoId()).isEqualTo(2L);
        assertThat(response.asignaciones().get(0).cantidadAsignada()).isEqualByComparingTo("2.000000");
        assertThat(response.asignaciones().get(1).loteProductoId()).isEqualTo(3L);
        assertThat(response.asignaciones().get(1).cantidadAsignada()).isEqualByComparingTo("3.000000");
    }

    @Test
    void modoManualBloqueaVidaUtilInsuficiente() {
        Producto producto = crearProducto(11);
        LoteProducto lote = crearLote(3L, producto, LocalDateTime.now().plusDays(5),
                new BigDecimal("10"), BigDecimal.ZERO, 10);

        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(new Usuario(1L));
        given(catalogResolver.getAlmacenPtId()).willReturn(10L);
        given(catalogResolver.getTipoDetalleSalidaPtId()).willReturn(20L);
        given(picklistRepository.findTopByCodigoStartingWithOrderByCodigoDesc(any()))
                .willReturn(Optional.empty());
        given(productoRepository.findById(11L)).willReturn(Optional.of(producto));
        given(loteProductoRepository.findById(3L)).willReturn(Optional.of(lote));

        PicklistPtCreateRequest request = new PicklistPtCreateRequest(
                "Cliente Y",
                30,
                null,
                null,
                null,
                List.of(new PicklistPtLineaRequest(11L, new BigDecimal("4"),
                        PicklistPtModoAsignacion.MANUAL_LOTE, 3L))
        );

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(CustomBusinessException.class)
                .extracting(ex -> ((CustomBusinessException) ex).getCode())
                .isEqualTo(ApiErrorCode.PICKLIST_VIDA_UTIL_INSUFICIENTE);
    }

    @Test
    void modoManualFallaSiLoteNoEsDePt() {
        Producto producto = crearProducto(12);
        LoteProducto lote = crearLote(6L, producto, LocalDateTime.now().plusDays(20),
                new BigDecimal("10"), BigDecimal.ZERO, 11);

        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(new Usuario(1L));
        given(catalogResolver.getAlmacenPtId()).willReturn(10L);
        given(catalogResolver.getTipoDetalleSalidaPtId()).willReturn(20L);
        given(picklistRepository.findTopByCodigoStartingWithOrderByCodigoDesc(any()))
                .willReturn(Optional.empty());
        given(productoRepository.findById(12L)).willReturn(Optional.of(producto));
        given(loteProductoRepository.findById(6L)).willReturn(Optional.of(lote));

        PicklistPtCreateRequest request = new PicklistPtCreateRequest(
                "Cliente Z",
                null,
                null,
                null,
                null,
                List.of(new PicklistPtLineaRequest(12L, new BigDecimal("4"),
                        PicklistPtModoAsignacion.MANUAL_LOTE, 6L))
        );

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(CustomBusinessException.class)
                .extracting(ex -> ((CustomBusinessException) ex).getCode())
                .isEqualTo(ApiErrorCode.PICKLIST_LOTE_INVALIDO);
    }

    @Test
    void modoManualFallaSiLoteNoLiberado() {
        Producto producto = crearProducto(13);
        LoteProducto lote = crearLote(7L, producto, LocalDateTime.now().plusDays(20),
                new BigDecimal("10"), BigDecimal.ZERO, 10);
        lote.setEstado(com.willyes.clemenintegra.inventario.model.enums.EstadoLote.DISPONIBLE);

        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(new Usuario(1L));
        given(catalogResolver.getAlmacenPtId()).willReturn(10L);
        given(catalogResolver.getTipoDetalleSalidaPtId()).willReturn(20L);
        given(picklistRepository.findTopByCodigoStartingWithOrderByCodigoDesc(any()))
                .willReturn(Optional.empty());
        given(productoRepository.findById(13L)).willReturn(Optional.of(producto));
        given(loteProductoRepository.findById(7L)).willReturn(Optional.of(lote));

        PicklistPtCreateRequest request = new PicklistPtCreateRequest(
                "Cliente Z",
                null,
                null,
                null,
                null,
                List.of(new PicklistPtLineaRequest(13L, new BigDecimal("4"),
                        PicklistPtModoAsignacion.MANUAL_LOTE, 7L))
        );

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(CustomBusinessException.class)
                .extracting(ex -> ((CustomBusinessException) ex).getCode())
                .isEqualTo(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO);
    }

    @Test
    void modoManualFallaSiStockInsuficiente() {
        Producto producto = crearProducto(14);
        LoteProducto lote = crearLote(8L, producto, LocalDateTime.now().plusDays(20),
                new BigDecimal("2"), BigDecimal.ZERO, 10);

        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(new Usuario(1L));
        given(catalogResolver.getAlmacenPtId()).willReturn(10L);
        given(catalogResolver.getTipoDetalleSalidaPtId()).willReturn(20L);
        given(picklistRepository.findTopByCodigoStartingWithOrderByCodigoDesc(any()))
                .willReturn(Optional.empty());
        given(productoRepository.findById(14L)).willReturn(Optional.of(producto));
        given(loteProductoRepository.findById(8L)).willReturn(Optional.of(lote));

        PicklistPtCreateRequest request = new PicklistPtCreateRequest(
                "Cliente Z",
                null,
                null,
                null,
                null,
                List.of(new PicklistPtLineaRequest(14L, new BigDecimal("4"),
                        PicklistPtModoAsignacion.MANUAL_LOTE, 8L))
        );

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(CustomBusinessException.class)
                .extracting(ex -> ((CustomBusinessException) ex).getCode())
                .isEqualTo(ApiErrorCode.PICKLIST_STOCK_INSUFICIENTE);
    }

    @Test
    void ejecutarPicklistFallaSiStockCambio() {
        Producto producto = crearProducto(12);
        LoteProducto loteAsignado = crearLote(4L, producto, LocalDateTime.now().plusDays(40),
                new BigDecimal("5"), BigDecimal.ZERO, 10);
        PicklistPtAsignacion asignacion = PicklistPtAsignacion.builder()
                .id(7L)
                .producto(producto)
                .loteProducto(loteAsignado)
                .cantidadAsignada(new BigDecimal("8"))
                .build();
        PicklistPt picklist = PicklistPt.builder()
                .id(99L)
                .estado(PicklistPtEstado.CONFIRMADO)
                .almacenPtId(10)
                .tipoMovimientoDetalleId(20)
                .asignaciones(List.of(asignacion))
                .build();

        given(picklistRepository.findByIdWithLineas(99L)).willReturn(Optional.of(picklist));
        given(picklistRepository.findByIdWithAsignaciones(99L)).willReturn(Optional.of(picklist));
        given(loteProductoRepository.findByIdForUpdate(4L)).willReturn(Optional.of(loteAsignado));

        assertThatThrownBy(() -> service.ejecutar(99L))
                .isInstanceOf(CustomBusinessException.class)
                .extracting(ex -> ((CustomBusinessException) ex).getCode())
                .isEqualTo(ApiErrorCode.PICKLIST_DESACTUALIZADO);

        verify(movimientoInventarioService, never()).registrarMovimiento(any(), any());
    }

    private Producto crearProducto(int id) {
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.PRODUCTO_TERMINADO);
        UnidadMedida um = new UnidadMedida();
        um.setNombre("UND");
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre("Producto " + id);
        producto.setCategoriaProducto(categoria);
        producto.setUnidadMedida(um);
        return producto;
    }

    private LoteProducto crearLote(Long id,
                                   Producto producto,
                                   LocalDateTime vencimiento,
                                   BigDecimal stock,
                                   BigDecimal reservado,
                                   int almacenId) {
        LoteProducto lote = new LoteProducto();
        lote.setId(id);
        lote.setProducto(producto);
        lote.setEstado(com.willyes.clemenintegra.inventario.model.enums.EstadoLote.LIBERADO);
        lote.setFechaVencimiento(vencimiento);
        lote.setStockLote(stock);
        lote.setStockReservado(reservado);
        Almacen almacen = new Almacen();
        almacen.setId(almacenId);
        lote.setAlmacen(almacen);
        lote.setCodigoLote("L-" + id);
        return lote;
    }
}
