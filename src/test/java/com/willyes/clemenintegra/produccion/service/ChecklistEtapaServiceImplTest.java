package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.ChecklistItemDTO;
import com.willyes.clemenintegra.produccion.model.ChecklistEtapaItem;
import com.willyes.clemenintegra.produccion.model.ChecklistEtapaTemplate;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.model.enums.EstadoChecklistItem;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
        when(checklistRepository.countByEtapaProduccionId(10L)).thenReturn(1L);
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
        when(checklistRepository.countByEtapaProduccionId(7L)).thenReturn(1L);
        when(checklistRepository.findByEtapaProduccionIdOrderByIdAsc(7L)).thenReturn(List.of(
                ChecklistEtapaItem.builder().obligatorio(true).completado(false).nombrePaso("Paso faltante").build()
        ));

        assertThrows(CustomBusinessException.class, () -> service.validarChecklistCompleto(7L));
    }

    @Test
    @DisplayName("Obtener checklist por orden valida pertenencia y falla si no hay checklist configurado")
    void obtenerChecklistPorOrden_sinItems() {
        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(3L)
                .ordenProduccion(OrdenProduccion.builder().id(2L).build())
                .build();
        when(etapaProduccionRepository.findById(3L)).thenReturn(Optional.of(etapa));
        when(checklistRepository.countByEtapaProduccionId(3L)).thenReturn(1L);
        when(checklistRepository.findByEtapaProduccionIdOrderByIdAsc(3L)).thenReturn(List.of());

        assertThrows(CustomBusinessException.class, () -> service.obtenerPorOrdenYEtapa(2L, 3L));
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
                .secuencia(2)
                .ordenProduccion(OrdenProduccion.builder().id(3L)
                        .producto(Producto.builder().id(5).build())
                        .build())
                .build();
        when(checklistRepository.countByEtapaProduccionId(11L)).thenReturn(0L);
        when(etapaProduccionRepository.findById(11L)).thenReturn(Optional.of(etapa));
        EtapaPlantilla etapaPlantilla = EtapaPlantilla.builder().id(20L).build();
        when(etapaPlantillaRepository.findFirstByProductoIdAndSecuencia(5, 2)).thenReturn(Optional.of(etapaPlantilla));
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

    @Test
    @DisplayName("Generar checklist falla si plantilla solo tiene placeholder")
    void generarChecklistSoloPlaceholder() {
        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(12L)
                .nombre("Envasado")
                .secuencia(1)
                .ordenProduccion(OrdenProduccion.builder().id(4L)
                        .producto(Producto.builder().id(6).build())
                        .build())
                .build();
        when(checklistRepository.countByEtapaProduccionId(12L)).thenReturn(0L);
        when(etapaProduccionRepository.findById(12L)).thenReturn(Optional.of(etapa));
        EtapaPlantilla plantilla = EtapaPlantilla.builder().id(30L).build();
        when(etapaPlantillaRepository.findFirstByProductoIdAndSecuencia(6, 1)).thenReturn(Optional.of(plantilla));
        ChecklistEtapaTemplate placeholder = ChecklistEtapaTemplate.builder()
                .id(40L)
                .nombreItem(ChecklistEtapaTemplateService.PLACEHOLDER_NOMBRE)
                .obligatorio(true)
                .build();
        when(templateService.listarActivosPorEtapaPlantilla(30L)).thenReturn(List.of(placeholder));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(new Usuario());

        CustomBusinessException exception = assertThrows(CustomBusinessException.class,
                () -> service.ensureChecklistOperativo(12L));

        assertThat(exception.getCode()).isEqualTo(ApiErrorCode.PRODUCCION_CHECKLIST_NO_CONFIGURADO);
        verify(checklistRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Finalizar etapa con plantilla activa genera checklist y valida obligatorios pendientes")
    void validarChecklistGeneradoDesdeTemplate() {
        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(21L)
                .secuencia(3)
                .ordenProduccion(OrdenProduccion.builder().id(8L)
                        .producto(Producto.builder().id(9).build())
                        .build())
                .build();
        EtapaPlantilla plantilla = EtapaPlantilla.builder().id(31L).build();
        AtomicReference<List<ChecklistEtapaItem>> almacenados = new AtomicReference<>(List.of());

        when(checklistRepository.countByEtapaProduccionId(21L)).thenReturn(0L);
        when(etapaProduccionRepository.findById(21L)).thenReturn(Optional.of(etapa));
        when(etapaPlantillaRepository.findFirstByProductoIdAndSecuencia(9, 3)).thenReturn(Optional.of(plantilla));
        when(templateService.listarActivosPorEtapaPlantilla(31L)).thenReturn(List.of(
                ChecklistEtapaTemplate.builder().id(1L).nombreItem("Paso 1").obligatorio(true).build(),
                ChecklistEtapaTemplate.builder().id(2L).nombreItem("Paso 2").obligatorio(false).build(),
                ChecklistEtapaTemplate.builder().id(3L).nombreItem("Paso 3").obligatorio(true).build()
        ));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(new Usuario());
        when(checklistRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<ChecklistEtapaItem> lista = invocation.getArgument(0);
            almacenados.set(lista);
            return lista;
        });
        when(checklistRepository.findByEtapaProduccionIdOrderByIdAsc(21L))
                .thenAnswer(invocation -> almacenados.get());

        CustomBusinessException exception = assertThrows(CustomBusinessException.class,
                () -> service.validarChecklistCompleto(21L));

        assertThat(exception.getCode()).isEqualTo(ApiErrorCode.CHECKLIST_ETAPA_INCOMPLETO);
        assertThat(almacenados.get()).hasSize(3);
    }

    @Test
    @DisplayName("Validar checklist con plantilla solo placeholder lanza error de configuración")
    void validarChecklistSoloPlaceholderDesdeTemplate() {
        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(22L)
                .secuencia(4)
                .ordenProduccion(OrdenProduccion.builder().id(10L)
                        .producto(Producto.builder().id(11).build())
                        .build())
                .build();
        EtapaPlantilla plantilla = EtapaPlantilla.builder().id(32L).build();

        when(checklistRepository.countByEtapaProduccionId(22L)).thenReturn(0L);
        when(etapaProduccionRepository.findById(22L)).thenReturn(Optional.of(etapa));
        when(etapaPlantillaRepository.findFirstByProductoIdAndSecuencia(11, 4)).thenReturn(Optional.of(plantilla));
        when(templateService.listarActivosPorEtapaPlantilla(32L)).thenReturn(List.of(
                ChecklistEtapaTemplate.builder()
                        .id(90L)
                        .nombreItem(ChecklistEtapaTemplateService.PLACEHOLDER_NOMBRE)
                        .activo(true)
                        .build()
        ));

        CustomBusinessException exception = assertThrows(CustomBusinessException.class,
                () -> service.validarChecklistCompleto(22L));

        assertThat(exception.getCode()).isEqualTo(ApiErrorCode.PRODUCCION_CHECKLIST_NO_CONFIGURADO);
        verify(checklistRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Validar checklist placeholder lanza error configuracion")
    void validarChecklistSoloPlaceholder() {
        ChecklistEtapaItem placeholder = ChecklistEtapaItem.builder()
                .id(1L)
                .nombrePaso(ChecklistEtapaTemplateService.PLACEHOLDER_NOMBRE)
                .obligatorio(true)
                .build();

        when(checklistRepository.countByEtapaProduccionId(15L)).thenReturn(1L);
        when(checklistRepository.findByEtapaProduccionIdOrderByIdAsc(15L)).thenReturn(List.of(placeholder));

        assertThrows(CustomBusinessException.class, () -> service.validarChecklistCompleto(15L));
    }
}
