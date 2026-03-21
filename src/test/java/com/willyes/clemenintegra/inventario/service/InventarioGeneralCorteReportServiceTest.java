package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioGeneralCorteReportServiceTest {

    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private LoteProductoRepository loteProductoRepository;

    private final MovimientoSignosResolver movimientoSignosResolver = new MovimientoSignosResolver();

    @Test
    void reconstruyeTransferenciaFragmentadaUsandoLoteOrigenReal() {
        InventarioGeneralCorteReportService service =
                new InventarioGeneralCorteReportService(movimientoInventarioRepository, movimientoSignosResolver, loteProductoRepository);

        Producto producto = new Producto();
        producto.setId(21);
        producto.setCodigoSku("ME0114");
        producto.setNombre("Material Empaque");
        UnidadMedida udm = new UnidadMedida();
        udm.setNombre("KG");
        producto.setUnidadMedida(udm);

        Almacen principal = new Almacen(5);
        principal.setNombre("Principal Empaque");
        Almacen preBodega = new Almacen(7);
        preBodega.setNombre("Pre-Bodega Producción");

        LoteProducto lote2048 = new LoteProducto();
        lote2048.setId(2048L);
        lote2048.setProducto(producto);
        lote2048.setCodigoLote("02E-0001");
        lote2048.setAlmacen(principal);

        LoteProducto lote2492 = new LoteProducto();
        lote2492.setId(2492L);
        lote2492.setProducto(producto);
        lote2492.setCodigoLote("02E-0001");
        lote2492.setAlmacen(preBodega);
        lote2492.setLoteOrigen(lote2048);

        MovimientoInventario recepcion = new MovimientoInventario();
        recepcion.setProducto(producto);
        recepcion.setLote(lote2048);
        recepcion.setCantidad(new BigDecimal("3016"));
        recepcion.setTipoMovimiento(TipoMovimiento.RECEPCION);
        recepcion.setClasificacion(ClasificacionMovimientoInventario.RECEPCION_COMPRA);
        recepcion.setAlmacenDestino(principal);
        recepcion.setFechaIngreso(LocalDateTime.now().minusDays(10));

        MovimientoInventario salida = new MovimientoInventario();
        salida.setProducto(producto);
        salida.setLote(lote2048);
        salida.setCantidad(new BigDecimal("1"));
        salida.setTipoMovimiento(TipoMovimiento.SALIDA);
        salida.setClasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
        salida.setAlmacenOrigen(principal);
        salida.setFechaIngreso(LocalDateTime.now().minusDays(9));

        MovimientoInventario transferencia = new MovimientoInventario();
        transferencia.setProducto(producto);
        transferencia.setLote(lote2492);
        transferencia.setCantidad(new BigDecimal("1173"));
        transferencia.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        transferencia.setClasificacion(ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION);
        transferencia.setAlmacenOrigen(principal);
        transferencia.setAlmacenDestino(preBodega);
        transferencia.setFechaIngreso(LocalDateTime.now().minusDays(8));

        when(movimientoInventarioRepository.findAllByFechaIngresoLessThanEqual(any()))
                .thenReturn(List.of(recepcion, salida, transferencia));

        List<InventarioGeneralCorteReportService.InventarioGeneralRow> filas =
                service.calcularFilasInventarioGeneralCorte(LocalDateTime.now());

        assertThat(filas).hasSize(2);
        assertThat(filas)
                .extracting(InventarioGeneralCorteReportService.InventarioGeneralRow::lote,
                        InventarioGeneralCorteReportService.InventarioGeneralRow::ubicacion,
                        InventarioGeneralCorteReportService.InventarioGeneralRow::cant)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("02E-0001", "Principal Empaque", new BigDecimal("1842")),
                        org.assertj.core.groups.Tuple.tuple("02E-0001", "Pre-Bodega Producción", new BigDecimal("1173"))
                );
    }

    @Test
    void reconstruyeDevolucionDesdeProduccionSinInflarTotalCuandoFaltaLoteOrigenEnRuntime() {
        MovimientoSignosResolver resolver = new MovimientoSignosResolver(loteProductoRepository);
        InventarioGeneralCorteReportService service =
                new InventarioGeneralCorteReportService(movimientoInventarioRepository, resolver, loteProductoRepository);

        Producto producto = new Producto();
        producto.setId(21);
        producto.setCodigoSku("ME0114");
        producto.setNombre("Material Empaque");
        UnidadMedida udm = new UnidadMedida();
        udm.setNombre("KG");
        producto.setUnidadMedida(udm);

        Almacen principal = new Almacen(5);
        principal.setNombre("Principal Empaque");
        Almacen preBodega = new Almacen(6);
        preBodega.setNombre("Pre-Bodega Producción");

        LoteProducto lotePrincipal = new LoteProducto();
        lotePrincipal.setId(2048L);
        lotePrincipal.setProducto(producto);
        lotePrincipal.setCodigoLote("02E-0001");
        lotePrincipal.setAlmacen(principal);

        LoteProducto lotePreBodega = new LoteProducto();
        lotePreBodega.setId(2492L);
        lotePreBodega.setProducto(producto);
        lotePreBodega.setCodigoLote("02E-0001");
        lotePreBodega.setAlmacen(preBodega);

        MovimientoInventario recepcion = new MovimientoInventario();
        recepcion.setProducto(producto);
        recepcion.setLote(lotePrincipal);
        recepcion.setCantidad(new BigDecimal("3016"));
        recepcion.setTipoMovimiento(TipoMovimiento.RECEPCION);
        recepcion.setClasificacion(ClasificacionMovimientoInventario.RECEPCION_COMPRA);
        recepcion.setAlmacenDestino(principal);
        recepcion.setFechaIngreso(LocalDateTime.now().minusDays(10));

        MovimientoInventario transferencia = new MovimientoInventario();
        transferencia.setProducto(producto);
        transferencia.setLote(lotePreBodega);
        transferencia.setCantidad(new BigDecimal("1174"));
        transferencia.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        transferencia.setClasificacion(ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION);
        transferencia.setAlmacenOrigen(principal);
        transferencia.setAlmacenDestino(preBodega);
        transferencia.setFechaIngreso(LocalDateTime.now().minusDays(9));

        MovimientoInventario devolucion = new MovimientoInventario();
        devolucion.setProducto(producto);
        devolucion.setLote(lotePrincipal);
        devolucion.setCantidad(new BigDecimal("23"));
        devolucion.setTipoMovimiento(TipoMovimiento.DEVOLUCION);
        devolucion.setClasificacion(ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION);
        devolucion.setAlmacenOrigen(preBodega);
        devolucion.setAlmacenDestino(principal);
        devolucion.setFechaIngreso(LocalDateTime.now().minusDays(8));

        when(movimientoInventarioRepository.findAllByFechaIngresoLessThanEqual(any()))
                .thenReturn(List.of(recepcion, transferencia, devolucion));
        when(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId("02E-0001", 21, 6))
                .thenReturn(Optional.of(lotePreBodega));
        when(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId("02E-0001", 21, 5))
                .thenReturn(Optional.of(lotePrincipal));
        when(loteProductoRepository.findById(2492L)).thenReturn(Optional.of(lotePreBodega));

        List<InventarioGeneralCorteReportService.InventarioGeneralRow> filas =
                service.calcularFilasInventarioGeneralCorte(LocalDateTime.now());

        assertThat(filas).hasSize(2);
        assertThat(filas)
                .extracting(InventarioGeneralCorteReportService.InventarioGeneralRow::lote,
                        InventarioGeneralCorteReportService.InventarioGeneralRow::ubicacion,
                        InventarioGeneralCorteReportService.InventarioGeneralRow::cant)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("02E-0001", "Principal Empaque", new BigDecimal("1865")),
                        org.assertj.core.groups.Tuple.tuple("02E-0001", "Pre-Bodega Producción", new BigDecimal("1151"))
                );
    }
}
