package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.NoConformidadDTO;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.OrigenNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface NoConformidadService {
    Page<NoConformidadDTO> listar(SeveridadNoConformidad severidad,
                                  OrigenNoConformidad origen,
                                  Pageable pageable);

    NoConformidadDTO crear(NoConformidadDTO dto, Usuario authUser);

    NoConformidadDTO actualizar(Long id, NoConformidadDTO dto);

    void eliminar(Long id);

    NoConformidadDTO obtenerPorId(Long id);

    NoConformidadDTO cerrar(Long id, Usuario authUser);

    NoConformidad registrarDesdeEvaluacion(LoteProducto lote,
                                           EvaluacionCalidad evaluacion,
                                           SeveridadNoConformidad severidad,
                                           String descripcion,
                                           Usuario usuario);

    Optional<NoConformidad> obtenerActivaPorLote(Long loteId);

    Optional<NoConformidad> obtenerActivaPorLoteYEvaluacion(Long loteId, Long evaluacionId);
}
