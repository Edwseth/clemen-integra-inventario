package com.willyes.clemenintegra.gerencial.service;

import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.gerencial.dto.SeguimientoGerencialResponseDTO;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraDetalleRepository;
import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.DetalleCorridaMrp;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionDetalle;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoSugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.repository.CorridaMrpRepository;
import com.willyes.clemenintegra.planeacion.repository.PlanProduccionSemanalRepository;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoBatchRecord;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
/**
 * Servicio agregador gerencial (V1).
 *
 * <p>Diseño de construcción:
 * <ul>
 *   <li>{@code items[]} se construye 1:1 desde {@link PlanProduccionDetalle} del plan semanal.</li>
 *   <li>{@code summary} se deriva exclusivamente de los {@code items[]} calculados.</li>
 *   <li>Consolida señales de Planeación, BOM, MRP, Compras, Producción, Inventario y Calidad.</li>
 * </ul>
 *
 * <p>Limitación explícita V1:
 * la trazabilidad exacta por {@code plan_detalle_id} existe en Producción (OP/lotes vinculados),
 * pero MRP y Compras/Recepciones operan como señales derivadas por producto. Por tanto,
 * esas señales no representan correspondencia exacta por ítem cuando un mismo producto aparece
 * en múltiples detalles del plan.
 */
public class SeguimientoGerencialServiceImpl implements SeguimientoGerencialService {

    private static final List<EstadoProduccion> ESTADOS_OP_CERRADOS = List.of(
            EstadoProduccion.FINALIZADA,
            EstadoProduccion.CERRADA_INCOMPLETA,
            EstadoProduccion.CANCELADA
    );

    private final PlanProduccionSemanalRepository planProduccionSemanalRepository;
    private final OrdenProduccionRepository ordenProduccionRepository;
    private final FormulaProductoRepository formulaProductoRepository;
    private final CorridaMrpRepository corridaMrpRepository;
    private final OrdenCompraDetalleRepository ordenCompraDetalleRepository;
    private final LoteProductoRepository loteProductoRepository;

