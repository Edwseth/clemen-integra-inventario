package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.KardexFiltro;
import com.willyes.clemenintegra.inventario.dto.KardexItemDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KardexServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @InjectMocks
    private KardexServiceImpl kardexService;

    @Test
    void calculaSaldoConEntradasYSalidas() {
        Producto producto = crearProducto(1, "SKU-01", "Producto A");
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        MovimientoInventario entrada = movimiento(LocalDateTime.now().minusDays(2), new BigDecimal("10"),
                TipoMovimiento.RECEPCION, ClasificacionMovimientoInventario.RECEPCION_COMPRA);
        MovimientoInventario salida = movimiento(LocalDateTime.now().minusDays(1), new BigDecimal("3"),
                TipoMovimiento.SALIDA, ClasificacionMovimientoInventario.SALIDA_PRODUCCION);

        when(movimientoInventarioRepository.buscarParaKardex(any(), any(), eq(1L), any(), any(), any(), any()))
                .thenReturn(List.of(entrada, salida));

        KardexFiltro filtro = KardexFiltro.builder().productoId(1L).build();
        List<KardexItemDTO> resultado = kardexService.obtenerKardex(filtro);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getSaldo()).isEqualByComparingTo("10");
        assertThat(resultado.get(1).getSaldo()).isEqualByComparingTo("7");
        assertThat(resultado.get(0).getCantidadEntrada()).isEqualByComparingTo("10");
        assertThat(resultado.get(1).getCantidadSalida()).isEqualByComparingTo("3");
    }

    @Test
    void respetaFiltroPorLote() {
        Producto producto = crearProducto(5, "SKU-05", "Producto F");
        LoteProducto lote = new LoteProducto();
        lote.setId(9L);
        lote.setProducto(producto);
        when(productoRepository.findByCodigoSku("SKU-05")).thenReturn(Optional.of(producto));
        when(loteProductoRepository.findAllByCodigoLoteAndProductoId("L-001", 5L)).thenReturn(List.of(lote));

        when(movimientoInventarioRepository.buscarParaKardex(any(), any(), eq(5L), eq(9L), any(), any(), any()))
                .thenReturn(List.of());

        KardexFiltro filtro = KardexFiltro.builder()
                .codigoSku("SKU-05")
                .codigoLote("L-001")
                .build();

        List<KardexItemDTO> resultado = kardexService.obtenerKardex(filtro);

        assertThat(resultado).isEmpty();
        verify(movimientoInventarioRepository).buscarParaKardex(null, null, 5L, 9L, null, null, null);
    }

    @Test
    void desambiguaLoteDuplicadoPorAlmacen() {
        Producto producto = crearProducto(8, "SKU-08", "Producto H");
        when(productoRepository.findByCodigoSku("SKU-08")).thenReturn(Optional.of(producto));

        Almacen almacen1 = new Almacen(1);
        Almacen almacen2 = new Almacen(2);

        LoteProducto loteAlmacen1 = new LoteProducto();
        loteAlmacen1.setId(100L);
        loteAlmacen1.setProducto(producto);
        loteAlmacen1.setAlmacen(almacen1);

        LoteProducto loteAlmacen2 = new LoteProducto();
        loteAlmacen2.setId(200L);
        loteAlmacen2.setProducto(producto);
        loteAlmacen2.setAlmacen(almacen2);

        when(loteProductoRepository.findAllByCodigoLoteAndProductoId("L-AMB", 8L))
                .thenReturn(List.of(loteAlmacen1, loteAlmacen2));
        when(movimientoInventarioRepository.buscarParaKardex(any(), any(), eq(8L), eq(200L), eq(2L), any(), any()))
                .thenReturn(List.of());

        KardexFiltro filtro = KardexFiltro.builder()
                .codigoSku("SKU-08")
                .codigoLote("L-AMB")
                .almacenId(2L)
                .build();

        List<KardexItemDTO> resultado = kardexService.obtenerKardex(filtro);

        assertThat(resultado).isEmpty();
        verify(movimientoInventarioRepository).buscarParaKardex(null, null, 8L, 200L, 2L, null, null);
    }

    @Test
    void lanzaErrorClaroCuandoLoteDuplicadoSinCriterios() {
        Producto producto = crearProducto(9, "SKU-09", "Producto I");
        when(productoRepository.findByCodigoSku("SKU-09")).thenReturn(Optional.of(producto));

        LoteProducto loteUno = new LoteProducto();
        loteUno.setId(300L);
        loteUno.setProducto(producto);

        LoteProducto loteDos = new LoteProducto();
        loteDos.setId(301L);
        loteDos.setProducto(producto);

        when(loteProductoRepository.findAllByCodigoLoteAndProductoId("L-AMB", 9L))
                .thenReturn(List.of(loteUno, loteDos));

        KardexFiltro filtro = KardexFiltro.builder()
                .codigoSku("SKU-09")
                .codigoLote("L-AMB")
                .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> kardexService.obtenerKardex(filtro))
                .isInstanceOf(CustomBusinessException.class)
                .hasMessageContaining("múltiples lotes");
    }

    @Test
    void calculaTransferenciaPorAlmacenDestino() {
        Producto producto = crearProducto(2, "SKU-02", "Producto B");
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));

        Almacen origen = new Almacen(1);
        origen.setNombre("Origen");
        Almacen destino = new Almacen(2);
        destino.setNombre("Destino");

        MovimientoInventario transferencia = movimiento(LocalDateTime.now(), new BigDecimal("5"),
                TipoMovimiento.TRANSFERENCIA, ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL);
        transferencia.setAlmacenOrigen(origen);
        transferencia.setAlmacenDestino(destino);

        when(movimientoInventarioRepository.buscarParaKardex(any(), any(), eq(2L), any(), eq(2L), any(), any()))
                .thenReturn(List.of(transferencia));

        KardexFiltro filtro = KardexFiltro.builder()
                .productoId(2L)
                .almacenId(2L)
                .build();

        List<KardexItemDTO> resultado = kardexService.obtenerKardex(filtro);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCantidadEntrada()).isEqualByComparingTo("5");
        assertThat(resultado.get(0).getSaldo()).isEqualByComparingTo("5");
    }

    @Test
    void filtraPorOrdenProduccion() {
        Producto producto = crearProducto(3, "SKU-03", "Producto C");
        when(productoRepository.findById(3L)).thenReturn(Optional.of(producto));

        MovimientoInventario movOp = movimiento(LocalDateTime.now(), new BigDecimal("4"),
                TipoMovimiento.SALIDA, ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
        com.willyes.clemenintegra.produccion.model.OrdenProduccion op = new com.willyes.clemenintegra.produccion.model.OrdenProduccion();
        op.setId(10L);
        movOp.setOrdenProduccion(op);

        when(movimientoInventarioRepository.buscarParaKardex(any(), any(), eq(3L), any(), any(), eq(10L), any()))
                .thenReturn(List.of(movOp));

        KardexFiltro filtro = KardexFiltro.builder()
                .productoId(3L)
                .ordenProduccionId(10L)
                .build();

        List<KardexItemDTO> resultado = kardexService.obtenerKardex(filtro);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCodigoSku()).isEqualTo("SKU-03");
        verify(movimientoInventarioRepository).buscarParaKardex(null, null, 3L, null, null, 10L, null);
    }

    private Producto crearProducto(Integer id, String sku, String nombre) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setCodigoSku(sku);
        producto.setNombre(nombre);
        return producto;
    }

    private MovimientoInventario movimiento(LocalDateTime fecha, BigDecimal cantidad,
                                           TipoMovimiento tipoMovimiento,
                                           ClasificacionMovimientoInventario clasificacion) {
        MovimientoInventario mov = new MovimientoInventario();
        mov.setFechaIngreso(fecha);
        mov.setCantidad(cantidad);
        mov.setTipoMovimiento(tipoMovimiento);
        mov.setClasificacion(clasificacion);
        mov.setLote(new LoteProducto());
        return mov;
    }
}
