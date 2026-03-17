package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.config.InventoryVencidosProperties;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoteVencimientoJobServiceTest {

    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private LoteVencimientoChunkProcessor chunkProcessor;
    @Mock
    private LoteVencimientoContextResolver contextResolver;

    private InventoryVencidosProperties properties;
    private LoteVencimientoJobService service;

    @BeforeEach
    void setUp() {
        properties = new InventoryVencidosProperties();
        properties.setChunkSize(10);
        properties.getJob().setTimezone("UTC");
        properties.getMovimiento().setEnabled(true);
        properties.getMovimiento().setMotivoId(1L);
        properties.getMovimiento().setClasificacion("RECHAZO_CALIDAD");
        service = new LoteVencimientoJobService(properties, loteProductoRepository, chunkProcessor, contextResolver);
    }

    @Test
    void ejecutar_fallaControladoSiNoExisteAlmacenObsoletos() {
        when(contextResolver.resolveAlmacenDestinoId(properties))
                .thenThrow(new IllegalStateException("No existe el almacén destino"));

        assertThatThrownBy(() -> service.ejecutar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("almacén destino");
    }

    @Test
    void ejecutar_fallaControladoSiNoExisteUsuarioSystem() {
        when(contextResolver.resolveAlmacenDestinoId(properties)).thenReturn(3L);
        when(contextResolver.resolveUsuarioSistema())
                .thenThrow(new IllegalStateException("USUARIO_SISTEMA_NO_CONFIGURADO"));

        assertThatThrownBy(() -> service.ejecutar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("USUARIO_SISTEMA_NO_CONFIGURADO");
    }

    @Test
    void ejecutar_procesaYAcumulaResultados() {
        Usuario system = new Usuario();
        system.setId(53L);

        when(contextResolver.resolveAlmacenDestinoId(properties)).thenReturn(3L);
        when(contextResolver.resolveUsuarioSistema()).thenReturn(system);
        when(loteProductoRepository.findIdsParaExpirar(any(), anyCollection(), any(Pageable.class)))
                .thenReturn(List.of(11L, 12L))
                .thenReturn(List.of());
        when(chunkProcessor.process(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new LoteVencimientoChunkProcessor.ChunkResult(2, 2));

        LoteVencimientoJobService.LoteVencimientoExecutionResult result = service.ejecutar();

        assertThat(result.totalActualizados()).isEqualTo(2);
        assertThat(result.totalMovimientos()).isEqualTo(2);
    }
}
