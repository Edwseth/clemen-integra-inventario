package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.Almacen;
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
                        new MovimientoSignosResolver.AporteInventario(10L, new BigDecimal("-5312")),
                        new MovimientoSignosResolver.AporteInventario(20L, new BigDecimal("5312"))
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
                new MovimientoSignosResolver.AporteInventario(5L, new BigDecimal("-1"))
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
                new MovimientoSignosResolver.AporteInventario(5L, new BigDecimal("-12.5"))
        );
    }
}
