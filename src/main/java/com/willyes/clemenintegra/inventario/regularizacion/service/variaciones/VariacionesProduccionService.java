package com.willyes.clemenintegra.inventario.regularizacion.service.variaciones;

import com.willyes.clemenintegra.inventario.regularizacion.dto.variaciones.RegularizacionDetalleDTO;
import com.willyes.clemenintegra.inventario.regularizacion.dto.variaciones.TipoVariacionDTO;
import com.willyes.clemenintegra.inventario.regularizacion.dto.variaciones.VariacionOPResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface VariacionesProduccionService {

    Page<VariacionOPResponseDTO> listarVariaciones(LocalDate fechaInicio,
                                                   LocalDate fechaFin,
                                                   Long ordenProduccionId,
                                                   boolean soloConVariacion,
                                                   TipoVariacionDTO tipoVariacion,
                                                   Pageable pageable);

    List<RegularizacionDetalleDTO> obtenerDetalle(Long regularizacionId);

    byte[] exportarExcel(LocalDate fechaInicio,
                        LocalDate fechaFin,
                        Long ordenProduccionId,
                        boolean soloConVariacion,
                        TipoVariacionDTO tipoVariacion);
}
