package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.config.InventoryVencidosProperties;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoteVencimientoChunkProcessorTest {

    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private MovimientoInventarioService movimientoInventarioService;
    @Mock
    private EntityManager entityManager;
    @Mock
    private PlatformTransactionManager txManager;

    private final InventoryVencidosProperties properties = new InventoryVencidosProperties();

    private LoteVencimientoChunkProcessor processor;

    @Test
    void process_expiraMueveAObsoletosYRegistraMovimientoConSystem() {
        processor = new LoteVencimientoChunkProcessor(loteProductoRepository, movimientoInventarioService, properties, entityManager, txManager);
        properties.setEstadoObjetivo(EstadoLote.VENCIDO);
        properties.getMovimiento().setEnabled(true);
        properties.getMovimiento().setMotivoId(1L);
        properties.getMovimiento().setClasificacion("RECHAZO_CALIDAD");

        LoteProducto lote = new LoteProducto();
        lote.setId(10L);
        lote.setCodigoLote("L-EXP");
        lote.setEstado(EstadoLote.LIBERADO);
        lote.setFechaVencimiento(LocalDateTime.now().minusDays(1));
        lote.setStockLote(new BigDecimal("5.000000"));
        lote.setAlmacen(new Almacen(1));

        Usuario system = new Usuario();
        system.setId(53L);
        system.setNombreUsuario("SYSTEM");

        when(loteProductoRepository.findByIdWithLock(10L)).thenReturn(Optional.of(lote));
        when(movimientoInventarioService.existeMovimientoVencimientoHoy(eq(10L), eq(1L), any(), any())).thenReturn(false);
        when(entityManager.getReference(Almacen.class, 3)).thenReturn(new Almacen(3));
        mockTransactions();

        LoteVencimientoChunkProcessor.ChunkResult result = processor.process(
                List.of(10L),
                LocalDateTime.now(),
                LocalDateTime.now().withHour(0).withMinute(0),
                LocalDateTime.now().withHour(23).withMinute(59),
                LocalDateTime.now(),
                3L,
                system);

        assertThat(result.actualizados()).isEqualTo(1);
        assertThat(result.movimientos()).isEqualTo(1);
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.VENCIDO);
        assertThat(lote.getAlmacen().getId()).isEqualTo(3);

        verify(movimientoInventarioService).registrarRetiroPorVencimiento(eq(lote), eq(properties), eq(3L), eq(system), any());
        verify(loteProductoRepository).save(lote);
    }

    @Test
    void process_mergeaCuandoExisteDestinoYEvitaColisionUnica() {
        processor = new LoteVencimientoChunkProcessor(loteProductoRepository, movimientoInventarioService, properties, entityManager, txManager);
        properties.setEstadoObjetivo(EstadoLote.VENCIDO);
        properties.getMovimiento().setEnabled(true);
        properties.getMovimiento().setMotivoId(1L);
        properties.getMovimiento().setClasificacion("RECHAZO_CALIDAD");

        LoteProducto origen = new LoteProducto();
        origen.setId(95L);
        origen.setCodigoLote("FTA030240325");
        origen.setEstado(EstadoLote.LIBERADO);
        origen.setFechaVencimiento(LocalDateTime.now().minusDays(1));
        origen.setStockLote(new BigDecimal("474.140000"));
        origen.setStockReservado(new BigDecimal("0.000000"));
        origen.setAgotado(false);
        origen.setProducto(com.willyes.clemenintegra.inventario.model.Producto.builder().id(97).build());
        origen.setAlmacen(new Almacen(1));

        LoteProducto destino = new LoteProducto();
        destino.setId(3288L);
        destino.setCodigoLote("FTA030240325");
        destino.setEstado(EstadoLote.VENCIDO);
        destino.setFechaVencimiento(LocalDateTime.now().minusDays(2));
        destino.setStockLote(new BigDecimal("10.000000"));
        destino.setStockReservado(new BigDecimal("1.000000"));
        destino.setAgotado(false);
        destino.setProducto(com.willyes.clemenintegra.inventario.model.Producto.builder().id(97).build());
        destino.setAlmacen(new Almacen(3));

        Usuario system = new Usuario();
        system.setId(53L);

        when(loteProductoRepository.findByIdWithLock(95L)).thenReturn(Optional.of(origen));
        when(movimientoInventarioService.existeMovimientoVencimientoHoy(eq(95L), eq(1L), any(), any())).thenReturn(false);
        when(loteProductoRepository.findByProductoIdAndCodigoLoteAndAlmacenIdForUpdate(97, "FTA030240325", 3))
                .thenReturn(Optional.of(destino));
        mockTransactions();

        LoteVencimientoChunkProcessor.ChunkResult result = processor.process(
                List.of(95L),
                LocalDateTime.now(),
                LocalDateTime.now().withHour(0).withMinute(0),
                LocalDateTime.now().withHour(23).withMinute(59),
                LocalDateTime.now(),
                3L,
                system);

        assertThat(result.actualizados()).isEqualTo(1);
        assertThat(result.movimientos()).isEqualTo(1);
        assertThat(origen.getEstado()).isEqualTo(EstadoLote.VENCIDO);
        assertThat(origen.isAgotado()).isTrue();
        assertThat(origen.getStockLote()).isEqualByComparingTo("0");
        assertThat(destino.getStockLote()).isEqualByComparingTo("484.140000");
        verify(loteProductoRepository, times(2)).save(any(LoteProducto.class));
        verify(entityManager, never()).getReference(Almacen.class, 3);
    }

    @Test
    void process_siUnLoteFallaContinuaConElSiguiente() {
        processor = new LoteVencimientoChunkProcessor(loteProductoRepository, movimientoInventarioService, properties, entityManager, txManager);
        properties.setEstadoObjetivo(EstadoLote.VENCIDO);
        properties.getMovimiento().setEnabled(true);
        properties.getMovimiento().setMotivoId(1L);
        properties.getMovimiento().setClasificacion("RECHAZO_CALIDAD");

        LoteProducto loteOk = new LoteProducto();
        loteOk.setId(2219L);
        loteOk.setCodigoLote("202403525");
        loteOk.setEstado(EstadoLote.LIBERADO);
        loteOk.setFechaVencimiento(LocalDateTime.now().minusDays(1));
        loteOk.setStockLote(new BigDecimal("440.000000"));
        loteOk.setStockReservado(BigDecimal.ZERO);
        loteOk.setProducto(com.willyes.clemenintegra.inventario.model.Producto.builder().id(328).build());
        loteOk.setAlmacen(new Almacen(1));

        when(loteProductoRepository.findByIdWithLock(95L)).thenThrow(new RuntimeException("error forzado"));
        when(loteProductoRepository.findByIdWithLock(2219L)).thenReturn(Optional.of(loteOk));
        when(movimientoInventarioService.existeMovimientoVencimientoHoy(eq(2219L), eq(1L), any(), any())).thenReturn(false);
        when(loteProductoRepository.findByProductoIdAndCodigoLoteAndAlmacenIdForUpdate(328, "202403525", 3))
                .thenReturn(Optional.empty());
        when(entityManager.getReference(Almacen.class, 3)).thenReturn(new Almacen(3));
        mockTransactions();

        LoteVencimientoChunkProcessor.ChunkResult result = processor.process(
                List.of(95L, 2219L),
                LocalDateTime.now(),
                LocalDateTime.now().withHour(0).withMinute(0),
                LocalDateTime.now().withHour(23).withMinute(59),
                LocalDateTime.now(),
                3L,
                new Usuario());

        assertThat(result.actualizados()).isEqualTo(1);
        assertThat(result.movimientos()).isEqualTo(1);
        assertThat(loteOk.getEstado()).isEqualTo(EstadoLote.VENCIDO);
        assertThat(loteOk.getAlmacen().getId()).isEqualTo(3);
    }

    private void mockTransactions() {
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }
}
