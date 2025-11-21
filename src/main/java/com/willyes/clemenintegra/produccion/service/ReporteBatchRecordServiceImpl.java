package com.willyes.clemenintegra.produccion.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.willyes.clemenintegra.produccion.dto.BatchRecordDTO;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReporteBatchRecordServiceImpl implements ReporteBatchRecordService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BatchRecordService batchRecordService;

    @Override
    public byte[] generarPdfBatchRecord(Long ordenProduccionId) {
        BatchRecordDTO batchRecord = batchRecordService.buildByOrdenProduccion(ordenProduccionId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String html = buildHtml(batchRecord);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF de batch record para la OP {}", ordenProduccionId, e);
            throw new CustomBusinessException(ApiErrorCode.ERROR_INTERNO, "REPORTE_NO_DISPONIBLE");
        }
    }

    private String buildHtml(BatchRecordDTO batchRecord) throws IOException {
        String template = loadTemplate("templates/produccion/batch-record.ftl");

        BatchRecordDTO.OpDTO op = batchRecord.op;
        BatchRecordDTO.ProduccionFinalDTO produccionFinal = batchRecord.produccionFinal;
        BatchRecordDTO.LotePTDTO lotePT = batchRecord.loteProductoTerminado;

        template = template.replace("${op.codigoOrden}", escape(nz(op != null ? op.codigoOrden : "")))
                .replace("${op.producto}", escape(nz(op != null ? op.productoNombre : "")))
                .replace("${op.codigoSku}", escape(nz(op != null ? op.codigoSku : "")))
                .replace("${op.lote}", escape(nz(op != null ? op.loteProduccion : "")))
                .replace("${op.presentacion}", escape(nz(op != null ? op.presentacion : "")))
                .replace("${op.cantidadProgramada}", escape(formatInteger(op != null ? op.cantidadProgramada : null)))
                .replace("${op.cantidadProducida}", escape(formatInteger(op != null ? op.cantidadProducida : null)))
                .replace("${op.estado}", escape(nz(op != null ? op.estado : "")))
                .replace("${op.fechaInicio}", escape(formatDate(op != null ? op.fechaInicio : null)))
                .replace("${op.fechaFin}", escape(formatDate(op != null ? op.fechaFin : null)))
                .replace("${op.responsable}", escape(nz(op != null ? op.responsableNombre : "")))
                .replace("${op.porcentajeCumplimiento}", escape(formatDecimal(op != null ? op.porcentajeCumplimiento : null)));

        template = template.replace("${formulaRows}", buildFormulaRows(batchRecord.formula));
        template = template.replace("${consumoRows}", buildConsumoRows(batchRecord.consumos));
        template = template.replace("${reservaRows}", buildReservaRows(batchRecord.reservas));
        template = template.replace("${controlProcesoRows}", buildControlProcesoRows(batchRecord.controlesProceso));
        template = template.replace("${controlEmpaqueRows}", buildControlEmpaqueRows(batchRecord.controlesEmpaque));
        template = template.replace("${observacionRows}", buildObservacionRows(batchRecord.observacionesProceso));

        template = template.replace("${produccionFinal.unidadesProducidas}", escape(formatInteger(produccionFinal != null ? produccionFinal.unidadesProducidas : null)))
                .replace("${produccionFinal.unidadesAprobadas}", escape(formatInteger(produccionFinal != null ? produccionFinal.unidadesAprobadas : null)))
                .replace("${produccionFinal.unidadesRechazadas}", escape(formatInteger(produccionFinal != null ? produccionFinal.unidadesRechazadas : null)))
                .replace("${produccionFinal.rendimiento}", escape(formatDecimal(produccionFinal != null ? produccionFinal.rendimientoCalculado : null)));

        template = template.replace("${lotePT.codigoLote}", escape(nz(lotePT != null ? lotePT.codigoLote : "")))
                .replace("${lotePT.estado}", escape(nz(lotePT != null ? lotePT.estado : "")))
                .replace("${lotePT.fechaFabricacion}", escape(formatDate(lotePT != null ? lotePT.fechaFabricacion : null)))
                .replace("${lotePT.fechaVencimiento}", escape(formatDate(lotePT != null ? lotePT.fechaVencimiento : null)))
                .replace("${lotePT.fechaLiberacion}", escape(formatDate(lotePT != null ? lotePT.fechaLiberacion : null)))
                .replace("${lotePT.usuarioLiberador}", escape(nz(lotePT != null ? lotePT.usuarioLiberador : "")));

        template = template.replace("${evaluacionRows}", buildEvaluacionRows(batchRecord.calidad != null ? batchRecord.calidad.evaluaciones : null));
        template = template.replace("${retencionRows}", buildRetencionRows(batchRecord.calidad != null ? batchRecord.calidad.retenciones : null));
        return template;
    }

    private String buildFormulaRows(BatchRecordDTO.FormulaDTO formula) {
        StringBuilder sb = new StringBuilder();
        List<BatchRecordDTO.DetalleFormulaDTO> detalles = formula != null ? formula.detalles : null;
        if (detalles != null) {
            for (BatchRecordDTO.DetalleFormulaDTO detalle : detalles) {
                sb.append("<tr>")
                        .append(td(detalle.codigoSku))
                        .append(td(detalle.nombre))
                        .append(td(detalle.unidad))
                        .append(td(formatDecimal(detalle.cantidadNecesaria)))
                        .append(td(detalle.obligatorio ? "Sí" : "No"))
                        .append("</tr>");
            }
        }
        return sb.toString();
    }

    private String buildConsumoRows(List<BatchRecordDTO.ConsumoDTO> consumos) {
        StringBuilder sb = new StringBuilder();
        if (consumos != null) {
            for (BatchRecordDTO.ConsumoDTO consumo : consumos) {
                sb.append("<tr>")
                        .append(td(consumo.nombreProducto))
                        .append(td(consumo.codigoLote))
                        .append(td(consumo.almacenOrigen))
                        .append(td(formatDecimal(consumo.cantidad)))
                        .append(td(consumo.unidad))
                        .append(td(formatDate(consumo.fechaMovimiento)))
                        .append("</tr>");
            }
        }
        return sb.toString();
    }

    private String buildReservaRows(List<BatchRecordDTO.ReservaDTO> reservas) {
        StringBuilder sb = new StringBuilder();
        if (reservas != null) {
            for (BatchRecordDTO.ReservaDTO reserva : reservas) {
                sb.append("<tr>")
                        .append(td(reserva.codigoLote))
                        .append(td(formatDecimal(reserva.cantidadReservada)))
                        .append(td(formatDecimal(reserva.cantidadConsumida)))
                        .append(td(reserva.estado))
                        .append("</tr>");
            }
        }
        return sb.toString();
    }

    private String buildControlProcesoRows(List<BatchRecordDTO.ControlProcesoDTO> controles) {
        StringBuilder sb = new StringBuilder();
        if (controles != null) {
            for (BatchRecordDTO.ControlProcesoDTO control : controles) {
                sb.append("<tr>")
                        .append(td(control.etapa))
                        .append(td(control.parametro))
                        .append(td(control.valorMedido))
                        .append(td(control.unidad))
                        .append(td(control.cumple != null && control.cumple ? "Sí" : "No"))
                        .append(td(control.observaciones))
                        .append(td(control.evaluadoPor))
                        .append(td(formatDate(control.fechaRegistro)))
                        .append("</tr>");
            }
        }
        return sb.toString();
    }

    private String buildControlEmpaqueRows(List<BatchRecordDTO.ControlEmpaqueDTO> controles) {
        StringBuilder sb = new StringBuilder();
        if (controles != null) {
            for (BatchRecordDTO.ControlEmpaqueDTO control : controles) {
                sb.append("<tr>")
                        .append(td(control.parametro))
                        .append(td(control.valorMedido))
                        .append(td(control.unidad))
                        .append(td(control.cumple != null && control.cumple ? "Sí" : "No"))
                        .append(td(control.observaciones))
                        .append(td(control.evaluadoPor))
                        .append(td(formatDate(control.fechaRegistro)))
                        .append("</tr>");
            }
        }
        return sb.toString();
    }

    private String buildObservacionRows(List<BatchRecordDTO.ObservacionProcesoDTO> observaciones) {
        StringBuilder sb = new StringBuilder();
        if (observaciones != null) {
            for (BatchRecordDTO.ObservacionProcesoDTO obs : observaciones) {
                sb.append("<tr>")
                        .append(td(obs.tipo))
                        .append(td(obs.descripcion))
                        .append(td(obs.registradoPor))
                        .append(td(formatDate(obs.fechaRegistro)))
                        .append("</tr>");
            }
        }
        return sb.toString();
    }

    private String buildEvaluacionRows(List<BatchRecordDTO.EvaluacionDTO> evaluaciones) {
        StringBuilder sb = new StringBuilder();
        if (evaluaciones != null) {
            for (BatchRecordDTO.EvaluacionDTO eval : evaluaciones) {
                sb.append("<tr>")
                        .append(td(eval.tipoEvaluacion))
                        .append(td(eval.resultado))
                        .append(td(eval.observaciones))
                        .append(td(eval.evaluador))
                        .append(td(formatDate(eval.fechaEvaluacion)))
                        .append("</tr>");
            }
        }
        return sb.toString();
    }

    private String buildRetencionRows(List<BatchRecordDTO.RetencionDTO> retenciones) {
        StringBuilder sb = new StringBuilder();
        if (retenciones != null) {
            for (BatchRecordDTO.RetencionDTO ret : retenciones) {
                sb.append("<tr>")
                        .append(td(ret.estado))
                        .append(td(ret.motivo))
                        .append(td(formatDate(ret.fechaRetencion)))
                        .append(td(formatDate(ret.fechaLiberacion)))
                        .append(td(ret.aprobador))
                        .append("</tr>");
            }
        }
        return sb.toString();
    }

    private String loadTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (var inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }

    private String td(String value) {
        return "<td>" + escape(nz(value)) + "</td>";
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }

    private String escape(String value) {
        String sanitized = nz(value);
        String normalized = java.text.Normalizer.normalize(sanitized, java.text.Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");
        return normalized
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatDecimal(java.math.BigDecimal value) {
        if (value == null) return "";
        return String.format(Locale.US, "%.2f", value);
    }

    private String formatInteger(Integer value) {
        return value == null ? "" : value.toString();
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }
}
