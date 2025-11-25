package com.willyes.clemenintegra.planeacion.service.impl;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.DetalleCorridaMrp;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionDetalle;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoCorridaMrp;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoSugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.model.enums.TipoSugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.repository.CorridaMrpRepository;
import com.willyes.clemenintegra.planeacion.service.MrpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MrpServiceImpl implements MrpService {

    private final FormulaProductoRepository formulaProductoRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final CorridaMrpRepository corridaMrpRepository;

    @Override
    public CorridaMrp ejecutarCorridaSemana(PlanProduccionSemanal plan) {
        if (plan == null) {
            throw new IllegalArgumentException("El plan semanal es obligatorio");
        }
        if (plan.getEstado() != EstadoPlanProduccion.CONFIRMADO) {
            throw new IllegalStateException("El plan debe estar confirmado para ejecutar el MRP");
        }

        log.info("Iniciando corrida MRP semanal para el plan {}", plan.getId());
        CorridaMrp corrida = CorridaMrp.builder()
                .planProduccionSemanal(plan)
                .fechaEjecucion(LocalDateTime.now())
                .horizonteInicio(plan.getSemanaInicio())
                .horizonteFin(plan.getSemanaFin())
                .estado(EstadoCorridaMrp.EN_PROCESO)
                .versionFormulaUsada("APROBADA")
                .usuarioEjecucion(plan.getCreadoPor())
                .build();

        Map<Producto, BigDecimal> requerimientosBrutos = calcularRequerimientosBrutos(plan);
        List<DetalleCorridaMrp> requerimientosNetos = calcularRequerimientosNetos(requerimientosBrutos);
        requerimientosNetos.forEach(detalle -> detalle.setCorrida(corrida));
        corrida.setDetalles(requerimientosNetos);

        generarSugerencias(requerimientosNetos);
        corrida.setEstado(EstadoCorridaMrp.COMPLETADA);

        CorridaMrp guardada = corridaMrpRepository.save(corrida);
        log.info("Corrida MRP semanal {} finalizada. Insumos evaluados: {}", guardada.getId(), requerimientosNetos.size());
        return guardada;
    }

    @Override
    public Map<Producto, BigDecimal> calcularRequerimientosBrutos(PlanProduccionSemanal plan) {
        Map<Producto, BigDecimal> requerimientos = new HashMap<>();
        if (plan == null || plan.getDetalles() == null) {
            return requerimientos;
        }

        for (PlanProduccionDetalle detallePlan : plan.getDetalles()) {
            if (detallePlan.getProducto() == null || detallePlan.getCantidadPlanificada() == null) {
                continue;
            }
            Long productoId = detallePlan.getProducto().getId() != null ? detallePlan.getProducto().getId().longValue() : null;
            if (productoId == null) {
                continue;
            }
            Optional<FormulaProducto> formulaOpt = formulaProductoRepository
                    .findByProductoIdAndEstadoAndActivoTrue(productoId, EstadoFormula.APROBADA);
            if (formulaOpt.isEmpty()) {
                log.warn("No se encontró fórmula aprobada para el producto {}", productoId);
                continue;
            }

            for (DetalleFormula detalleFormula : formulaOpt.get().getDetalles()) {
                if (detalleFormula.getInsumo() == null || detalleFormula.getCantidadNecesaria() == null) {
                    continue;
                }
                BigDecimal requerido = detalleFormula.getCantidadNecesaria()
                        .multiply(detallePlan.getCantidadPlanificada());
                requerimientos.merge(detalleFormula.getInsumo(), requerido, BigDecimal::add);
            }
        }

        return requerimientos;
    }

    @Override
    public List<DetalleCorridaMrp> calcularRequerimientosNetos(Map<Producto, BigDecimal> requerimientosBrutos) {
        List<DetalleCorridaMrp> netos = new ArrayList<>();
        if (requerimientosBrutos == null || requerimientosBrutos.isEmpty()) {
            return netos;
        }

        for (Map.Entry<Producto, BigDecimal> entry : requerimientosBrutos.entrySet()) {
            Producto producto = entry.getKey();
            BigDecimal bruto = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;
            BigDecimal inventario = obtenerInventarioDisponible(producto);
            BigDecimal recepciones = BigDecimal.ZERO;
            BigDecimal neto = bruto.subtract(inventario).max(BigDecimal.ZERO);

            DetalleCorridaMrp detalle = DetalleCorridaMrp.builder()
                    .producto(producto)
                    .requerimientoBruto(bruto)
                    .inventarioDisponible(inventario)
                    .recepcionesProgramadas(recepciones)
                    .requerimientoNeto(neto)
                    .nivelBom(1)
                    .build();
            netos.add(detalle);
        }

        return netos;
    }

    @Override
    public List<SugerenciaAbastecimiento> generarSugerencias(List<DetalleCorridaMrp> requerimientosNetos) {
        List<SugerenciaAbastecimiento> sugerencias = new ArrayList<>();
        if (requerimientosNetos == null) {
            return sugerencias;
        }

        for (DetalleCorridaMrp detalle : requerimientosNetos) {
            if (detalle.getRequerimientoNeto() == null ||
                    detalle.getRequerimientoNeto().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Producto producto = detalle.getProducto();
            TipoSugerenciaAbastecimiento tipo = determinarTipo(producto);
            Integer leadTime = obtenerLeadTime(tipo, producto);
            LocalDate fechaNecesidad = detalle.getCorrida() != null && detalle.getCorrida().getHorizonteFin() != null
                    ? detalle.getCorrida().getHorizonteFin()
                    : LocalDate.now();
            LocalDate fechaLanzamiento = leadTime != null ? fechaNecesidad.minusDays(leadTime) : fechaNecesidad;

            SugerenciaAbastecimiento sugerencia = SugerenciaAbastecimiento.builder()
                    .detalleCorrida(detalle)
                    .tipo(tipo)
                    .cantidadSugerida(detalle.getRequerimientoNeto())
                    .fechaNecesidad(fechaNecesidad)
                    .fechaSugeridaLanzamiento(fechaLanzamiento)
                    .leadTimeDias(leadTime)
                    .estado(EstadoSugerenciaAbastecimiento.PENDIENTE)
                    .build();
            detalle.setSugerencia(sugerencia);
            sugerencias.add(sugerencia);
        }
        return sugerencias;
    }

    @Override
    @Transactional(readOnly = true)
    public CorridaMrp obtenerCorrida(Long id) {
        return corridaMrpRepository.findWithDetallesById(id)
                .orElseThrow(() -> new NoSuchElementException("Corrida MRP no encontrada"));
    }

    private BigDecimal obtenerInventarioDisponible(Producto producto) {
        if (producto == null || producto.getId() == null) {
            return BigDecimal.ZERO;
        }
        List<Object[]> resultados = loteProductoRepository.sumarPorEstado(producto.getId().longValue());
        Map<EstadoLote, BigDecimal> porEstado = new EnumMap<>(EstadoLote.class);
        for (Object[] fila : resultados) {
            if (fila.length >= 2 && fila[0] instanceof EstadoLote estado) {
                BigDecimal total = fila[1] != null ? (BigDecimal) fila[1] : BigDecimal.ZERO;
                porEstado.put(estado, total);
            }
        }
        BigDecimal disponible = porEstado.getOrDefault(EstadoLote.DISPONIBLE, BigDecimal.ZERO);
        BigDecimal liberado = porEstado.getOrDefault(EstadoLote.LIBERADO, BigDecimal.ZERO);
        return disponible.add(liberado);
    }

    private TipoSugerenciaAbastecimiento determinarTipo(Producto producto) {
        if (producto != null && producto.getCategoriaProducto() != null
                && producto.getCategoriaProducto().getTipo() == TipoCategoria.PRODUCTO_TERMINADO) {
            return TipoSugerenciaAbastecimiento.FABRICAR;
        }
        return TipoSugerenciaAbastecimiento.COMPRA;
    }

    private Integer obtenerLeadTime(TipoSugerenciaAbastecimiento tipo, Producto producto) {
        if (producto == null) {
            return null;
        }
        if (TipoSugerenciaAbastecimiento.FABRICAR == tipo) {
            return producto.getLeadTimeProduccionDias();
        }
        return producto.getLeadTimeCompraDias();
    }
}
