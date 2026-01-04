package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.ChecklistItemDTO;
import com.willyes.clemenintegra.produccion.model.ChecklistEtapaItem;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.model.ChecklistEtapaTemplate;
import com.willyes.clemenintegra.produccion.model.enums.EstadoChecklistItem;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.produccion.repository.ChecklistEtapaItemRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaPlantillaRepository;
import com.willyes.clemenintegra.produccion.service.ChecklistEtapaTemplateService;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChecklistEtapaServiceImplTest {

    private ChecklistEtapaItemRepository checklistRepository;
    private EtapaProduccionRepository etapaProduccionRepository;
    private ChecklistEtapaTemplateService templateService;
    private EtapaPlantillaRepository etapaPlantillaRepository;
    private UsuarioService usuarioService;
    private ChecklistEtapaServiceImpl service;

    @BeforeEach
    void setUp() {
        checklistRepository = mock(ChecklistEtapaItemRepository.class);
        etapaProduccionRepository = mock(EtapaProduccionRepository.class);
        templateService = mock(ChecklistEtapaTemplateService.class);
        etapaPlantillaRepository = mock(EtapaPlantillaRepository.class);
        usuarioService = mock(UsuarioService.class);
        service = new ChecklistEtapaServiceImpl(checklistRepository, etapaProduccionRepository, templateService, etapaPlantillaRepository, usuarioService);
    }

    @Test
    @DisplayName("Obtiene checklist con faltantes obligatorios")
    void obtenerChecklist_conFaltantes() {
        EtapaProduccion etapa = EtapaProduccion.builder().id(10L).build();
        when(etapaProduccionRepository.findById(10L)).thenReturn(Optional.of(etapa));
        ChecklistEtapaItem incompleto = ChecklistEtapaItem.builder()
                .id(1L)
                .etapaProduccion(etapa)
                .nombrePaso("Paso 1")
                .obligatorio(true)
                .completado(false)
                .estado(EstadoChecklistItem.PENDIENTE)
                .build();
        when(checklistRepository.findByEtapaProduccionIdOrderByIdAsc(10L)).thenReturn(List.of(incompleto));

        var dto = service.obtenerPorEtapa(10L);

        assertThat(dto.getFaltantesObligatorios()).isEqualTo(1);
        assertThat(dto.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("Actualizar guarda items y marca completado por usuario autenticado")
    void actualizarChecklist_guarda() {
        EtapaProduccion etapa = EtapaProduccion.builder().id(5L).build();
        when(etapaProduccionRepository.findById(5L)).thenReturn(Optional.of(etapa));
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setNombreCompleto("Tester");
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(checklistRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ChecklistItemDTO payload = ChecklistItemDTO.builder()
                .nombrePaso("Validar limpieza")
                .obligatorio(true)
                .completado(true)
                .observacion("ok")
                .build();

        ArgumentCaptor<List<ChecklistEtapaItem>> captor = ArgumentCaptor.forClass(List.class);

        var dto = service.actualizar(5L, List.of(payload));

        verify(checklistRepository).saveAll(captor.capture());
        ChecklistEtapaItem guardado = captor.getValue().get(0);
        assertThat(guardado.getCompletedBy()).isEqualTo(usuario);
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getFaltantesObligatorios()).isZero();
    }

    @Test
    @DisplayName("Validar checklist incompleto lanza CustomBusinessException")
    void validarChecklist_incompleto() {
        when(checklistRepository.findByEtapaProduccionIdOrderByIdAsc(7L)).thenReturn(List.of(
                ChecklistEtapaItem.builder().obligatorio(true).completado(false).nombrePaso("Paso faltante").build()
        ));

        assertThrows(CustomBusinessException.class, () -> service.validarChecklistCompleto(7L));
    }

    @Test
    @DisplayName("Obtener checklist por orden valida pertenencia y retorna lista vacía sin plantilla")
    void obtenerChecklistPorOrden_sinItems() {
        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(3L)
                .ordenProduccion(OrdenProduccion.builder().id(2L).build())
                .build();
        when(etapaProduccionRepository.findById(3L)).thenReturn(Optional.of(etapa));
        when(checklistRepository.findByEtapaProduccionIdOrderByIdAsc(3L)).thenReturn(List.of());

        var dto = service.obtenerPorOrdenYEtapa(2L, 3L);

        assertThat(dto.getItems()).isEmpty();
        assertThat(dto.getFaltantesObligatorios()).isZero();
        assertThat(dto.getOrdenProduccionId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Obtener checklist por orden falla si etapa no pertenece a la orden")
    void obtenerChecklistPorOrden_etapaNoPertenece() {
        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(6L)
                .ordenProduccion(OrdenProduccion.builder().id(99L).build())
                .build();
        when(etapaProduccionRepository.findById(6L)).thenReturn(Optional.of(etapa));

        assertThrows(CustomBusinessException.class, () -> service.obtenerPorOrdenYEtapa(1L, 6L));
    }

    @Test
    @DisplayName("Marcar no aplica falla si el item no lo permite")
    void marcarNoAplica_noPermitido() {
        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(4L)
                .ordenProduccion(OrdenProduccion.builder().id(1L).build())
                .build();
        when(etapaProduccionRepository.findById(4L)).thenReturn(Optional.of(etapa));
        ChecklistEtapaItem item = ChecklistEtapaItem.builder()
                .id(8L)
                .etapaProduccion(etapa)
                .obligatorio(true)
                .permitirNoAplica(false)
                .estado(EstadoChecklistItem.PENDIENTE)
                .build();
        when(checklistRepository.findById(8L)).thenReturn(Optional.of(item));

        assertThrows(CustomBusinessException.class, () -> service.marcarNoAplica(1L, 4L, 8L, "N/A"));
    }

    @Test
    @DisplayName("Genera checklist desde plantilla cuando no hay items")
    void generarChecklistDesdeTemplate() {
        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(11L)
                .nombre("Mezclado")
                .ordenProduccion(OrdenProduccion.builder().id(3L)
                        .producto(Producto.builder().id(5).build())
                        .build())
                .build();
        when(checklistRepository.countByEtapaProduccionId(11L)).thenReturn(0L);
        when(etapaProduccionRepository.findById(11L)).thenReturn(Optional.of(etapa));
        EtapaPlantilla etapaPlantilla = EtapaPlantilla.builder().id(20L).build();
        when(etapaPlantillaRepository.findFirstByProductoIdAndNombreIgnoreCase(5, "Mezclado")).thenReturn(Optional.of(etapaPlantilla));
        ChecklistEtapaTemplate template = ChecklistEtapaTemplate.builder()
                .id(30L)
                .nombreItem("Verificar limpieza")
                .obligatorio(true)
                .permitirNoAplica(true)
                .build();
        when(templateService.listarActivosPorEtapaPlantilla(20L)).thenReturn(List.of(template));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(new Usuario());

        service.generarChecklistDesdeTemplateSiNoExiste(11L);

        verify(checklistRepository).saveAll(anyList());
    }
}
