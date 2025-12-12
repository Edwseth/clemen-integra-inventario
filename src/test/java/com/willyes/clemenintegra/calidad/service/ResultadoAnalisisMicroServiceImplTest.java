package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ResultadoAnalisisMicroRequestDTO;
import com.willyes.clemenintegra.calidad.model.*;
import com.willyes.clemenintegra.calidad.model.enums.TipoResultadoAnalisis;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.repository.ResultadoAnalisisMicrobiologicoRepository;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultadoAnalisisMicroServiceImplTest {

    @Mock
    private EvaluacionCalidadRepository evaluacionRepository;

    @Mock
    private ResultadoAnalisisMicrobiologicoRepository resultadoRepository;

    @Mock
    private AnalisisMicroPdfService analisisMicroPdfService;

    @InjectMocks
    private ResultadoAnalisisMicroServiceImpl service;

    @Test
    void debeGuardarResultadosCuandoParametroPerteneceALaPlantilla() {
        PlantillaAnalisisMicrobiologico plantilla = PlantillaAnalisisMicrobiologico.builder()
                .id(10L)
                .nombre("Plantilla")
                .build();
        ParametroAnalisisMicrobiologico parametro = ParametroAnalisisMicrobiologico.builder()
                .id(5L)
                .plantilla(plantilla)
                .nombreEnsayo("Mesófilos")
                .tipoResultado(TipoResultadoAnalisis.NUMERICO)
                .orden(1)
                .build();
        plantilla.setParametros(List.of(parametro));

        Producto producto = Producto.builder().id(1).nombre("Prod").plantillaAnalisisMicrobiologico(plantilla).build();
        LoteProducto lote = LoteProducto.builder().id(2L).producto(producto).codigoLote("L-1").build();
        EvaluacionCalidad evaluacion = EvaluacionCalidad.builder()
                .id(3L)
                .loteProducto(lote)
                .tipoEvaluacion(com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .archivosAdjuntos(new java.util.ArrayList<>())
                .fechaEvaluacion(LocalDateTime.now())
                .build();

        when(evaluacionRepository.findById(3L)).thenReturn(Optional.of(evaluacion));
        when(resultadoRepository.findByEvaluacionId(3L)).thenReturn(List.of());
        when(resultadoRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(analisisMicroPdfService.generarPdf(3L)).thenReturn("pdf".getBytes());

        var payload = List.of(ResultadoAnalisisMicroRequestDTO.builder()
                .parametroId(5L)
                .resultado("10")
                .cumple(true)
                .build());

        var res = service.guardarResultados(3L, payload);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getResultado()).isEqualTo("10");
        assertThat(res.get(0).getNombreEnsayo()).isEqualTo("Mesófilos");
    }

    @Test
    void conservaAdjuntosQuimicosYRegistraPdfMicro() {
        PlantillaAnalisisMicrobiologico plantilla = PlantillaAnalisisMicrobiologico.builder()
                .id(11L)
                .build();
        ParametroAnalisisMicrobiologico parametro = ParametroAnalisisMicrobiologico.builder()
                .id(7L)
                .plantilla(plantilla)
                .nombreEnsayo("Moho")
                .tipoResultado(TipoResultadoAnalisis.NUMERICO)
                .orden(1)
                .build();
        plantilla.setParametros(List.of(parametro));

        Producto producto = Producto.builder().id(3).nombre("Prod B").plantillaAnalisisMicrobiologico(plantilla).build();
        LoteProducto lote = LoteProducto.builder().id(4L).producto(producto).codigoLote("L-2").build();
        EvaluacionCalidad evaluacion = EvaluacionCalidad.builder()
                .id(8L)
                .loteProducto(lote)
                .tipoEvaluacion(com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .archivosAdjuntos(new java.util.ArrayList<>(List.of(
                        ArchivoEvaluacion.builder().nombreVisible("Químico").nombreArchivo("quim.pdf").build())))
                .fechaEvaluacion(LocalDateTime.now())
                .build();

        when(evaluacionRepository.findById(8L)).thenReturn(Optional.of(evaluacion));
        when(resultadoRepository.findByEvaluacionId(8L)).thenReturn(List.of());
        when(resultadoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(analisisMicroPdfService.generarPdf(8L)).thenReturn("pdf".getBytes());
        when(evaluacionRepository.save(any(EvaluacionCalidad.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var payload = List.of(ResultadoAnalisisMicroRequestDTO.builder()
                .parametroId(7L)
                .resultado("5")
                .cumple(true)
                .build());

        service.guardarResultados(8L, payload);

        assertThat(evaluacion.getArchivosAdjuntos())
                .extracting(ArchivoEvaluacion::getNombreVisible)
                .contains("Químico", "Microbiológico");
    }
}

