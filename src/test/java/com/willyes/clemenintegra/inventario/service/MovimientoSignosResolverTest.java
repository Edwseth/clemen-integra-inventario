package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MovimientoSignosResolverTest {

    private final MovimientoSignosResolver resolver = new MovimientoSignosResolver();

    @Test
    void trataComoTransferenciaCuandoLaClasificacionEsDeTransferenciaAunqueTipoSeaEntrada() {
        MovimientoInventario mov = new MovimientoInventario();
        mov.setCantidad(new BigDecimal("5312"));
        mov.setTipoMovimiento(TipoMovimiento.ENTRADA);
        mov.setClasificacion(ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL);
        mov.setAlmacenOrigen(new Almacen(10));
        mov.setAlmacenDestino(new Almacen(20));

        List<MovimientoSignosResolver.AporteInventario> aportes = resolver.resolverAportesPorAlmacen(mov);

        assertThat(aportes)
                .containsExactlyInAnyOrder(
                        new MovimientoSignosResolver.AporteInventario(null, 10L, new BigDecimal("-5312")),
                        new MovimientoSignosResolver.AporteInventario(null, 20L, new BigDecimal("5312"))
                );
    }

    @Test
    void transferenciaConSoloDestinoCuentaComoEntradaEnDestino() {
        MovimientoInventario mov = new MovimientoInventario();
        mov.setCantidad(new BigDecimal("5312"));
        mov.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        mov.setAlmacenDestino(new Almacen(20));

        assertThat(resolver.calcularEntrada(mov, 20L)).isEqualByComparingTo("5312");
        assertThat(resolver.calcularSalida(mov, 20L)).isEqualByComparingTo("0");
    }

    @Test
    void transferenciaConSoloOrigenCuentaComoSalidaEnOrigen() {
        MovimientoInventario mov = new MovimientoInventario();
        mov.setCantidad(new BigDecimal("5312"));
        mov.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        mov.setAlmacenOrigen(new Almacen(10));

        assertThat(resolver.calcularSalida(mov, 10L)).isEqualByComparingTo("5312");
        assertThat(resolver.calcularEntrada(mov, 10L)).isEqualByComparingTo("0");
    }

    @Test
    void salidaMuestraConOrigenYDestinoSigueSiendoSalidaReal() {
        MovimientoInventario mov = new MovimientoInventario();
        mov.setCantidad(BigDecimal.ONE);
        mov.setTipoMovimiento(TipoMovimiento.SALIDA);
        mov.setClasificacion(ClasificacionMovimientoInventario.SALIDA_MUESTRA_CALIDAD);
        mov.setAlmacenOrigen(new Almacen(5));
        mov.setAlmacenDestino(new Almacen(6));

        List<MovimientoSignosResolver.AporteInventario> aportes = resolver.resolverAportesPorAlmacen(mov);

        assertThat(aportes).containsExactly(
                new MovimientoSignosResolver.AporteInventario(null, 5L, new BigDecimal("-1"))
        );
    }

    @Test
    void salidaProduccionConOrigenYDestinoSigueSiendoSalidaReal() {
        MovimientoInventario mov = new MovimientoInventario();
        mov.setCantidad(new BigDecimal("12.5"));
        mov.setTipoMovimiento(TipoMovimiento.SALIDA);
        mov.setClasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
        mov.setAlmacenOrigen(new Almacen(5));
        mov.setAlmacenDestino(new Almacen(6));

        List<MovimientoSignosResolver.AporteInventario> aportes = resolver.resolverAportesPorAlmacen(mov);

        assertThat(aportes).containsExactly(
                new MovimientoSignosResolver.AporteInventario(null, 5L, new BigDecimal("-12.5"))
        );
    }

    @Test
    void transferenciaFragmentadaUsaLoteOrigenRealParaElDescuento() {
        Almacen origen = new Almacen(5);
        Almacen destino = new Almacen(7);

        LoteProducto loteOrigen = new LoteProducto();
        loteOrigen.setId(2048L);
        loteOrigen.setAlmacen(origen);

        LoteProducto loteDestino = new LoteProducto();
        loteDestino.setId(2492L);
        loteDestino.setAlmacen(destino);
        loteDestino.setLoteOrigen(loteOrigen);

        MovimientoInventario mov = new MovimientoInventario();
        mov.setCantidad(new BigDecimal("1182"));
        mov.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        mov.setClasificacion(ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION);
        mov.setAlmacenOrigen(origen);
        mov.setAlmacenDestino(destino);
        mov.setLote(loteDestino);

        List<MovimientoSignosResolver.AporteInventario> aportes = resolver.resolverAportesPorAlmacen(mov);

        assertThat(aportes).containsExactly(
                new MovimientoSignosResolver.AporteInventario(2048L, 5L, new BigDecimal("-1182")),
                new MovimientoSignosResolver.AporteInventario(2492L, 7L, new BigDecimal("1182"))
        );
    }

    @Test
    void devolucionDesdeProduccionSeReconstruyeComoMovimientoEntreAlmacenes() {
        Almacen preBodega = new Almacen(6);
        Almacen principal = new Almacen(5);

        LoteProducto loteOrigen = new LoteProducto();
        loteOrigen.setId(4101L);
        loteOrigen.setAlmacen(preBodega);

        LoteProducto loteDestino = new LoteProducto();
        loteDestino.setId(5120L);
        loteDestino.setAlmacen(principal);
        loteDestino.setLoteOrigen(loteOrigen);

        MovimientoInventario mov = new MovimientoInventario();
        mov.setCantidad(new BigDecimal("23"));
        mov.setTipoMovimiento(TipoMovimiento.DEVOLUCION);
        mov.setClasificacion(ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION);
        mov.setAlmacenOrigen(preBodega);
        mov.setAlmacenDestino(principal);
        mov.setLote(loteDestino);

        List<MovimientoSignosResolver.AporteInventario> aportes = resolver.resolverAportesPorAlmacen(mov);

        assertThat(aportes).containsExactly(
                new MovimientoSignosResolver.AporteInventario(4101L, 6L, new BigDecimal("-23")),
                new MovimientoSignosResolver.AporteInventario(5120L, 5L, new BigDecimal("23"))
        );
        assertThat(resolver.calcularSalida(mov, 6L)).isEqualByComparingTo("23");
        assertThat(resolver.calcularEntrada(mov, 5L)).isEqualByComparingTo("23");
        assertThat(resolver.calcularEntrada(mov, 6L)).isEqualByComparingTo("0");
        assertThat(resolver.calcularSalida(mov, 5L)).isEqualByComparingTo("0");
    }
}
