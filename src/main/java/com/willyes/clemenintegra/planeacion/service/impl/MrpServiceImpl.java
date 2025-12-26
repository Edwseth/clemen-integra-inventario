package com.willyes.clemenintegra.planeacion.service.impl;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraDetalleRepository;
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
import com.willyes.clemenintegra.planeacion.model.enums.TipoCambioMrp;
import com.willyes.clemenintegra.planeacion.repository.CorridaMrpRepository;
import com.willyes.clemenintegra.planeacion.service.MrpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MrpServiceImpl implements MrpService {

    private final FormulaProductoRepository formulaProductoRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final OrdenCompraDetalleRepository ordenCompraDetalleRepository;
    private final CorridaMrpRepository corridaMrpRepository;

    @Override
    public CorridaMrp ejecutarCorridaSemana(PlanProduccionSemanal plan) {
        if (plan == null) {
            throw new IllegalArgumentException("El plan semanal es obligatorio");
        }
        if (plan.getEstado() == EstadoPlanProduccion.BORRADOR) {
            throw new IllegalStateException("No se puede ejecutar MRP sobre un plan en BORRADOR; primero debe confirmarse");
        }
        if (plan.getEstado() == EstadoPlanProduccion.CERRADO) {
            throw new IllegalStateException("No se puede ejecutar MRP sobre un plan CERRADO");
        }
        if (plan.getEstado() != EstadoPlanProduccion.CONFIRMADO) {
            throw new IllegalStateException("El plan debe estar confirmado para ejecutar el MRP");
        }

        log.info("Iniciando corrida MRP semanal para el plan {}", plan.getId());
        Optional<CorridaMrp> corridaAnterior = corridaMrpRepository
                .findTopByPlanProduccionSemanalAndEstadoOrderByFechaEjecucionDesc(
                        plan, EstadoCorridaMrp.COMPLETADA
                );
        Map<String, BigDecimal> netosAnteriores = construirMapaNetos(corridaAnterior);

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
        List<DetalleCorridaMrp> requerimientosNetos = calcularRequerimientosNetos(
                requerimientosBrutos, corrida.getHorizonteInicio(), corrida.getHorizonteFin());
        requerimientosNetos.forEach(detalle -> {
            detalle.setCorrida(corrida);
            asignarTipoCambio(detalle, netosAnteriores);
        });
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
    public List<DetalleCorridaMrp> calcularRequerimientosNetos(Map<Producto, BigDecimal> requerimientosBrutos,
                                                               LocalDate horizonteInicio,
                                                               LocalDate horizonteFin) {
        List<DetalleCorridaMrp> netos = new ArrayList<>();
        if (requerimientosBrutos == null || requerimientosBrutos.isEmpty()) {
            return netos;
        }

        for (Map.Entry<Producto, BigDecimal> entry : requerimientosBrutos.entrySet()) {
            Producto producto = entry.getKey();
            BigDecimal bruto = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;
            BigDecimal inventario = obtenerInventarioDisponible(producto);
            BigDecimal recepciones = obtenerRecepcionesProgramadas(producto, horizonteInicio, horizonteFin);
            BigDecimal neto = bruto.subtract(inventario).subtract(recepciones).max(BigDecimal.ZERO);

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
            // Métricas de consumo y cobertura para exponer al frontend.
            BigDecimal consumoTotal = Optional.ofNullable(detalle.getRequerimientoBruto()).orElse(BigDecimal.ZERO);
            sugerencia.setConsumoTotalPeriodo(consumoTotal);

            BigDecimal horizonteSemanas = calcularHorizonteSemanas(detalle.getCorrida());
            BigDecimal consumoSemanalPromedio = calcularConsumoSemanalPromedio(consumoTotal, horizonteSemanas);
            sugerencia.setConsumoSemanalPromedio(consumoSemanalPromedio);

            BigDecimal semanasCobertura = calcularSemanasCobertura(detalle, consumoSemanalPromedio);
            sugerencia.setSemanasCobertura(semanasCobertura);

            // Determinar criticidad basada en la cobertura y lead time.
            asignarCriticidad(sugerencia, leadTime);
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

    private Map<String, BigDecimal> construirMapaNetos(Optional<CorridaMrp> corridaAnterior) {
        Map<String, BigDecimal> netos = new HashMap<>();
        if (corridaAnterior.isEmpty() || corridaAnterior.get().getDetalles() == null) {
            return netos;
        }
        for (DetalleCorridaMrp detalle : corridaAnterior.get().getDetalles()) {
            String clave = construirClaveDetalle(detalle.getProducto(), detalle.getNivelBom());
            BigDecimal neto = Optional.ofNullable(detalle.getRequerimientoNeto()).orElse(BigDecimal.ZERO);
            netos.put(clave, neto);
        }
        return netos;
    }

    private void asignarTipoCambio(DetalleCorridaMrp detalle, Map<String, BigDecimal> netosAnteriores) {
        String clave = construirClaveDetalle(detalle.getProducto(), detalle.getNivelBom());
        BigDecimal netoActual = Optional.ofNullable(detalle.getRequerimientoNeto()).orElse(BigDecimal.ZERO);
        BigDecimal netoAnterior = netosAnteriores.get(clave);
        if (netoAnterior == null) {
            detalle.setTipoCambioMrp(TipoCambioMrp.NUEVO);
            return;
        }
        int comparacion = netoActual.compareTo(netoAnterior);
        if (comparacion > 0) {
            detalle.setTipoCambioMrp(TipoCambioMrp.AUMENTO);
        } else if (comparacion < 0) {
            detalle.setTipoCambioMrp(TipoCambioMrp.REDUCCION);
        } else {
            detalle.setTipoCambioMrp(TipoCambioMrp.SIN_CAMBIO);
        }
    }

    private String construirClaveDetalle(Producto producto, Integer nivelBom) {
        String productoId = producto != null && producto.getId() != null ? producto.getId().toString() : "null";
        String nivel = nivelBom != null ? nivelBom.toString() : "";
        return productoId + "#" + nivel;
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

    private BigDecimal obtenerRecepcionesProgramadas(Producto producto, LocalDate horizonteInicio, LocalDate horizonteFin) {
        if (producto == null || producto.getId() == null || horizonteInicio == null || horizonteFin == null) {
            return BigDecimal.ZERO;
        }
        LocalDate inicio = horizonteInicio.isAfter(horizonteFin) ? horizonteFin : horizonteInicio;
        LocalDate fin = horizonteFin.isAfter(horizonteInicio) ? horizonteFin : horizonteInicio;
        List<EstadoOrdenCompra> estadosAbiertos = List.of(
                EstadoOrdenCompra.CREADA,
                EstadoOrdenCompra.ENVIADA,
                EstadoOrdenCompra.PARCIALMENTE_RECIBIDA
        );
        BigDecimal pendiente = ordenCompraDetalleRepository
                .sumarCantidadPendientePorProductoYEstadoYFechas(producto.getId().longValue(), estadosAbiertos, inicio, fin);
        return pendiente != null ? pendiente : BigDecimal.ZERO;
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

    private BigDecimal calcularHorizonteSemanas(CorridaMrp corrida) {
        if (corrida == null || corrida.getHorizonteInicio() == null || corrida.getHorizonteFin() == null) {
            return null;
        }
        long dias = ChronoUnit.DAYS.between(corrida.getHorizonteInicio(), corrida.getHorizonteFin()) + 1;
        if (dias <= 0) {
            return null;
        }
        return BigDecimal.valueOf(dias)
                .divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularConsumoSemanalPromedio(BigDecimal consumoTotal, BigDecimal horizonteSemanas) {
        if (horizonteSemanas == null || horizonteSemanas.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return consumoTotal.divide(horizonteSemanas, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularSemanasCobertura(DetalleCorridaMrp detalle, BigDecimal consumoSemanalPromedio) {
        if (consumoSemanalPromedio == null || consumoSemanalPromedio.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal inventario = Optional.ofNullable(detalle.getInventarioDisponible()).orElse(BigDecimal.ZERO);
        BigDecimal recepciones = Optional.ofNullable(detalle.getRecepcionesProgramadas()).orElse(BigDecimal.ZERO);
        return inventario.add(recepciones)
                .divide(consumoSemanalPromedio, 2, RoundingMode.HALF_UP);
    }

    private void asignarCriticidad(SugerenciaAbastecimiento sugerencia, Integer leadTimeDias) {
        BigDecimal semanasCobertura = sugerencia.getSemanasCobertura();
        List<String> razones = new ArrayList<>();
        if (semanasCobertura == null) {
            sugerencia.setNivelCriticidad("ALTO");
            sugerencia.setEsCritico(false);
            sugerencia.setRazonesCriticidad(razones);
            return;
        }

        BigDecimal leadTimeSemanas = leadTimeDias != null && leadTimeDias > 0
                ? BigDecimal.valueOf(leadTimeDias).divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP)
                : null;

        if (semanasCobertura.compareTo(BigDecimal.ONE) < 0) {
            razones.add("COBERTURA_MENOR_A_1_SEMANA");
        }
        if (leadTimeSemanas != null && semanasCobertura.compareTo(leadTimeSemanas) < 0) {
            razones.add("COBERTURA_MENOR_A_LEAD_TIME");
        }

        if (!razones.isEmpty()) {
            sugerencia.setNivelCriticidad("CRITICO");
            sugerencia.setEsCritico(true);
            sugerencia.setRazonesCriticidad(razones);
            return;
        }

        if (semanasCobertura.compareTo(BigDecimal.ONE) >= 0 && semanasCobertura.compareTo(BigDecimal.valueOf(3)) < 0) {
            sugerencia.setNivelCriticidad("ALTO");
            sugerencia.setEsCritico(true);
            razones.add("COBERTURA_MENOR_A_3_SEMANAS");
            sugerencia.setRazonesCriticidad(razones);
            return;
        }

        if (semanasCobertura.compareTo(BigDecimal.valueOf(3)) >= 0 && semanasCobertura.compareTo(BigDecimal.valueOf(6)) < 0) {
            sugerencia.setNivelCriticidad("MEDIO");
            sugerencia.setEsCritico(false);
            sugerencia.setRazonesCriticidad(razones);
            return;
        }

        sugerencia.setNivelCriticidad("BAJO");
        sugerencia.setEsCritico(false);
        sugerencia.setRazonesCriticidad(razones);
    }
}
