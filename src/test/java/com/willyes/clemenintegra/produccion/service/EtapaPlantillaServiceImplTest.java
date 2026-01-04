package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.repository.EtapaPlantillaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EtapaPlantillaServiceImplTest {

    private EtapaPlantillaRepository repository;
    private ChecklistEtapaTemplateService checklistEtapaTemplateService;
    private EtapaPlantillaServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(EtapaPlantillaRepository.class);
        checklistEtapaTemplateService = mock(ChecklistEtapaTemplateService.class);
        service = new EtapaPlantillaServiceImpl(repository, checklistEtapaTemplateService);
    }

    @Test
    @DisplayName("Crear etapa genera placeholder de checklist")
    void crearGeneraPlaceholder() {
        Producto producto = new Producto();
        producto.setId(10);
        EtapaPlantilla etapa = EtapaPlantilla.builder()
                .nombre("Empaque")
                .secuencia(1)
                .producto(producto)
                .build();

        when(repository.save(any(EtapaPlantilla.class))).thenAnswer(invocation -> {
            EtapaPlantilla saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        service.crear(etapa);

        ArgumentCaptor<EtapaPlantilla> captor = ArgumentCaptor.forClass(EtapaPlantilla.class);
        verify(checklistEtapaTemplateService).crearPlaceholderPorDefectoSiNoExiste(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Eliminar etapa elimina checklist template asociado")
    void eliminarEtapaBorraChecklist() {
        EtapaPlantilla etapa = EtapaPlantilla.builder()
                .id(8L)
                .nombre("Sellado")
                .build();
        when(repository.findById(8L)).thenReturn(Optional.of(etapa));

        service.eliminar(8L);

        verify(checklistEtapaTemplateService).eliminarPorEtapaPlantilla(8L);
        verify(repository).delete(etapa);
    }
}
