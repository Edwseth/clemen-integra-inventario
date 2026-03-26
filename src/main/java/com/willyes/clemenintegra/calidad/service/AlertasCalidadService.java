package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.AlertaLoteCalidadDTO;
import com.willyes.clemenintegra.calidad.dto.ResumenAlertasCalidadDTO;
import com.willyes.clemenintegra.calidad.model.enums.TipoAlertaLoteCalidad;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertasCalidadService {

    private static final int DIAS_UMBRAL_DEFECTO = 30;
    private static final EnumSet<EstadoLote> ESTADOS_ALERTA_VENCIMIENTO = EnumSet.of(
            EstadoLote.DISPONIBLE,
            EstadoLote.LIBERADO,
            EstadoLote.EN_CUARENTENA,
            EstadoLote.RETENIDO
    );
    private static final EnumSet<EstadoLote> ESTADOS_PENDIENTES_LIBERAR = EnumSet.of(
            EstadoLote.EN_CUARENTENA,
            EstadoLote.RETENIDO
    );

    private final LoteProductoRepository loteProductoRepository;
    private Clock clock = Clock.systemDefaultZone();

    public ResumenAlertasCalidadDTO obtenerAlertas(int diasUmbral) {
        int umbral = diasUmbral > 0 ? diasUmbral : DIAS_UMBRAL_DEFECTO;
        LocalDate hoy = LocalDate.now(clock);
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.plusDays(umbral).atTime(LocalTime.MAX);

        // Radiografía: FEFO/vencimientos ya se consultan en Inventarios con
        // - LoteProductoRepository#findByFechaVencimientoBetweenFetchProducto / #findVencidosFetch
        // - LoteProductoRepository#listarLotesConVencimiento y FEFO en #findFefoSalidaPt
        // Radiografía: pendientes de liberar se listan en LoteProductoServiceImpl#obtenerLotesPorEvaluar
        // y en AlertaInventarioServiceImpl#obtenerLotesRetenidosOCuarentenaProlongados.
        List<LoteProducto> proximos = loteProductoRepository.findAlertasProximasVencer(inicio, fin, ESTADOS_ALERTA_VENCIMIENTO);
        List<LoteProducto> vencidos = loteProductoRepository.findAlertasVencidos(inicio, ESTADOS_ALERTA_VENCIMIENTO);
        List<LoteProducto> pendientes = loteProductoRepository.findAlertasPendientesLiberar(ESTADOS_PENDIENTES_LIBERAR);

        List<AlertaLoteCalidadDTO> lotesProximos = proximos.stream()
                .map(lote -> mapAlerta(lote, TipoAlertaLoteCalidad.PROXIMO_VENCER, hoy))
                .toList();
        List<AlertaLoteCalidadDTO> lotesVencidos = vencidos.stream()
                .map(lote -> mapAlerta(lote, TipoAlertaLoteCalidad.VENCIDO, hoy))
                .toList();
        List<AlertaLoteCalidadDTO> lotesPendientesLiberar = pendientes.stream()
                .map(lote -> mapAlerta(lote, TipoAlertaLoteCalidad.PENDIENTE_LIBERAR, hoy))
                .toList();

        // TODO: enganchar aquí una futura notificación automática (correo/job) con las alertas consolidadas.
        return ResumenAlertasCalidadDTO.builder()
                .lotesProximosVencer(lotesProximos)
                .lotesVencidos(lotesVencidos)
                .lotesPendientesLiberar(lotesPendientesLiberar)
                .totalProximosVencer(lotesProximos.size())
                .totalVencidos(lotesVencidos.size())
                .totalPendientesLiberar(lotesPendientesLiberar.size())
                .build();
    }

    public byte[] generarReporteAlertasExcel(int diasUmbral) {
        ResumenAlertasCalidadDTO resumen = obtenerAlertas(diasUmbral);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            crearHojaAlertas(workbook, "Próximos a vencer", resumen.getLotesProximosVencer(), dtf);
            crearHojaAlertas(workbook, "Vencidos", resumen.getLotesVencidos(), dtf);
            crearHojaAlertas(workbook, "Pendientes liberar", resumen.getLotesPendientesLiberar(), dtf);
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el Excel de alertas de calidad", e);
        }
    }

    private void crearHojaAlertas(Workbook workbook, String nombre, List<AlertaLoteCalidadDTO> alertas, DateTimeFormatter dtf) {
        Sheet sheet = workbook.createSheet(nombre);
        String[] headers = {
                "Tipo Alerta", "Código Lote", "SKU", "Producto", "Almacén", "Estado Lote", "Fecha Vencimiento", "Días para vencer"
        };

        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        int rowIdx = 1;
        List<AlertaLoteCalidadDTO> filas = alertas != null ? alertas : List.of();
        for (AlertaLoteCalidadDTO alerta : filas) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(alerta.getTipoAlerta() != null ? alerta.getTipoAlerta().name() : "");
            row.createCell(1).setCellValue(alerta.getCodigoLote() != null ? alerta.getCodigoLote() : "");
            row.createCell(2).setCellValue(alerta.getCodigoSku() != null ? alerta.getCodigoSku() : "");
            row.createCell(3).setCellValue(alerta.getNombreProducto() != null ? alerta.getNombreProducto() : "");
            row.createCell(4).setCellValue(alerta.getNombreAlmacen() != null ? alerta.getNombreAlmacen() : "");
            row.createCell(5).setCellValue(alerta.getEstadoLote() != null ? alerta.getEstadoLote().name() : "");
            row.createCell(6).setCellValue(alerta.getFechaVencimiento() != null ? alerta.getFechaVencimiento().format(dtf) : "");
            row.createCell(7).setCellValue(alerta.getDiasParaVencer() != null ? alerta.getDiasParaVencer() : 0);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private AlertaLoteCalidadDTO mapAlerta(LoteProducto lote, TipoAlertaLoteCalidad tipo, LocalDate hoy) {
        LocalDateTime fechaVencimiento = lote.getFechaVencimiento();
        Integer diasParaVencer = null;
        if (fechaVencimiento != null) {
            diasParaVencer = (int) ChronoUnit.DAYS.between(hoy, fechaVencimiento.toLocalDate());
        }
        return AlertaLoteCalidadDTO.builder()
                .loteId(lote.getId())
                .codigoLote(lote.getCodigoLote())
                .productoId(lote.getProducto() != null ? lote.getProducto().getId().longValue() : null)
                .codigoSku(lote.getProducto() != null ? lote.getProducto().getCodigoSku() : null)
                .nombreProducto(lote.getProducto() != null ? lote.getProducto().getNombre() : null)
                .almacenId(lote.getAlmacen() != null ? lote.getAlmacen().getId().longValue() : null)
                .nombreAlmacen(lote.getAlmacen() != null ? lote.getAlmacen().getNombre() : null)
                .estadoLote(lote.getEstado())
                .fechaVencimiento(fechaVencimiento)
                .diasParaVencer(diasParaVencer)
                .tipoAlerta(tipo)
                .build();
    }

    void setClock(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }
}
