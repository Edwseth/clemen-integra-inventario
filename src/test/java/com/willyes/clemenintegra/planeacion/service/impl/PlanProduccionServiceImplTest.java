package com.willyes.clemenintegra.planeacion.service.impl;

import com.willyes.clemenintegra.planeacion.dto.PlanProduccionSemanalDTO;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import com.willyes.clemenintegra.planeacion.repository.PlanProduccionSemanalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
}
