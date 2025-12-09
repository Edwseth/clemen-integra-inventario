package com.willyes.clemenintegra.planeacion.service.impl;

import com.willyes.clemenintegra.planeacion.dto.PlanProduccionSemanalDTO;
import com.willyes.clemenintegra.planeacion.dto.PlanProduccionResumenDTO;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import com.willyes.clemenintegra.planeacion.repository.PlanProduccionSemanalRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class PlanProduccionServiceImplTest {

    @Mock
    private PlanProduccionSemanalRepository planProduccionSemanalRepository;

    @InjectMocks
    private PlanProduccionServiceImpl service;

    @Test
    void crearPlanAsignaSemanas() {
        LocalDate inicio = LocalDate.of(2025, 11, 24);
        LocalDate fin = LocalDate.of(2025, 11, 30);
        PlanProduccionSemanalDTO dto = PlanProduccionSemanalDTO.builder()
                .semanaInicio(inicio)
                .semanaFin(fin)
                .build();

        when(planProduccionSemanalRepository.save(any(PlanProduccionSemanal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlanProduccionSemanal plan = service.crearOActualizar(dto);

        assertEquals(inicio, plan.getSemanaInicio());
        assertEquals(fin, plan.getSemanaFin());
        assertEquals(EstadoPlanProduccion.BORRADOR, plan.getEstado());
    }

    @Test
    void crearPlanSinSemanasLanzaError() {
        PlanProduccionSemanalDTO dto = new PlanProduccionSemanalDTO();

        assertThrows(IllegalArgumentException.class, () -> service.crearOActualizar(dto));
    }

    @Test
    void editarPlanBorradorActualizaDatos() {
        PlanProduccionSemanal existente = PlanProduccionSemanal.builder()
                .id(10L)
                .estado(EstadoPlanProduccion.BORRADOR)
                .semanaInicio(LocalDate.of(2025, 11, 17))
                .semanaFin(LocalDate.of(2025, 11, 23))
                .build();
        PlanProduccionSemanalDTO dto = PlanProduccionSemanalDTO.builder()
                .id(10L)
                .semanaInicio(LocalDate.of(2025, 11, 24))
                .semanaFin(LocalDate.of(2025, 11, 30))
                .comentarios("actualizado")
                .build();

        when(planProduccionSemanalRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(planProduccionSemanalRepository.save(any(PlanProduccionSemanal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlanProduccionSemanal plan = service.crearOActualizar(dto);

        assertEquals(EstadoPlanProduccion.BORRADOR, plan.getEstado());
        assertEquals(dto.getSemanaInicio(), plan.getSemanaInicio());
        assertEquals(dto.getSemanaFin(), plan.getSemanaFin());
        assertEquals(dto.getComentarios(), plan.getComentarios());
    }

    @Test
    void confirmarPlanBorradorCambiaEstado() {
        PlanProduccionSemanal existente = PlanProduccionSemanal.builder()
                .id(20L)
                .estado(EstadoPlanProduccion.BORRADOR)
                .build();

        when(planProduccionSemanalRepository.findById(20L)).thenReturn(Optional.of(existente));
        when(planProduccionSemanalRepository.save(any(PlanProduccionSemanal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlanProduccionSemanal confirmado = service.confirmar(20L);

        assertEquals(EstadoPlanProduccion.CONFIRMADO, confirmado.getEstado());
    }

    @Test
    void editarPlanConfirmadoLanzaError() {
        PlanProduccionSemanal existente = PlanProduccionSemanal.builder()
                .id(30L)
                .estado(EstadoPlanProduccion.CONFIRMADO)
                .build();
        PlanProduccionSemanalDTO dto = PlanProduccionSemanalDTO.builder()
                .id(30L)
                .semanaInicio(LocalDate.now())
                .semanaFin(LocalDate.now())
                .build();

        when(planProduccionSemanalRepository.findById(30L)).thenReturn(Optional.of(existente));

        assertThrows(IllegalStateException.class, () -> service.crearOActualizar(dto));
    }

    @Test
    void confirmarPlanConfirmadoLanzaError() {
        PlanProduccionSemanal existente = PlanProduccionSemanal.builder()
                .id(40L)
                .estado(EstadoPlanProduccion.CONFIRMADO)
                .build();

        when(planProduccionSemanalRepository.findById(40L)).thenReturn(Optional.of(existente));

        assertThrows(IllegalStateException.class, () -> service.confirmar(40L));
    }

    @Test
    void cerrarPlanConfirmadoCambiaEstado() {
        PlanProduccionSemanal existente = PlanProduccionSemanal.builder()
                .id(50L)
                .estado(EstadoPlanProduccion.CONFIRMADO)
                .build();

        when(planProduccionSemanalRepository.findById(50L)).thenReturn(Optional.of(existente));
        when(planProduccionSemanalRepository.save(any(PlanProduccionSemanal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlanProduccionSemanal cerrado = service.cerrar(50L);

        assertEquals(EstadoPlanProduccion.CERRADO, cerrado.getEstado());
    }

    @Test
    void cerrarPlanEnBorradorLanzaError() {
        PlanProduccionSemanal existente = PlanProduccionSemanal.builder()
                .id(60L)
                .estado(EstadoPlanProduccion.BORRADOR)
                .build();

        when(planProduccionSemanalRepository.findById(60L)).thenReturn(Optional.of(existente));

        assertThrows(IllegalStateException.class, () -> service.cerrar(60L));
    }

    @Test
    void listarSinFiltrosUsaOrdenPorSemanaDesc() {
        PlanProduccionSemanal planReciente = PlanProduccionSemanal.builder()
                .id(1L)
                .semanaInicio(LocalDate.of(2024, 1, 8))
                .semanaFin(LocalDate.of(2024, 1, 14))
                .estado(EstadoPlanProduccion.CONFIRMADO)
                .build();
        PlanProduccionSemanal planAnterior = PlanProduccionSemanal.builder()
                .id(2L)
                .semanaInicio(LocalDate.of(2023, 12, 31))
                .semanaFin(LocalDate.of(2024, 1, 6))
                .estado(EstadoPlanProduccion.CERRADO)
                .build();

        when(planProduccionSemanalRepository.buscarPorFiltros(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(planReciente, planAnterior)));

        Page<PlanProduccionResumenDTO> resultado = service.listar(null, null, null, PageRequest.of(0, 10));

        assertEquals(2, resultado.getContent().size());
        assertEquals(planReciente.getSemanaInicio(), resultado.getContent().get(0).getSemanaInicio());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(planProduccionSemanalRepository, times(1))
                .buscarPorFiltros(eq(null), eq(null), eq(null), pageableCaptor.capture());
        Sort.Order sortOrder = pageableCaptor.getValue().getSort().getOrderFor("semanaInicio");
        assertNotNull(sortOrder);
        assertEquals(Sort.Direction.DESC, sortOrder.getDirection());
    }

    @Test
    void listarFiltradoPorEstado() {
        PlanProduccionSemanal planConfirmado = PlanProduccionSemanal.builder()
                .id(3L)
                .semanaInicio(LocalDate.of(2024, 2, 5))
                .semanaFin(LocalDate.of(2024, 2, 11))
                .estado(EstadoPlanProduccion.CONFIRMADO)
                .build();

        when(planProduccionSemanalRepository.buscarPorFiltros(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(planConfirmado)));

        service.listar(null, null, EstadoPlanProduccion.CONFIRMADO, PageRequest.of(0, 5, Sort.by("semanaInicio")));

        ArgumentCaptor<EstadoPlanProduccion> estadoCaptor = ArgumentCaptor.forClass(EstadoPlanProduccion.class);
        verify(planProduccionSemanalRepository).buscarPorFiltros(eq(null), eq(null), estadoCaptor.capture(), any(Pageable.class));
        assertEquals(EstadoPlanProduccion.CONFIRMADO, estadoCaptor.getValue());
    }

    @Test
    void listarIncluyeNombreCompletoDelCreador() {
        Usuario creador = Usuario.builder()
                .nombreCompleto("Juan Pérez")
                .nombreUsuario("JPEREZ")
                .build();
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .id(5L)
                .semanaInicio(LocalDate.of(2024, 4, 1))
                .semanaFin(LocalDate.of(2024, 4, 7))
                .estado(EstadoPlanProduccion.BORRADOR)
                .creadoPor(creador)
                .build();

        when(planProduccionSemanalRepository.buscarPorFiltros(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(plan)));

        Page<PlanProduccionResumenDTO> resultado = service.listar(null, null, null, PageRequest.of(0, 1));

        assertEquals("Juan Pérez", resultado.getContent().get(0).getCreadoPorNombre());
    }

    @Test
    void listarUsaNombreUsuarioCuandoNoHayNombreCompleto() {
        Usuario creador = Usuario.builder()
                .nombreUsuario("JPEREZ")
                .build();
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .id(6L)
                .semanaInicio(LocalDate.of(2024, 4, 8))
                .semanaFin(LocalDate.of(2024, 4, 14))
                .estado(EstadoPlanProduccion.BORRADOR)
                .creadoPor(creador)
                .build();

        when(planProduccionSemanalRepository.buscarPorFiltros(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(plan)));

        Page<PlanProduccionResumenDTO> resultado = service.listar(null, null, null, PageRequest.of(0, 1));

        assertEquals("JPEREZ", resultado.getContent().get(0).getCreadoPorNombre());
    }

    @Test
    void listarFiltradoPorRangoSemanaInicio() {
        LocalDate desde = LocalDate.of(2024, 3, 4);
        LocalDate hasta = LocalDate.of(2024, 3, 18);
        PlanProduccionSemanal planDentroDeRango = PlanProduccionSemanal.builder()
                .id(4L)
                .semanaInicio(LocalDate.of(2024, 3, 11))
                .semanaFin(LocalDate.of(2024, 3, 17))
                .estado(EstadoPlanProduccion.BORRADOR)
                .build();

        when(planProduccionSemanalRepository.buscarPorFiltros(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(planDentroDeRango)));

        Page<PlanProduccionResumenDTO> resultado = service.listar(desde, hasta, null, PageRequest.of(0, 5));

        assertEquals(1, resultado.getTotalElements());
        assertEquals(planDentroDeRango.getSemanaInicio(), resultado.getContent().get(0).getSemanaInicio());

        ArgumentCaptor<LocalDate> fechaCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(planProduccionSemanalRepository).buscarPorFiltros(fechaCaptor.capture(), fechaCaptor.capture(), eq(null), any(Pageable.class));
        List<LocalDate> capturedDates = fechaCaptor.getAllValues();
        assertEquals(desde, capturedDates.get(0));
        assertEquals(hasta, capturedDates.get(1));
    }
}
