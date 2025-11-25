package com.willyes.clemenintegra.planeacion.service.impl;

import com.willyes.clemenintegra.planeacion.dto.PlanProduccionSemanalDTO;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.repository.PlanProduccionSemanalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

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
    }

    @Test
    void crearPlanSinSemanasLanzaError() {
        PlanProduccionSemanalDTO dto = new PlanProduccionSemanalDTO();

        assertThrows(IllegalArgumentException.class, () -> service.crearOActualizar(dto));
    }
}
