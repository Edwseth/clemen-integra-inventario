package com.willyes.clemenintegra.inventario.regularizacion.service.impl;

import com.willyes.clemenintegra.inventario.regularizacion.dto.variaciones.RegularizacionDetalleDTO;
import com.willyes.clemenintegra.inventario.regularizacion.dto.variaciones.TipoVariacionDTO;
import com.willyes.clemenintegra.inventario.regularizacion.dto.variaciones.VariacionOPResponseDTO;
import com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidadDetalle;
import com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadDetalleRepository;
import com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadRepository;
import com.willyes.clemenintegra.inventario.regularizacion.repository.projection.VariacionRegularizacionProjection;
import com.willyes.clemenintegra.inventario.regularizacion.service.variaciones.VariacionesProduccionService;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VariacionesProduccionServiceImpl implements VariacionesProduccionService {

    private final RegularizacionTrazabilidadRepository regularizacionRepository;
    private final RegularizacionTrazabilidadDetalleRepository detalleRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<VariacionOPResponseDTO> listarVariaciones(LocalDate fechaInicio,
                                                          LocalDate fechaFin,
                                                          Long ordenProduccionId,
                                                          boolean soloConVariacion,
                                                          TipoVariacionDTO tipoVariacion,
                                                          Pageable pageable) {
        return regularizacionRepository.findUltimasVariacionesPorOP(
                        toStartDateTime(fechaInicio),
                        toEndDateTime(fechaFin),
                        ordenProduccionId,
                        soloConVariacion,
                        tipoVariacion != null ? tipoVariacion.name() : null,
                        pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegularizacionDetalleDTO> obtenerDetalle(Long regularizacionId) {
        regularizacionRepository.findById(regularizacionId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "No se encontró la regularización " + regularizacionId));

        return detalleRepository.findByRegularizacionIdOrderByIdAsc(regularizacionId)
                .stream()
                .map(this::mapDetalle)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarExcel(LocalDate fechaInicio,
                                LocalDate fechaFin,
                                Long ordenProduccionId,
                                boolean soloConVariacion,
                                TipoVariacionDTO tipoVariacion) {
        List<VariacionOPResponseDTO> variaciones = regularizacionRepository.findUltimasVariacionesPorOPSinPaginacion(
                        toStartDateTime(fechaInicio),
                        toEndDateTime(fechaFin),
                        ordenProduccionId,
                        soloConVariacion,
                        tipoVariacion != null ? tipoVariacion.name() : null)
                .stream()
                .map(this::mapToResponse)
                .toList();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Variaciones");
            Row header = sheet.createRow(0);
            String[] columnas = {
                    "fechaIngreso", "ordenProduccionId", "cantidadProgramada", "cantidadReal", "diferencia",
                    "rendimientoPct", "diferenciaPct", "ajustarPt", "tieneDetalle", "documentoReferencia",
                    "observaciones", "usuarioId"
            };
            for (int i = 0; i < columnas.length; i++) {
                header.createCell(i).setCellValue(columnas[i]);
            }

            int rowNum = 1;
            for (VariacionOPResponseDTO item : variaciones) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(item.fechaIngreso() != null ? item.fechaIngreso().toString() : "");
                row.createCell(1).setCellValue(item.ordenProduccionId() != null ? item.ordenProduccionId() : 0L);
                row.createCell(2).setCellValue(decimalToDouble(item.cantidadProgramada()));
                row.createCell(3).setCellValue(decimalToDouble(item.cantidadReal()));
                row.createCell(4).setCellValue(decimalToDouble(item.diferencia()));
                row.createCell(5).setCellValue(decimalToDouble(item.rendimientoPct()));
                row.createCell(6).setCellValue(decimalToDouble(item.diferenciaPct()));
                row.createCell(7).setCellValue(Boolean.TRUE.equals(item.ajustarPt()));
                row.createCell(8).setCellValue(Boolean.TRUE.equals(item.tieneDetalle()));
                row.createCell(9).setCellValue(item.documentoReferencia() != null ? item.documentoReferencia() : "");
                row.createCell(10).setCellValue(item.observaciones() != null ? item.observaciones() : "");
                row.createCell(11).setCellValue(item.usuarioId() != null ? item.usuarioId() : 0L);
            }

            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Error generando Excel de variaciones de producción", e);
            throw new IllegalStateException("No se pudo generar el Excel de variaciones", e);
        }
    }

    private VariacionOPResponseDTO mapToResponse(VariacionRegularizacionProjection row) {
        BigDecimal cantidadProgramada = row.getCantidadProgramada();
        boolean inconsistente = cantidadProgramada == null || cantidadProgramada.compareTo(BigDecimal.ZERO) == 0;

        BigDecimal rendimientoPct = inconsistente
                ? null
                : safePercent(row.getCantidadReal(), cantidadProgramada);

        BigDecimal diferenciaPct = inconsistente
                ? null
                : safePercent(row.getDiferencia(), cantidadProgramada);

        return VariacionOPResponseDTO.builder()
                .regularizacionId(row.getRegularizacionId())
                .ordenProduccionId(row.getOrdenProduccionId())
                .cantidadProgramada(cantidadProgramada)
                .cantidadReal(row.getCantidadReal())
                .diferencia(row.getDiferencia())
                .rendimientoPct(rendimientoPct)
                .diferenciaPct(diferenciaPct)
                .ajustarPt(row.getAjustarPt())
                .documentoReferencia(row.getDocumentoReferencia())
                .observaciones(row.getObservaciones())
                .usuarioId(row.getUsuarioId())
                .fechaIngreso(row.getFechaIngreso())
                .tieneDetalle(Boolean.TRUE.equals(row.getTieneDetalle()))
                .dataInconsistente(inconsistente)
                .build();
    }

    private RegularizacionDetalleDTO mapDetalle(RegularizacionTrazabilidadDetalle detalle) {
        return new RegularizacionDetalleDTO(
                detalle.getProductoId(),
                detalle.getLoteId(),
                detalle.getCantidad(),
                detalle.getTipo(),
                detalle.getAlmacenOrigenId(),
                detalle.getAlmacenDestinoId(),
                detalle.getMovimiento() != null ? detalle.getMovimiento().getId() : null
        );
    }

    private BigDecimal safePercent(BigDecimal numerador, BigDecimal denominador) {
        if (numerador == null || denominador == null || denominador.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerador.multiply(BigDecimal.valueOf(100))
                .divide(denominador, 2, RoundingMode.HALF_UP);
    }

    private LocalDateTime toStartDateTime(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private LocalDateTime toEndDateTime(LocalDate date) {
        return date != null ? date.atTime(LocalTime.MAX) : null;
    }

    private double decimalToDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0d;
    }
}
