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

    private final InventoryVencidosProperties properties = new InventoryVencidosProperties();

    private LoteVencimientoChunkProcessor processor;

    @Test
    void process_expiraMueveAObsoletosYRegistraMovimientoConSystem() {
        processor = new LoteVencimientoChunkProcessor(loteProductoRepository, movimientoInventarioService, properties, entityManager);
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
}
