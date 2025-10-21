package com.willyes.clemenintegra.inventario.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class OrdenCompraPdfService {

    private static final Locale ES_CO = new Locale("es", "CO");
    private static final DateTimeFormatter FECHA_ES =
            DateTimeFormatter.ofPattern("dd MMM yyyy", ES_CO);

    /** Recibe HTML completo y devuelve el PDF en bytes. */
    public byte[] render(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null); // baseURL null si no cargas assets locales
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar PDF", e);
        }
    }

    /** Fecha local en mayúsculas: 01 OCT 2025 */
    public static String fechaES(LocalDate date) {
        return FECHA_ES.format(date).toUpperCase(ES_CO);
    }
}


