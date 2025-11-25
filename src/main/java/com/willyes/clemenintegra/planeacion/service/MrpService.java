package com.willyes.clemenintegra.planeacion.service;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.DetalleCorridaMrp;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MrpService {

    CorridaMrp ejecutarCorridaSemana(PlanProduccionSemanal plan);

    Map<Producto, BigDecimal> calcularRequerimientosBrutos(PlanProduccionSemanal plan);

    List<DetalleCorridaMrp> calcularRequerimientosNetos(Map<Producto, BigDecimal> requerimientosBrutos);

    List<SugerenciaAbastecimiento> generarSugerencias(List<DetalleCorridaMrp> requerimientosNetos);

    CorridaMrp obtenerCorrida(Long id);
}
