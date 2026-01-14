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
        LoteProducto loteValido = crearLote(2L, producto, LocalDateTime.now().plusDays(20),
                new BigDecimal("10"), BigDecimal.ZERO, 10);

        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(new Usuario(1L));
        given(catalogResolver.getAlmacenPtId()).willReturn(10L);
        given(catalogResolver.getTipoDetalleSalidaPtId()).willReturn(20L);
        given(picklistRepository.findTopByCodigoStartingWithOrderByCodigoDesc(any()))
                .willReturn(Optional.empty());
        given(productoRepository.findById(10L)).willReturn(Optional.of(producto));
        given(loteProductoRepository.findFefoSalidaPt(eq(10L), eq(10L), any()))
                .willReturn(List.of(loteInvalido, loteValido));
        given(picklistRepository.save(any(PicklistPt.class))).willAnswer(invocation -> invocation.getArgument(0));

        PicklistPtCreateRequest request = new PicklistPtCreateRequest(
                "Cliente X",
                10,
                "DOC-1",
                null,
                List.of(new PicklistPtLineaRequest(10L, new BigDecimal("5"),
                        PicklistPtModoAsignacion.AUTO_FEFO, null))
        );

        var response = service.crear(request);

        assertThat(response.asignaciones()).hasSize(1);
        assertThat(response.asignaciones().get(0).loteProductoId()).isEqualTo(2L);
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
                List.of(new PicklistPtLineaRequest(11L, new BigDecimal("4"),
                        PicklistPtModoAsignacion.MANUAL_LOTE, 3L))
        );

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(CustomBusinessException.class)
                .extracting(ex -> ((CustomBusinessException) ex).getCode())
                .isEqualTo(ApiErrorCode.PICKLIST_VIDA_UTIL_INSUFICIENTE);
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

        given(picklistRepository.findWithDetallesById(99L)).willReturn(Optional.of(picklist));
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
