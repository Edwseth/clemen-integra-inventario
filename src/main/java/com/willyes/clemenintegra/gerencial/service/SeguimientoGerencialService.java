package com.willyes.clemenintegra.gerencial.service;

import com.willyes.clemenintegra.gerencial.dto.SeguimientoGerencialResponseDTO;

public interface SeguimientoGerencialService {
    SeguimientoGerencialResponseDTO obtenerSeguimiento(Long planSemanalId);
}
