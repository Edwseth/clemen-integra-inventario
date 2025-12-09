package com.willyes.clemenintegra.planeacion.service.impl;

import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import com.willyes.clemenintegra.planeacion.repository.CorridaMrpRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class MrpServiceImplTest {

    @Mock
    private FormulaProductoRepository formulaProductoRepository;

    @Mock
    private LoteProductoRepository loteProductoRepository;

    @Mock
    private CorridaMrpRepository corridaMrpRepository;

    @InjectMocks
    private MrpServiceImpl service;

    @Test
    void noPermiteEjecutarMrpConPlanBorrador() {
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .estado(EstadoPlanProduccion.BORRADOR)
                .build();

        assertThrows(IllegalStateException.class, () -> service.ejecutarCorridaSemana(plan));
    }

    @Test
    void noPermiteEjecutarMrpConPlanCerrado() {
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .estado(EstadoPlanProduccion.CERRADO)
                .build();

        assertThrows(IllegalStateException.class, () -> service.ejecutarCorridaSemana(plan));
    }
}
