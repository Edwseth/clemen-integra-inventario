package com.willyes.clemenintegra.calidad.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.ParametroAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.PlantillaAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.ResultadoAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.repository.ResultadoAnalisisMicrobiologicoRepository;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class AnalisisMicroPdfService {

    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EvaluacionCalidadRepository evaluacionRepository;
    private final ResultadoAnalisisMicrobiologicoRepository resultadoRepository;

    public AnalisisMicroPdfService(EvaluacionCalidadRepository evaluacionRepository,
                                   ResultadoAnalisisMicrobiologicoRepository resultadoRepository) {
        this.evaluacionRepository = evaluacionRepository;
        this.resultadoRepository = resultadoRepository;
    }

    public byte[] generarPdf(Long evaluacionId) {
        EvaluacionCalidad evaluacion = evaluacionRepository.findById(evaluacionId)
                .orElseThrow(() -> new NoSuchElementException("Evaluación no encontrada con ID: " + evaluacionId));

        List<ResultadoAnalisisMicrobiologico> resultados = resultadoRepository.findByEvaluacionId(evaluacionId);
        PlantillaAnalisisMicrobiologico plantilla = Optional.ofNullable(evaluacion.getLoteProducto())
                .map(l -> l.getProducto().getPlantillaAnalisisMicrobiologico())
                .orElse(null);

        String html = construirHtml(evaluacion, resultados, plantilla);
        return renderPdf(html);
    }

    String construirHtml(EvaluacionCalidad evaluacion, List<ResultadoAnalisisMicrobiologico> resultados,
                         PlantillaAnalisisMicrobiologico plantilla) {
        String html = loadTemplate("templates/calidad/informe-micro.ftl");

        html = html.replace("${codigoProducto}", esc(Optional.ofNullable(evaluacion.getLoteProducto())
                        .map(l -> l.getProducto().getCodigoSku()).orElse("")))
                .replace("${descripcionProducto}", esc(Optional.ofNullable(evaluacion.getLoteProducto())
                        .map(l -> l.getProducto().getNombre()).orElse("")))
                .replace("${lote}", esc(Optional.ofNullable(evaluacion.getLoteProducto())
                        .map(LoteProducto::getCodigoLote).orElse("")))
                .replace("${fechaResultado}", esc(formatearFecha(evaluacion)))
                .replace("${fechaMuestra}", "")
                .replace("${cantidadInspeccionada}", "")
                .replace("${opOc}", "")
                .replace("${observaciones}", esc(obtenerObservaciones(evaluacion)))
                .replace("${conclusiones}", esc(obtenerConclusiones(evaluacion)))
                .replace("${nombreElabora}", esc(obtenerNombreElabora(evaluacion)))
                .replace("${fechaEvaluacion}", esc(formatearFecha(evaluacion)));

        html = html.replace("${tablaResultados}", construirTabla(resultados, plantilla));
        return html;
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF de análisis microbiológico", e);
        }
    }

    private String construirTabla(List<ResultadoAnalisisMicrobiologico> resultados,
                                  PlantillaAnalisisMicrobiologico plantilla) {
        Map<Long, ResultadoAnalisisMicrobiologico> index = new HashMap<>();
        resultados.forEach(r -> index.put(r.getParametro().getId(), r));

        StringBuilder sb = new StringBuilder();
        List<ParametroAnalisisMicrobiologico> parametros = plantilla != null ? plantilla.getParametros() : List.of();
        parametros.stream()
                .sorted(Comparator.comparing(ParametroAnalisisMicrobiologico::getOrden, Comparator.nullsLast(Integer::compareTo)))
                .forEach(p -> {
                    ResultadoAnalisisMicrobiologico res = index.get(p.getId());
                    sb.append("<tr>")
                            .append("<td>").append(esc(p.getNombreEnsayo())).append("</td>")
                            .append("<td>").append(esc(p.getMetodo())).append("</td>")
                            .append("<td>").append(esc(p.getEspecificacion())).append("</td>")
                            .append("<td>").append(esc(formatearResultado(p, res))).append("</td>")
                            .append("</tr>");
                });
        return sb.toString();
    }

    private String esc(String val) {
        return val == null ? "" : val.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String formatearFecha(EvaluacionCalidad evaluacion) {
        return evaluacion.getFechaEvaluacion() != null ? evaluacion.getFechaEvaluacion().format(FECHA_FORMATTER) : "";
    }

    private String formatearResultado(ParametroAnalisisMicrobiologico parametro, ResultadoAnalisisMicrobiologico resultado) {
        if (resultado == null || resultado.getResultado() == null) {
            return "";
        }
        if (parametro != null && parametro.getTipoResultado() == com.willyes.clemenintegra.calidad.model.enums.TipoResultadoAnalisis.NUMERICO) {
            String unidad = Optional.ofNullable(parametro.getUnidad()).orElse("");
            return (resultado.getResultado() + (unidad.isBlank() ? "" : " " + unidad)).trim();
        }
        return resultado.getResultado();
    }

    private String obtenerObservaciones(EvaluacionCalidad evaluacion) {
        if (evaluacion.getObservaciones() == null || evaluacion.getObservaciones().isBlank()) {
            return "Sin observaciones.";
        }
        return evaluacion.getObservaciones();
    }

    private String obtenerConclusiones(EvaluacionCalidad evaluacion) {
        if (evaluacion.getResultado() == null) {
            return "";
        }
        return switch (evaluacion.getResultado()) {
            case CONFORME -> "Lote conforme según criterios microbiológicos.";
            case NO_CONFORME -> "Lote no conforme según criterios microbiológicos.";
            case CONDICIONADO -> "Lote condicionado. Revisar observaciones y acciones asociadas.";
        };
    }

    private String obtenerNombreElabora(EvaluacionCalidad evaluacion) {
        return Optional.ofNullable(evaluacion.getUsuarioEvaluador())
                .map(Usuario::getNombreCompleto)
                .filter(nombre -> !nombre.isBlank())
                .orElse("Alexander Baloco");
    }

    private String loadTemplate(String path) {
        try {
            var resource = new ClassPathResource(path);
            try (var in = resource.getInputStream()) {
                return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar la plantilla de reporte microbiológico", e);
        }
    }
}