    @Override
    /**
     * Orquesta la respuesta consolidada de seguimiento gerencial para un plan semanal.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Carga plan + detalles.</li>
     *   <li>Resuelve trazabilidad base por detalle hacia OP/lotes.</li>
     *   <li>Calcula contexto semántico por ítem (estado, etapa, bloqueo, responsable, métricas).</li>
     *   <li>Construye {@code summary} como agregado de {@code items[]}.</li>
     * </ol>
     */
    public SeguimientoGerencialResponseDTO obtenerSeguimiento(Long planSemanalId) {
        PlanProduccionSemanal plan = planProduccionSemanalRepository.findWithDetallesById(planSemanalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PLAN_SEMANAL_NO_ENCONTRADO"));

        List<PlanProduccionDetalle> detalles = Optional.ofNullable(plan.getDetalles()).orElse(List.of());
        Set<Long> planDetalleIds = detalles.stream().map(PlanProduccionDetalle::getId).filter(Objects::nonNull).collect(Collectors.toSet());

        List<OrdenProduccion> ordenes = planDetalleIds.isEmpty()
                ? List.of()
                : ordenProduccionRepository.findByPlanProduccionDetalleIdIn(planDetalleIds);

        Map<Long, List<OrdenProduccion>> ordenesPorDetalle = ordenes.stream()
                .filter(op -> op.getPlanProduccionDetalle() != null && op.getPlanProduccionDetalle().getId() != null)
                .collect(Collectors.groupingBy(op -> op.getPlanProduccionDetalle().getId()));

        Set<Long> productoIds = detalles.stream()
                .map(d -> d.getProducto() != null && d.getProducto().getId() != null ? d.getProducto().getId().longValue() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Long> detallesPorProducto = detalles.stream()
                .map(d -> d.getProducto() != null && d.getProducto().getId() != null ? d.getProducto().getId().longValue() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        Map<Long, FormulaProducto> formulaPorProducto = formulaProductoRepository
                .findByProductoIdInAndEstadoAndActivoTrue(productoIds, EstadoFormula.APROBADA)
                .stream()
                .collect(Collectors.toMap(f -> f.getProducto().getId().longValue(), Function.identity(), (a, b) -> a));

        Optional<CorridaMrp> corridaOpt = corridaMrpRepository
                .findTopByPlanProduccionSemanalAndEstadoOrderByFechaEjecucionDesc(
                        plan,
                        com.willyes.clemenintegra.planeacion.model.enums.EstadoCorridaMrp.COMPLETADA
                );

        Map<Long, Long> sugerenciasPendientesPorProducto = new HashMap<>();
        corridaOpt.ifPresent(corrida -> {
            for (DetalleCorridaMrp detalleCorrida : Optional.ofNullable(corrida.getDetalles()).orElse(List.of())) {
                if (detalleCorrida.getProducto() == null || detalleCorrida.getProducto().getId() == null) {
                    continue;
                }
                if (detalleCorrida.getSugerencia() == null) {
                    continue;
                }
                if (detalleCorrida.getSugerencia().getEstado() == EstadoSugerenciaAbastecimiento.PENDIENTE
                        && Optional.ofNullable(detalleCorrida.getSugerencia().getCantidadSugerida()).orElse(BigDecimal.ZERO)
                        .compareTo(BigDecimal.ZERO) > 0) {
                    Long productoId = detalleCorrida.getProducto().getId().longValue();
                    sugerenciasPendientesPorProducto.merge(productoId, 1L, Long::sum);
                }
            }
        });

        Set<Long> opIds = ordenes.stream().map(OrdenProduccion::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, List<LoteProducto>> lotesPorOp = opIds.isEmpty()
                ? Map.of()
                : loteProductoRepository.findByOrdenProduccionIdIn(opIds).stream()
                .filter(l -> l.getOrdenProduccion() != null && l.getOrdenProduccion().getId() != null)
                .collect(Collectors.groupingBy(l -> l.getOrdenProduccion().getId()));

        LocalDate hoy = LocalDate.now();
        List<SeguimientoGerencialResponseDTO.ItemDTO> items = new ArrayList<>();
        for (PlanProduccionDetalle detalle : detalles) {
            Long detalleId = detalle.getId();
            List<OrdenProduccion> opsDetalle = detalleId != null ? ordenesPorDetalle.getOrDefault(detalleId, List.of()) : List.of();
            ItemContext context = construirContexto(plan, detalle, opsDetalle, formulaPorProducto, sugerenciasPendientesPorProducto,
                    detallesPorProducto, lotesPorOp, hoy, corridaOpt.orElse(null));
            items.add(construirItem(plan, detalle, context));
        }

        return SeguimientoGerencialResponseDTO.builder()
                .summary(construirSummary(plan, items))
                .items(items)
                .build();
    }

    private ItemContext construirContexto(PlanProduccionSemanal plan,
                                          PlanProduccionDetalle detalle,
                                          List<OrdenProduccion> opsDetalle,
                                          Map<Long, FormulaProducto> formulaPorProducto,
                                          Map<Long, Long> sugerenciasPendientesPorProducto,
                                          Map<Long, Long> detallesPorProducto,
                                          Map<Long, List<LoteProducto>> lotesPorOp,
                                          LocalDate hoy,
                                          CorridaMrp corridaMrp) {

        Long productoId = detalle.getProducto() != null && detalle.getProducto().getId() != null
                ? detalle.getProducto().getId().longValue()
                : null;

        FormulaProducto formula = productoId != null ? formulaPorProducto.get(productoId) : null;
        boolean sinFormulaAprobada = formula == null;

        long sugerenciasPendientes = productoId != null ? sugerenciasPendientesPorProducto.getOrDefault(productoId, 0L) : 0L;
        boolean senalDerivadaMrpPendiente = sugerenciasPendientes > 0;

        BigDecimal pendientesRecepcion = BigDecimal.ZERO;
        if (productoId != null) {
            pendientesRecepcion = Optional.ofNullable(ordenCompraDetalleRepository.sumarCantidadPendientePorProductoYEstados(
                    productoId,
                    List.of(EstadoOrdenCompra.ENVIADA, EstadoOrdenCompra.PARCIALMENTE_RECIBIDA)
            )).orElse(BigDecimal.ZERO);
        }
        boolean senalDerivadaRecepcionPendiente = pendientesRecepcion.compareTo(BigDecimal.ZERO) > 0;
        boolean senalesDerivadasConfiables = productoId != null && detallesPorProducto.getOrDefault(productoId, 0L) == 1;

        boolean batchRechazado = opsDetalle.stream().anyMatch(op -> op.getBatchRecordEstado() == EstadoBatchRecord.RECHAZADO);

        boolean opRetrasada = opsDetalle.stream()
                .anyMatch(op -> op.getEstado() != null
                        && !ESTADOS_OP_CERRADOS.contains(op.getEstado())
                        && op.getFechaFin() != null
                        && op.getFechaFin().toLocalDate().isBefore(hoy));

        List<LoteProducto> lotes = opsDetalle.stream()
                .map(OrdenProduccion::getId)
                .filter(Objects::nonNull)
                .flatMap(opId -> lotesPorOp.getOrDefault(opId, List.of()).stream())
                .toList();

        boolean loteBloqueadoCalidad = lotes.stream().anyMatch(l -> l.getEstado() == EstadoLote.EN_CUARENTENA
                || l.getEstado() == EstadoLote.RETENIDO
                || l.getEstado() == EstadoLote.RECHAZADO);

        boolean loteFinalUtilizable = lotes.stream().anyMatch(l -> l.getEstado() == EstadoLote.DISPONIBLE
                || l.getEstado() == EstadoLote.LIBERADO);

        int opGeneradas = opsDetalle.size();
        int opCerradas = (int) opsDetalle.stream().filter(op -> op.getEstado() != null && ESTADOS_OP_CERRADOS.contains(op.getEstado())).count();

        // cantidadEjecutada:
        // suma por ítem de OP, priorizando cantidadProducidaAcumulada; si no existe, usa cantidadProducida.
        BigDecimal cantidadEjecutada = opsDetalle.stream()
                .map(op -> op.getCantidadProducidaAcumulada() != null ? op.getCantidadProducidaAcumulada()
                        : Optional.ofNullable(op.getCantidadProducida()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal porcentajeCumplimiento = calcularPorcentaje(cantidadEjecutada, detalle.getCantidadPlanificada());

        String etapaActual = calcularEtapaActual(plan, sinFormulaAprobada, senalDerivadaMrpPendiente,
                senalDerivadaRecepcionPendiente, senalesDerivadasConfiables, loteBloqueadoCalidad, opGeneradas,
                opCerradas, loteFinalUtilizable, opsDetalle);

        SeguimientoGerencialResponseDTO.BloqueoPrincipalDTO bloqueo = calcularBloqueoPrincipal(sinFormulaAprobada,
                senalDerivadaMrpPendiente, senalDerivadaRecepcionPendiente, loteBloqueadoCalidad, opRetrasada, batchRechazado);

        boolean enVentanaCriticaSinOp = opGeneradas == 0 && plan.getSemanaFin() != null
                && !plan.getSemanaFin().isBefore(hoy)
                && !plan.getSemanaFin().isAfter(hoy.plusDays(2));

        String estadoGerencial = calcularEstadoGerencial(
                bloqueo,
                opsDetalle,
                batchRechazado,
                cantidadEjecutada,
                detalle.getCantidadPlanificada(),
                loteFinalUtilizable,
                enVentanaCriticaSinOp
        );

        List<SeguimientoGerencialResponseDTO.AlertaDTO> alertas = construirAlertas(sinFormulaAprobada, senalDerivadaMrpPendiente,
                senalDerivadaRecepcionPendiente, loteBloqueadoCalidad, opRetrasada, batchRechazado, opGeneradas == 0, enVentanaCriticaSinOp);

        SeguimientoGerencialResponseDTO.ResponsableActualDTO responsableActual = calcularResponsableActual(bloqueo, opsDetalle, detalle, opRetrasada);

        return new ItemContext(formula, corridaMrp, sugerenciasPendientes, etapaActual, bloqueo, responsableActual,
                opGeneradas, opCerradas, cantidadEjecutada, porcentajeCumplimiento, estadoGerencial, alertas,
                loteFinalUtilizable, opsDetalle, lotes);
    }

    private SeguimientoGerencialResponseDTO.ItemDTO construirItem(PlanProduccionSemanal plan,
                                                                  PlanProduccionDetalle detalle,
                                                                  ItemContext context) {
        return SeguimientoGerencialResponseDTO.ItemDTO.builder()
                .planDetalleId(detalle.getId())
                .planSemanalId(plan.getId())
                .productoId(detalle.getProducto() != null && detalle.getProducto().getId() != null ? detalle.getProducto().getId().longValue() : null)
                .sku(detalle.getProducto() != null ? detalle.getProducto().getCodigoSku() : null)
                .nombreProducto(detalle.getProducto() != null ? detalle.getProducto().getNombre() : null)
                .cantidadPlanificada(detalle.getCantidadPlanificada())
                .unidadMedida(detalle.getUnidadMedida() != null && detalle.getUnidadMedida().getSimbolo() != null
                        ? detalle.getUnidadMedida().getSimbolo()
                        : detalle.getUnidadMedida() != null ? detalle.getUnidadMedida().getNombre() : null)
                .prioridad(detalle.getPrioridad())
                .estadoGerencial(context.estadoGerencial)
                .etapaActual(context.etapaActual)
                .bloqueoPrincipal(context.bloqueoPrincipal)
                .responsableActual(context.responsableActual)
                .opGeneradas(context.opGeneradas)
                .opCerradas(context.opCerradas)
                .cantidadEjecutada(context.cantidadEjecutada)
                .porcentajeCumplimiento(context.porcentajeCumplimiento)
                .requiereIntervencion(requiereIntervencion(context.estadoGerencial, context.alertas))
                .alertas(context.alertas)
                .metadatosTrazabilidad(construirMetadatos(context))
                .build();
    }

    private SeguimientoGerencialResponseDTO.MetadatosTrazabilidadDTO construirMetadatos(ItemContext context) {
        List<Long> opIds = context.opsDetalle.stream().map(OrdenProduccion::getId).filter(Objects::nonNull).toList();
        List<Long> loteIds = context.lotes.stream().map(LoteProducto::getId).filter(Objects::nonNull).distinct().toList();
        List<String> batchEstados = context.opsDetalle.stream()
                .map(OrdenProduccion::getBatchRecordEstado)
                .filter(Objects::nonNull)
                .map(Enum::name)
                .distinct()
                .toList();

        return SeguimientoGerencialResponseDTO.MetadatosTrazabilidadDTO.builder()
                .formula(SeguimientoGerencialResponseDTO.FormulaDTO.builder()
                        .formulaId(context.formula != null ? context.formula.getId() : null)
                        .estado(context.formula != null && context.formula.getEstado() != null ? context.formula.getEstado().name() : null)
                        .version(context.formula != null ? context.formula.getVersion() : null)
                        .build())
                .mrp(SeguimientoGerencialResponseDTO.MrpDTO.builder()
                        .corridaMrpId(context.corridaMrp != null ? context.corridaMrp.getId() : null)
                        .estadoCorrida(context.corridaMrp != null && context.corridaMrp.getEstado() != null ? context.corridaMrp.getEstado().name() : null)
                        .sugerenciasPendientes((int) context.sugerenciasPendientes)
                        .build())
                .operacion(SeguimientoGerencialResponseDTO.OperacionDTO.builder()
                        .opIds(opIds)
                        .loteIds(loteIds)
                        .batchRecordEstados(batchEstados)
                        .build())
                .build();
    }

    private SeguimientoGerencialResponseDTO.SummaryDTO construirSummary(PlanProduccionSemanal plan,
                                                                        List<SeguimientoGerencialResponseDTO.ItemDTO> items) {
        int totalItems = items.size();
        int noIniciados = contar(items, "NO_INICIADO");
        int enProceso = contar(items, "EN_PROCESO");
        int enRiesgo = contar(items, "EN_RIESGO");
        int bloqueados = contar(items, "BLOQUEADO");
        int completados = contar(items, "COMPLETADO");
        int cerradosConNovedad = contar(items, "CERRADO_CON_NOVEDAD");

        int totalOpGeneradas = items.stream().mapToInt(SeguimientoGerencialResponseDTO.ItemDTO::getOpGeneradas).sum();
        int totalOpCerradas = items.stream().mapToInt(SeguimientoGerencialResponseDTO.ItemDTO::getOpCerradas).sum();
        int totalAlertas = items.stream().mapToInt(i -> i.getAlertas() != null ? i.getAlertas().size() : 0).sum();
        int itemsConIntervencion = (int) items.stream().filter(SeguimientoGerencialResponseDTO.ItemDTO::isRequiereIntervencion).count();

        BigDecimal planificada = items.stream().map(SeguimientoGerencialResponseDTO.ItemDTO::getCantidadPlanificada)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ejecutada = items.stream().map(SeguimientoGerencialResponseDTO.ItemDTO::getCantidadEjecutada)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        return SeguimientoGerencialResponseDTO.SummaryDTO.builder()
                .planSemanalId(plan.getId())
                .semanaInicio(plan.getSemanaInicio())
                .semanaFin(plan.getSemanaFin())
                .estadoPlan(plan.getEstado() != null ? plan.getEstado().name() : null)
                .totalItems(totalItems)
                .itemsNoIniciados(noIniciados)
                .itemsEnProceso(enProceso)
                .itemsEnRiesgo(enRiesgo)
                .itemsBloqueados(bloqueados)
                .itemsCompletados(completados)
                .itemsCerradosConNovedad(cerradosConNovedad)
                .porcentajeCumplimientoGeneral(calcularPorcentaje(ejecutada, planificada))
                .totalOpGeneradas(totalOpGeneradas)
                .totalOpCerradas(totalOpCerradas)
                .totalAlertas(totalAlertas)
                .totalItemsConIntervencionRequerida(itemsConIntervencion)
                .build();
    }

    private int contar(List<SeguimientoGerencialResponseDTO.ItemDTO> items, String estado) {
        return (int) items.stream().filter(i -> estado.equals(i.getEstadoGerencial())).count();
    }

    private BigDecimal calcularPorcentaje(BigDecimal numerador, BigDecimal denominador) {
        // porcentajeCumplimiento:
        // (numerador / denominador) * 100 con escala 2 y HALF_UP.
        // Si denominador <= 0 o nulo, retorna 0.00 para evitar divisiones inválidas.
        if (numerador == null || denominador == null || denominador.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return numerador.multiply(BigDecimal.valueOf(100))
                .divide(denominador, 2, RoundingMode.HALF_UP);
    }

    private String calcularEtapaActual(PlanProduccionSemanal plan,
                                       boolean sinFormulaAprobada,
                                       boolean senalDerivadaMrpPendiente,
                                       boolean senalDerivadaRecepcionPendiente,
                                       boolean senalesDerivadasConfiables,
                                       boolean loteBloqueadoCalidad,
                                       int opGeneradas,
                                       int opCerradas,
                                       boolean loteFinalUtilizable,
                                       List<OrdenProduccion> opsDetalle) {
        // Regla semántica de etapaActual (orden de precedencia):
        // 1) PLAN en BORRADOR -> PLANEACION.
        // 2) Sin fórmula aprobada -> BOM_FORMULA.
        // 3) Sin OP generadas -> se usa señal derivada confiable por producto para ubicar
        //    en ABASTECIMIENTO_COMPRAS o RECEPCION_INVENTARIO; en otro caso PRODUCCION.
        // 4) Con OP activas o sin cierre total -> PRODUCCION.
        // 5) Con lotes bloqueados por calidad -> CALIDAD.
        // 6) Con OP cerradas -> LIBERACION_FINAL (con/sin lote final utilizable).
        if (plan.getEstado() == null || plan.getEstado().name().equals("BORRADOR")) {
            return "PLANEACION";
        }
        if (sinFormulaAprobada) {
            return "BOM_FORMULA";
        }
        if (opGeneradas == 0) {
            if (senalesDerivadasConfiables && senalDerivadaMrpPendiente) {
                return "ABASTECIMIENTO_COMPRAS";
            }
            if (senalesDerivadasConfiables && senalDerivadaRecepcionPendiente) {
                return "RECEPCION_INVENTARIO";
            }
            return "PRODUCCION";
        }

        boolean hayOpActiva = opsDetalle.stream().anyMatch(op -> op.getEstado() == EstadoProduccion.EN_PROCESO
                || op.getEstado() == EstadoProduccion.CREADA
                || op.getEstado() == null);
        if (hayOpActiva || (opGeneradas > 0 && opCerradas < opGeneradas)) {
            return "PRODUCCION";
        }
        if (loteBloqueadoCalidad) {
            return "CALIDAD";
        }
        if (opGeneradas > 0 && opCerradas == opGeneradas && !loteFinalUtilizable) {
            return "LIBERACION_FINAL";
        }
        if (opGeneradas > 0) {
            return "LIBERACION_FINAL";
        }
        return "PRODUCCION";
    }

    private SeguimientoGerencialResponseDTO.BloqueoPrincipalDTO calcularBloqueoPrincipal(boolean sinFormulaAprobada,
                                                                                          boolean senalDerivadaMrpPendiente,
                                                                                          boolean senalDerivadaRecepcionPendiente,
                                                                                          boolean loteBloqueadoCalidad,
                                                                                          boolean opRetrasada,
                                                                                          boolean batchRechazado) {
        // Regla semántica de bloqueoPrincipal:
        // se retorna el primer bloqueo aplicable por severidad de negocio en este orden.
        if (sinFormulaAprobada) {
            return bloqueo("SIN_FORMULA_APROBADA", "No existe fórmula aprobada activa para el producto");
        }
        if (senalDerivadaMrpPendiente) {
            return bloqueo("ABASTECIMIENTO_PENDIENTE_CRITICO", "Hay señal derivada MRP pendiente por producto para abastecimiento");
        }
        if (senalDerivadaRecepcionPendiente) {
            return bloqueo("RECEPCION_INSUFICIENTE", "Hay señal derivada de compras/recepción pendiente por producto");
        }
        if (loteBloqueadoCalidad) {
            return bloqueo("LOTE_BLOQUEADO_CALIDAD", "Hay lotes en cuarentena, retenidos o rechazados");
        }
        if (opRetrasada) {
            return bloqueo("OP_RETRASADA", "Hay órdenes de producción vencidas sin cierre");
        }
        if (batchRechazado) {
            return bloqueo("BATCH_RECHAZADO", "Existe al menos un batch record rechazado");
        }
        return bloqueo("NONE", null);
    }

    private SeguimientoGerencialResponseDTO.BloqueoPrincipalDTO bloqueo(String codigo, String mensaje) {
        return SeguimientoGerencialResponseDTO.BloqueoPrincipalDTO.builder().codigo(codigo).mensaje(mensaje).build();
    }

    private String calcularEstadoGerencial(SeguimientoGerencialResponseDTO.BloqueoPrincipalDTO bloqueo,
                                           List<OrdenProduccion> opsDetalle,
                                           boolean batchRechazado,
                                           BigDecimal cantidadEjecutada,
                                           BigDecimal cantidadPlanificada,
                                           boolean loteFinalUtilizable,
                                           boolean enVentanaCriticaSinOp) {
        // Regla semántica de estadoGerencial (precedencia):
        // BLOQUEADO > CERRADO_CON_NOVEDAD > COMPLETADO > EN_RIESGO > EN_PROCESO > NO_INICIADO.

        boolean bloqueado = bloqueo != null && bloqueo.getCodigo() != null && !"NONE".equals(bloqueo.getCodigo());
        if (bloqueado) {
            return "BLOQUEADO";
        }

        boolean cerradaConNovedad = batchRechazado
                || opsDetalle.stream().anyMatch(op -> op.getEstado() == EstadoProduccion.CERRADA_INCOMPLETA);
        if (cerradaConNovedad) {
            return "CERRADO_CON_NOVEDAD";
        }

        BigDecimal planificadaSafe = Optional.ofNullable(cantidadPlanificada).orElse(BigDecimal.ZERO);
        BigDecimal ejecutadaSafe = Optional.ofNullable(cantidadEjecutada).orElse(BigDecimal.ZERO);
        if (planificadaSafe.compareTo(BigDecimal.ZERO) > 0
                && ejecutadaSafe.compareTo(planificadaSafe) >= 0
                && loteFinalUtilizable) {
            return "COMPLETADO";
        }

        if (opsDetalle.isEmpty() && enVentanaCriticaSinOp) {
            return "EN_RIESGO";
        }

        boolean enProceso = !opsDetalle.isEmpty() && opsDetalle.stream().anyMatch(op ->
                op.getEstado() == EstadoProduccion.EN_PROCESO
                        || op.getEstado() == EstadoProduccion.CREADA);
        if (enProceso) {
            return "EN_PROCESO";
        }

        if (!opsDetalle.isEmpty()) {
            return "EN_PROCESO";
        }

        return "NO_INICIADO";
    }

    private List<SeguimientoGerencialResponseDTO.AlertaDTO> construirAlertas(boolean sinFormulaAprobada,
                                                                              boolean senalDerivadaMrpPendiente,
                                                                              boolean senalDerivadaRecepcionPendiente,
                                                                              boolean loteBloqueadoCalidad,
                                                                              boolean opRetrasada,
                                                                              boolean batchRechazado,
                                                                              boolean sinOp,
                                                                              boolean enVentanaCriticaSinOp) {
        List<SeguimientoGerencialResponseDTO.AlertaDTO> alertas = new ArrayList<>();
        if (sinFormulaAprobada) {
            alertas.add(alerta("SIN_FORMULA_APROBADA", "ALTA", "BOM", "Falta fórmula aprobada activa"));
        }
        if (senalDerivadaMrpPendiente) {
            alertas.add(alerta("ABASTECIMIENTO_PENDIENTE", "MEDIA", "MRP", "Hay señal derivada MRP pendiente por producto"));
        }
        if (senalDerivadaRecepcionPendiente) {
            alertas.add(alerta("RECEPCION_INSUFICIENTE", "MEDIA", "COMPRAS", "Hay señal derivada de compras pendientes por producto"));
        }
        if (loteBloqueadoCalidad) {
            alertas.add(alerta("LOTE_BLOQUEADO_CALIDAD", "ALTA", "CALIDAD", "Existe lote bloqueado por calidad"));
        }
        if (opRetrasada) {
            alertas.add(alerta("OP_RETRASADA", "ALTA", "PRODUCCION", "Existe OP retrasada"));
        }
        if (batchRechazado) {
            alertas.add(alerta("BATCH_RECHAZADO", "ALTA", "CALIDAD", "Batch record rechazado"));
        }
        if (sinOp && enVentanaCriticaSinOp) {
            alertas.add(alerta("SIN_OP_VENTANA_CRITICA", "MEDIA", "PLANEACION", "Ítem sin OP dentro de ventana crítica"));
        }
        return alertas;
    }

    private SeguimientoGerencialResponseDTO.AlertaDTO alerta(String codigo, String severidad, String origen, String mensaje) {
        return SeguimientoGerencialResponseDTO.AlertaDTO.builder()
                .codigo(codigo)
                .severidad(severidad)
                .origen(origen)
                .mensaje(mensaje)
                .build();
    }

    private SeguimientoGerencialResponseDTO.ResponsableActualDTO calcularResponsableActual(
            SeguimientoGerencialResponseDTO.BloqueoPrincipalDTO bloqueo,
            List<OrdenProduccion> opsDetalle,
            PlanProduccionDetalle detalle,
            boolean opRetrasada) {
        // Regla semántica de responsableActual:
        // 1) Si existe bloqueo funcional, se asigna área responsable por tipo de bloqueo.
        // 2) Si hay OP y responsable de OP, se informa ese usuario.
        // 3) Fallback operativo: área Producción o Planeación según contexto.
        String codigoBloqueo = bloqueo != null ? bloqueo.getCodigo() : "NONE";
        if ("SIN_FORMULA_APROBADA".equals(codigoBloqueo)) {
            return area("BOM / Desarrollo");
        }
        if ("ABASTECIMIENTO_PENDIENTE_CRITICO".equals(codigoBloqueo)) {
            return area("Compras");
        }
        if ("RECEPCION_INSUFICIENTE".equals(codigoBloqueo)) {
            return area("Inventarios");
        }
        if ("LOTE_BLOQUEADO_CALIDAD".equals(codigoBloqueo) || "BATCH_RECHAZADO".equals(codigoBloqueo)) {
            return area("Calidad");
        }

        if (!opsDetalle.isEmpty()) {
            Optional<OrdenProduccion> conResponsable = opsDetalle.stream()
                    .filter(op -> op.getResponsable() != null && op.getResponsable().getId() != null)
                    .findFirst();
            if (conResponsable.isPresent()) {
                OrdenProduccion op = conResponsable.get();
                return SeguimientoGerencialResponseDTO.ResponsableActualDTO.builder()
                        .tipo("USUARIO")
                        .id(op.getResponsable().getId())
                        .nombre(op.getResponsable().getNombreCompleto())
                        .build();
            }
            if (opRetrasada) {
                return area("Producción");
            }
        }

        if (detalle.getPlan() != null && detalle.getPlan().getEstado() != null && detalle.getPlan().getEstado().name().equals("BORRADOR")) {
            return area("Planeación");
        }
        return area("Planeación");
    }

    private SeguimientoGerencialResponseDTO.ResponsableActualDTO area(String nombre) {
        return SeguimientoGerencialResponseDTO.ResponsableActualDTO.builder()
                .tipo("AREA")
                .id(null)
                .nombre(nombre)
                .build();
    }

    private boolean requiereIntervencion(String estadoGerencial, List<SeguimientoGerencialResponseDTO.AlertaDTO> alertas) {
        if ("BLOQUEADO".equals(estadoGerencial)
                || "EN_RIESGO".equals(estadoGerencial)
                || "CERRADO_CON_NOVEDAD".equals(estadoGerencial)) {
            return true;
        }
        return alertas != null && alertas.stream().anyMatch(a -> "ALTA".equals(a.getSeveridad()));
    }

    private static class ItemContext {
        private final FormulaProducto formula;
        private final CorridaMrp corridaMrp;
        private final long sugerenciasPendientes;
        private final String etapaActual;
        private final SeguimientoGerencialResponseDTO.BloqueoPrincipalDTO bloqueoPrincipal;
        private final SeguimientoGerencialResponseDTO.ResponsableActualDTO responsableActual;
        private final int opGeneradas;
        private final int opCerradas;
        private final BigDecimal cantidadEjecutada;
        private final BigDecimal porcentajeCumplimiento;
        private final String estadoGerencial;
        private final List<SeguimientoGerencialResponseDTO.AlertaDTO> alertas;
        private final boolean loteFinalUtilizable;
        private final List<OrdenProduccion> opsDetalle;
        private final List<LoteProducto> lotes;

        private ItemContext(FormulaProducto formula,
                            CorridaMrp corridaMrp,
                            long sugerenciasPendientes,
                            String etapaActual,
                            SeguimientoGerencialResponseDTO.BloqueoPrincipalDTO bloqueoPrincipal,
                            SeguimientoGerencialResponseDTO.ResponsableActualDTO responsableActual,
                            int opGeneradas,
                            int opCerradas,
                            BigDecimal cantidadEjecutada,
                            BigDecimal porcentajeCumplimiento,
                            String estadoGerencial,
                            List<SeguimientoGerencialResponseDTO.AlertaDTO> alertas,
                            boolean loteFinalUtilizable,
                            List<OrdenProduccion> opsDetalle,
                            List<LoteProducto> lotes) {
            this.formula = formula;
            this.corridaMrp = corridaMrp;
            this.sugerenciasPendientes = sugerenciasPendientes;
            this.etapaActual = etapaActual;
            this.bloqueoPrincipal = bloqueoPrincipal;
            this.responsableActual = responsableActual;
            this.opGeneradas = opGeneradas;
            this.opCerradas = opCerradas;
            this.cantidadEjecutada = cantidadEjecutada;
            this.porcentajeCumplimiento = porcentajeCumplimiento;
            this.estadoGerencial = estadoGerencial;
            this.alertas = alertas;
            this.loteFinalUtilizable = loteFinalUtilizable;
            this.opsDetalle = opsDetalle;
            this.lotes = lotes;
        }
    }
}
