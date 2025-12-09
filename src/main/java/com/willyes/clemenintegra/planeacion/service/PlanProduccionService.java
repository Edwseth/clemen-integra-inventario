package com.willyes.clemenintegra.planeacion.service;

import com.willyes.clemenintegra.planeacion.dto.PlanProduccionSemanalDTO;
import com.willyes.clemenintegra.planeacion.dto.PlanProduccionResumenDTO;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface PlanProduccionService {

    PlanProduccionSemanal crearOActualizar(PlanProduccionSemanalDTO dto);

    PlanProduccionSemanal confirmar(Long id);

    PlanProduccionSemanal cerrar(Long id);

    Optional<PlanProduccionSemanal> buscarPorId(Long id);

    Page<PlanProduccionResumenDTO> listar(LocalDate semanaInicioDesde, LocalDate semanaInicioHasta, EstadoPlanProduccion estado, Pageable pageable);
}
