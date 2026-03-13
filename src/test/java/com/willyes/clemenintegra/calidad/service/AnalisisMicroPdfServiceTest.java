package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.model.*;
import com.willyes.clemenintegra.calidad.model.enums.TipoResultadoAnalisis;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.repository.ResultadoAnalisisMicrobiologicoRepository;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalisisMicroPdfServiceTest {

    @Mock
    private EvaluacionCalidadRepository evaluacionRepository;

    @Mock
    private ResultadoAnalisisMicrobiologicoRepository resultadoRepository;

    @Test
    void generaPdfConDatosMinimos() {
        PlantillaAnalisisMicrobiologico plantilla = PlantillaAnalisisMicrobiologico.builder()
                .id(1L)
                .nombre("Plantilla")
                .build();
        ParametroAnalisisMicrobiologico param = ParametroAnalisisMicrobiologico.builder()
                .id(2L)
                .plantilla(plantilla)
                .nombreEnsayo("Ensayo")
                .metodo("USP <61>")
                .tipoResultado(TipoResultadoAnalisis.TEXTO)
                .orden(1)
                .build();
        plantilla.setParametros(List.of(param));

        Producto producto = Producto.builder().id(10).nombre("Producto X").plantillaAnalisisMicrobiologico(plantilla).build();
        producto.setCodigoSku("SKU-123");
        LoteProducto lote = LoteProducto.builder().id(20L).producto(producto).codigoLote("LT-1").build();
        Usuario usuario = Usuario.builder().id(30L).nombreCompleto("Ana Perez").build();
        EvaluacionCalidad evaluacion = EvaluacionCalidad.builder()
                .id(5L)
                .loteProducto(lote)
                .usuarioEvaluador(usuario)
                .observaciones("Sin novedades")
                .resultado(com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion.CONFORME)
                .fechaEvaluacion(LocalDateTime.of(2024, 2, 10, 12, 0))
                .build();

        ResultadoAnalisisMicrobiologico res = ResultadoAnalisisMicrobiologico.builder()
                .id(3L)
                .evaluacion(evaluacion)
                .parametro(param)
                .resultado("Ok")
                .build();

        when(evaluacionRepository.findById(5L)).thenReturn(Optional.of(evaluacion));
        when(resultadoRepository.findByEvaluacionId(5L)).thenReturn(List.of(res));

        AnalisisMicroPdfService service = new AnalisisMicroPdfService(evaluacionRepository, resultadoRepository);

        String html = service.construirHtml(evaluacion, List.of(res), plantilla);
        assertThat(html).contains("LABORATORIO DE MICROBIOLOGÍA CLEMEN");
        assertThat(html).contains("CÓDIGO: FOR-MIC-009");
        assertThat(html).contains("Ensayo");
        assertThat(html).contains("USP &lt;61&gt;");

        byte[] pdf = service.generarPdf(5L);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(100);
    }
}
