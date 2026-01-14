package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.PicklistPtCreateRequest;
import com.willyes.clemenintegra.inventario.dto.PicklistPtResponse;
import com.willyes.clemenintegra.inventario.dto.PicklistPtResumenDTO;
import com.willyes.clemenintegra.inventario.model.enums.PicklistPtEstado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface PicklistPtService {

    PicklistPtResponse crear(PicklistPtCreateRequest request);

    Page<PicklistPtResumenDTO> listar(PicklistPtEstado estado,
                                      String cliente,
                                      LocalDateTime desde,
                                      LocalDateTime hasta,
                                      Pageable pageable);

    PicklistPtResponse obtener(Long id);

    byte[] generarPdf(Long id);

    PicklistPtResponse confirmar(Long id);

    PicklistPtResponse ejecutar(Long id);

    PicklistPtResponse cancelar(Long id);
}
