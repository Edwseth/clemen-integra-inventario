package com.willyes.clemenintegra.planeacion.service;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.DetalleCorridaMrp;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.model.enums.TipoSugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.repository.CorridaMrpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MrpReporteServiceTest {

    @Mock
    private CorridaMrpRepository corridaMrpRepository;

    @InjectMocks
    private MrpReporteService mrpReporteService;

    private CorridaMrp corrida;

    @BeforeEach
    void setUp() {
        CategoriaProducto categoria = CategoriaProducto.builder()
                .id(1L)
                .nombre("Materia Prima")
                .build();

        Producto producto = Producto.builder()
                .id(1)
                .codigoSku("MAT-001")
                .nombre("Ácido cítrico")
                .categoriaProducto(categoria)
                .build();

        DetalleCorridaMrp detalle = DetalleCorridaMrp.builder()
                .id(1L)
                .producto(producto)
                .requerimientoBruto(BigDecimal.valueOf(100))
                .inventarioDisponible(BigDecimal.valueOf(20))
                .requerimientoNeto(BigDecimal.valueOf(80))
                .build();

        SugerenciaAbastecimiento sugerencia = SugerenciaAbastecimiento.builder()
                .id(5L)
                .detalleCorrida(detalle)
                .tipo(TipoSugerenciaAbastecimiento.COMPRA)
                .cantidadSugerida(BigDecimal.valueOf(80))
                .fechaNecesidad(LocalDate.now())
                .estado(null)
                .build();
        detalle.setSugerencia(sugerencia);

        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .id(3L)
                .semanaInicio(LocalDate.now())
                .semanaFin(LocalDate.now().plusDays(7))
                .build();

        corrida = CorridaMrp.builder()
                .id(10L)
                .planProduccionSemanal(plan)
                .fechaEjecucion(LocalDateTime.now())
                .detalles(List.of(detalle))
                .build();
    }

    @Test
    void generarExcelCorridaDevuelveContenido() {
        when(corridaMrpRepository.findWithDetallesById(anyLong())).thenReturn(Optional.of(corrida));

        byte[] excel = mrpReporteService.generarExcelCorrida(10L);

        assertNotNull(excel);
        assertNotEquals(0, excel.length);
    }

    @Test
    void generarPdfCorridaDevuelveContenido() {
        when(corridaMrpRepository.findWithDetallesById(anyLong())).thenReturn(Optional.of(corrida));

        byte[] pdf = mrpReporteService.generarPdfCorrida(10L);

        assertNotNull(pdf);
        assertNotEquals(0, pdf.length);
    }
}
