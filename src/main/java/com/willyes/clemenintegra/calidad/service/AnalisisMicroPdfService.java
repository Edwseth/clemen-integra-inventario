package com.willyes.clemenintegra.calidad.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.ParametroAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.PlantillaAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.ResultadoAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.repository.ResultadoAnalisisMicrobiologicoRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class AnalisisMicroPdfService {

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

        String html = loadTemplate("templates/calidad/analisis_microbiologico.ftl");
        html = html.replace("${empresa.nombre}", esc("LABORATORIO CLEMEN"))
                .replace("${empresa.nit}", esc("NIT PENDIENTE"))
                .replace("${empresa.direccion}", esc("Direccion"))
                .replace("${empresa.telefono}", esc("Telefono"));

        html = html.replace("${producto}", esc(evaluacion.getLoteProducto().getProducto().getNombre()))
                .replace("${lote}", esc(evaluacion.getLoteProducto().getCodigoLote()))
                .replace("${fecha}", evaluacion.getFechaEvaluacion() != null
                        ? evaluacion.getFechaEvaluacion().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        : "")
                .replace("${analista}", evaluacion.getUsuarioEvaluador() != null
                        ? esc(evaluacion.getUsuarioEvaluador().getNombreCompleto()) : "");

        html = html.replace("${tablaResultados}", construirTabla(resultados, plantilla));
        return renderPdf(html);
    }

    private String construirTabla(List<ResultadoAnalisisMicrobiologico> resultados,
                                  PlantillaAnalisisMicrobiologico plantilla) {
        Map<Long, ResultadoAnalisisMicrobiologico> index = new HashMap<>();
        resultados.forEach(r -> index.put(r.getParametro().getId(), r));
        StringBuilder sb = new StringBuilder();
        List<ParametroAnalisisMicrobiologico> parametros = plantilla != null ? plantilla.getParametros() : List.of();
        for (ParametroAnalisisMicrobiologico p : parametros) {
            ResultadoAnalisisMicrobiologico res = index.get(p.getId());
            sb.append("<tr>")
                    .append("<td>").append(esc(p.getNombreEnsayo())).append("</td>")
                    .append("<td>").append(esc(p.getUnidad())).append("</td>")
                    .append("<td>").append(esc(p.getEspecificacion())).append("</td>")
                    .append("<td>").append(res != null ? esc(res.getResultado()) : "").append("</td>")
                    .append("<td>").append(res != null && res.getCumple() != null ? (res.getCumple() ? "SI" : "NO") : "")
                    .append("</td>")
                    .append("<td>").append(res != null ? esc(res.getObservaciones()) : "").append("</td>")
                    .append("</tr>");
        }
        return sb.toString();
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

    private String esc(String val) {
        return val == null ? "" : val.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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

