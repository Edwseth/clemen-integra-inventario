package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.StockDisponibleComparativoResponseDTO;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockDisponibleComparativoServiceTest {

    @Mock
    private InventarioGeneralCorteReportService inventarioGeneralCorteReportService;
    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private StockDisponibleComparativoService service;

    @Test
    @DisplayName("Hace merge por sku+lote+ubicacion, completa faltantes en 0 y calcula diferencia con BigDecimal")
    void obtenerComparativo_mergeaConFaltantesYDiferencia() {
        LocalDate fecha = LocalDate.of(2024, 6, 15);
        LocalDate hoy = LocalDate.now();

        when(inventarioGeneralCorteReportService.calcularFilasInventarioGeneralCorte(eq(fecha.atTime(LocalTime.MAX)), any()))
                .thenReturn(List.of(
                        new InventarioGeneralCorteReportService.InventarioGeneralRow(
                                "SKU-1", "Producto 1", "KG", new BigDecimal("10.50"), "LOTE-A", "2025-01-01", "A1"
                        ),
                        new InventarioGeneralCorteReportService.InventarioGeneralRow(
                                "SKU-2", "Producto 2", "KG", new BigDecimal("7"), "LOTE-B", "2026-02-02", "B2"
                        )
                ));
        when(inventarioGeneralCorteReportService.calcularFilasInventarioGeneralCorte(eq(hoy.atTime(LocalTime.MAX)), any()))
                .thenReturn(List.of(
                        new InventarioGeneralCorteReportService.InventarioGeneralRow(
                                "SKU-1", "Producto 1", "KG", new BigDecimal("12.00"), "LOTE-A", "2025-01-01", "A1"
                        ),
                        new InventarioGeneralCorteReportService.InventarioGeneralRow(
                                "SKU-3", "Producto 3", "KG", new BigDecimal("2.25"), "LOTE-C", "2027-03-03", "C3"
                        )
                ));

        List<StockDisponibleComparativoResponseDTO> resultado = service.obtenerComparativo(fecha, null, "sku", "asc");

        assertThat(resultado).hasSize(3);

        StockDisponibleComparativoResponseDTO sku1 = resultado.stream()
                .filter(r -> "SKU-1".equals(r.sku()))
                .findFirst()
                .orElseThrow();
        assertThat(sku1.cantidadFechaIndicada()).isEqualByComparingTo("10.50");
        assertThat(sku1.cantidadActual()).isEqualByComparingTo("12.00");
        assertThat(sku1.diferencia()).isEqualByComparingTo("1.50");

        StockDisponibleComparativoResponseDTO sku2 = resultado.stream()
                .filter(r -> "SKU-2".equals(r.sku()))
                .findFirst()
                .orElseThrow();
        assertThat(sku2.cantidadFechaIndicada()).isEqualByComparingTo("7");
        assertThat(sku2.cantidadActual()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(sku2.diferencia()).isEqualByComparingTo("-7");

        StockDisponibleComparativoResponseDTO sku3 = resultado.stream()
                .filter(r -> "SKU-3".equals(r.sku()))
                .findFirst()
                .orElseThrow();
        assertThat(sku3.cantidadFechaIndicada()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(sku3.cantidadActual()).isEqualByComparingTo("2.25");
        assertThat(sku3.diferencia()).isEqualByComparingTo("2.25");
    }
}
