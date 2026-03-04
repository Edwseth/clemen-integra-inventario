package com.willyes.clemenintegra.inventario.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import com.willyes.clemenintegra.inventario.model.enums.CondicionesPago;
import com.willyes.clemenintegra.inventario.model.enums.TipoOrdenCompra;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
public class OrdenCompraPdfService {

    private static final Locale ES_CO = new Locale("es", "CO");

    public byte[] generarPdf(OrdenCompra oc) {
        String html = componerHtml(oc);
        return renderPdf(html);
    }

    /* -------------------- COMPOSICIÓN DE HTML -------------------- */

    private String componerHtml(OrdenCompra oc) {
        String html = loadTemplate("templates/oc/orden-compra.ftl");

        // Fecha (usa fecha_orden si existe)
        LocalDateTime f = oc.getFechaOrden() != null ? oc.getFechaOrden() : LocalDateTime.now();
        LocalDate fecha = f.toLocalDate();

        String fechaDia  = String.format("%02d", fecha.getDayOfMonth());
        String fechaMes  = monthUpper(fecha);     // “OCT”
        String fechaAnio = String.valueOf(fecha.getYear());

        // Empresa (estático por ahora; puedes parametrizarlo cuando definas tu tabla de empresa)
        html = html.replace("${empresa.nombre}",    esc("LABORATORIO CLEMEN SAS"))
                .replace("${empresa.nit}",       esc("901626440"))
                .replace("${empresa.direccion}", esc("Carrera 41 D # 46-40 Union de Vivienda, Cali Valle del Cauca, Colombia"))
                .replace("${empresa.telefono}",  esc("Teléfono / Movil 3175081762"));

        // Encabezado OC
        html = html.replace("${numero}",   esc(oc.getCodigoOrden() != null ? oc.getCodigoOrden() : String.valueOf(oc.getId())))
                .replace("${fechaDia}",  fechaDia)
                .replace("${fechaMes}",  fechaMes)
                .replace("${fechaAnio}", fechaAnio)
                .replace("${tituloDocumento}", oc.getTipo() == TipoOrdenCompra.SERVICIOS ? "Orden de Servicio" : "Orden de Compra");

        // Proveedor
        Proveedor p = oc.getProveedor();
        String provNombre    = p != null && p.getNombre() != null ? p.getNombre() : "";
        String provNit       = p != null && p.getIdentificacion() != null ? p.getIdentificacion() : "";
        String provDir       = p != null && p.getDireccion() != null ? p.getDireccion() : "";
        String provCiudad    = p != null && p.getCiudad() != null ? p.getCiudad() : "";
        String provTelefono  = p != null && p.getTelefono() != null ? p.getTelefono() : "";
        String provPaginaWeb = p != null && p.getPaginaWeb() != null ? p.getPaginaWeb() : "";

        String provTel = !provTelefono.isBlank() ? "Teléfono: " + esc(provTelefono) : "";
        String provWeb = !provPaginaWeb.isBlank() ? " &#160;Página Web: " + esc(provPaginaWeb) : "";

        html = html.replace("${proveedor.nombre}",    esc(provNombre))
                .replace("${proveedor.nit}",       esc(provNit))
                .replace("${proveedor.direccion}", esc(provDir))
                .replace("${proveedor.ciudad}",    esc(provCiudad))
                .replace("${prov_tel}",            provTel)
                .replace("${prov_web}",            provWeb);

        // Condiciones de pago y comprador
        String condicionesPago = Optional.ofNullable(oc.getCondicionesPago())
                .map(this::formCondicion)   // <— acepta el enum, no el .name()
                .orElse("—");
        html = html.replace("${condicionesPago}", esc(condicionesPago));

        String comprador = oc.getComprador() != null ? oc.getComprador() : "";
        html = html.replace("${comprador}", esc(comprador));

        // Observaciones
        html = html.replace("${observaciones}", escNull(oc.getObservaciones()));

        // Filas de ítems + totales
        StringBuilder rows = new StringBuilder();
        BigDecimal sumSubtotal = BigDecimal.ZERO;
        BigDecimal sumIva      = BigDecimal.ZERO;
        BigDecimal sumIcui     = BigDecimal.ZERO;     // si no lo manejas, quedará en 0
        BigDecimal descuento   = BigDecimal.ZERO;

        if (oc.getDetalles() != null) {
            for (OrdenCompraDetalle d : oc.getDetalles()) {
                String codigo = d.getProducto() != null ? nz(d.getProducto().getCodigoSku()) : "";
                String desc   = d.getProducto() != null ? nz(d.getProducto().getNombre())    : "";

                // UDM: preferir símbolo de impresión (mL, kg, m...), fallback a símbolo (ML, KG, M)
                String udmDisplay = "";
                if (d.getProducto() != null && d.getProducto().getUnidadMedida() != null) {
                    var u = d.getProducto().getUnidadMedida();
                    String imp = u.getSimboloImpresion();
                    String sim = u.getSimbolo();
                    String simboloPref = (imp != null && !imp.isBlank()) ? imp : nz(sim);

                    String singular = nz(u.getNombre());         // p.ej. "GRAMO"
                    String plural   = u.getNombrePlural();       // p.ej. "GRAMOS"

                    BigDecimal cant = nbd(d.getCantidad());
                    boolean esSingular = cant.compareTo(BigDecimal.ONE) == 0;

                    String textoUnidad = esSingular
                            ? (singular.isBlank() ? "UNIDAD" : singular)
                            : (plural == null || plural.isBlank()
                            ? (singular.isBlank() ? "UNIDADES" : singular + "S")
                            : plural);

                    udmDisplay = textoUnidad.toUpperCase(ES_CO) + " (" + simboloPref + ")";
                }

                // Fecha Necesidad (dd-MMM-yyyy ES, mayúsculas y sin punto)
                String fechaNecStr = "";
                if (d.getFechaNecesidad() != null) {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy", ES_CO);
                    String raw = d.getFechaNecesidad().format(fmt).replace(".", "");
                    fechaNecStr = Normalizer.normalize(raw, Normalizer.Form.NFD)
                            .replaceAll("\\p{InCombiningDiacriticalMarks}+","")
                            .toUpperCase(ES_CO);
                }

                BigDecimal cant   = nbd(d.getCantidad());
                BigDecimal pUnit  = nbd(d.getValorUnitario());
                BigDecimal ivaPct = nbd(d.getIva());                // % (0..100)
                BigDecimal bdTot  = nbd(d.getValorTotal());

                // Subtotal de la línea (base)
                BigDecimal subtotal = cant.multiply(pUnit);

                // Impuestos de la línea
                BigDecimal ivaValor  = ivaPct.compareTo(BigDecimal.ZERO) > 0
                        ? subtotal.multiply(ivaPct).divide(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;
                BigDecimal icuiValor = BigDecimal.ZERO; // ajusta si aplicas ICUI por ítem

                // Total de la línea (si no viene en BD, calcular)
                BigDecimal lineTotal = bdTot.compareTo(BigDecimal.ZERO) > 0
                        ? bdTot
                        : subtotal.add(ivaValor).add(icuiValor);

                // Acumular
                sumSubtotal = sumSubtotal.add(subtotal);
                sumIva      = sumIva.add(ivaValor);
                sumIcui     = sumIcui.add(icuiValor);


                // Fila HTML
                rows.append("<tr>")
                        .append("<td>").append(esc(codigo)).append("</td>")
                        .append("<td>").append(esc(desc)).append("</td>")
                        .append("<td class='txt-center nowrap'>").append(esc(fechaNecStr)).append("</td>")
                        .append("<td class='txt-center nowrap'>").append(esc(udmDisplay)).append("</td>")
                        .append("<td class='right'>").append(formNum(cant)).append("</td>")
                        .append("<td class='right'>").append(formNum(pUnit)).append("</td>")
                        .append("<td class='right'>").append(formNum(ivaPct)).append("</td>")
                        .append("<td class='right'>").append("0").append("</td>") // ICUI %
                        .append("<td class='right'>").append(formNum(lineTotal)).append("</td>")
                        .append("</tr>");
            }
        }

        // Total general = subtotal - descuento + impuestos
        descuento = nbd(oc.getDescuento());
        BigDecimal sumTotal = sumSubtotal.subtract(descuento).add(sumIva).add(sumIcui);

        html = html.replace("${itemsRows}", rows.toString())
                .replace("${subtotal}",  formNum(sumSubtotal))
                .replace("${descuento}", formNum(descuento))
                .replace("${valorIva}",  formNum(sumIva))
                .replace("${valorIcui}", formNum(sumIcui))
                .replace("${total}",     formNum(sumTotal));

        return html;
    }

    /* -------------------- RENDER PDF -------------------- */

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF de la Orden de Compra", e);
        }
    }

    /* -------------------- HELPERS -------------------- */

    private String loadTemplate(String classpathLocation) {
        try (InputStream is = new ClassPathResource(classpathLocation).getInputStream()) {
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("No se encontró la plantilla: " + classpathLocation, e);
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"","&quot;")
                .replace("'", "&#39;");
    }

    private static String escNull(String s) { return s == null ? "" : esc(s); }

    private static String nz(String s) { return s == null ? "" : s; }

    private static BigDecimal nbd(Number n) {
        if (n == null) return BigDecimal.ZERO;
        if (n instanceof BigDecimal) return (BigDecimal) n;
        return new BigDecimal(n.toString());
    }

    private static String formNum(BigDecimal v) {
        NumberFormat nf = NumberFormat.getNumberInstance(ES_CO);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(v);
    }

    private String formCondicion(CondicionesPago cp) {
        return switch (cp) {
            case ANTICIPADO -> "ANTICIPADO";
            case CONTADO -> "CONTADO";
            case DIAS_30 -> "30 DÍAS NETO";
            case DIAS_60 -> "60 DÍAS NETO";
        };
    }

    private static String monthUpper(LocalDate date) {
        String s = date.format(DateTimeFormatter.ofPattern("MMM", ES_CO));
        s = s.replace(".", "");
        // quitar tildes y poner mayúsculas
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return s.toUpperCase(ES_CO);
    }
}



