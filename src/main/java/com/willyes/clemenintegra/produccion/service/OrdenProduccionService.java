package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.OrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.dto.CierreProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.CierreProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public interface OrdenProduccionService {
    ResultadoValidacionOrdenDTO guardarConValidacionStock(OrdenProduccion orden);
    ResultadoValidacionOrdenDTO crearOrden(OrdenProduccionRequestDTO dto);
    List<OrdenProduccion> listarTodas();
    Optional<OrdenProduccion> buscarPorId(Long id);
    void eliminar(Long id);

    OrdenProduccion finalizar(Long id, BigDecimal cantidadProducida);

    OrdenProduccion registrarCierre(Long id, CierreProduccionRequestDTO dto);

    Page<CierreProduccionResponseDTO> listarCierres(Long id, Pageable pageable);

    Page<OrdenProduccionResponseDTO> listarPaginado(String codigo,
                                                    EstadoProduccion estado,
                                                    String responsable,
                                                    LocalDateTime fechaInicio,
                                                    LocalDateTime fechaFin,
                                                    Pageable pageable);
}
