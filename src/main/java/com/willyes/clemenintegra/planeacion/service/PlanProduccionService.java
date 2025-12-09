package com.willyes.clemenintegra.planeacion.service;

import com.willyes.clemenintegra.planeacion.dto.PlanProduccionSemanalDTO;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface PlanProduccionService {

    PlanProduccionSemanal crearOActualizar(PlanProduccionSemanalDTO dto);

    PlanProduccionSemanal confirmar(Long id);

    PlanProduccionSemanal cerrar(Long id);

    Optional<PlanProduccionSemanal> buscarPorId(Long id);

    Page<PlanProduccionSemanal> listar(LocalDate semanaInicio, LocalDate semanaFin, String estado, Pageable pageable);
}
