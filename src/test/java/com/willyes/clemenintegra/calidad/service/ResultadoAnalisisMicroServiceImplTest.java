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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        when(evaluacionRepository.save(any(EvaluacionCalidad.class))).thenAnswer(invocation -> invocation.getArgument(0));

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

    @Test
    void generaPdfMicroCuandoNoExisteAdjuntoPrevio() {
        PlantillaAnalisisMicrobiologico plantilla = PlantillaAnalisisMicrobiologico.builder()
                .id(20L)
                .build();
        ParametroAnalisisMicrobiologico parametro = ParametroAnalisisMicrobiologico.builder()
                .id(15L)
                .plantilla(plantilla)
                .nombreEnsayo("Aerobios")
                .tipoResultado(TipoResultadoAnalisis.TEXTO)
                .orden(1)
                .build();
        plantilla.setParametros(List.of(parametro));

        Producto producto = Producto.builder().id(5).nombre("Prod C").plantillaAnalisisMicrobiologico(plantilla).build();
        LoteProducto lote = LoteProducto.builder().id(9L).producto(producto).codigoLote("L-3").build();
        EvaluacionCalidad evaluacion = EvaluacionCalidad.builder()
                .id(12L)
                .loteProducto(lote)
                .tipoEvaluacion(com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .archivosAdjuntos(new java.util.ArrayList<>())
                .fechaEvaluacion(LocalDateTime.now())
                .build();

        when(evaluacionRepository.findById(12L)).thenReturn(Optional.of(evaluacion));
        when(resultadoRepository.findByEvaluacionId(12L)).thenReturn(List.of());
        when(resultadoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(analisisMicroPdfService.generarPdf(12L)).thenReturn("pdf".getBytes());
        when(evaluacionRepository.save(any(EvaluacionCalidad.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var payload = List.of(ResultadoAnalisisMicroRequestDTO.builder()
                .parametroId(15L)
                .resultado("Ok")
                .cumple(true)
                .build());

        service.guardarResultados(12L, payload);

        assertThat(evaluacion.getArchivosAdjuntos())
                .filteredOn(a -> "Microbiológico".equalsIgnoreCase(a.getNombreVisible()))
                .hasSize(1);
    }

    @Test
    void devuelveAdjuntoMicroExistenteSinRegenerar() throws Exception {
        String nombreArchivo = "L-4_MICRO_prueba.pdf";
        Path uploadRoot = Paths.get(System.getProperty("user.dir"), "uploads", "evaluaciones");
        Files.createDirectories(uploadRoot);
        Files.writeString(uploadRoot.resolve(nombreArchivo), "contenido");

        EvaluacionCalidad evaluacion = EvaluacionCalidad.builder()
                .id(30L)
                .archivosAdjuntos(List.of(ArchivoEvaluacion.builder()
                        .nombreArchivo(nombreArchivo)
                        .nombreVisible("Microbiológico")
                        .build()))
                .build();

        when(evaluacionRepository.findById(30L)).thenReturn(Optional.of(evaluacion));

        byte[] pdf = service.obtenerPdfMicro(30L);

        assertThat(new String(pdf)).isEqualTo("contenido");
        verifyNoInteractions(analisisMicroPdfService);
    }

    @Test
    void regeneraAdjuntoMicroCuandoNoExisteArchivoPeroHayResultados() {
        EvaluacionCalidad evaluacion = EvaluacionCalidad.builder()
                .id(40L)
                .archivosAdjuntos(new java.util.ArrayList<>())
                .tipoEvaluacion(com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .loteProducto(LoteProducto.builder().codigoLote("L-5").producto(new Producto()).build())
                .build();

        ResultadoAnalisisMicrobiologico resultado = ResultadoAnalisisMicrobiologico.builder()
                .id(100L)
                .evaluacion(evaluacion)
                .build();

        when(evaluacionRepository.findById(40L)).thenReturn(Optional.of(evaluacion));
        when(resultadoRepository.findByEvaluacionId(40L)).thenReturn(List.of(resultado));
        when(analisisMicroPdfService.generarPdf(40L)).thenReturn("nuevoPdf".getBytes());
        when(evaluacionRepository.save(any(EvaluacionCalidad.class))).thenAnswer(invocation -> invocation.getArgument(0));

        byte[] resultadoPdf = service.obtenerPdfMicro(40L);

        assertThat(resultadoPdf).isEqualTo("nuevoPdf".getBytes());
        verify(analisisMicroPdfService).generarPdf(40L);
        verify(evaluacionRepository, times(1)).save(any(EvaluacionCalidad.class));
        verify(resultadoRepository, times(1)).findByEvaluacionId(40L);
        assertThat(evaluacion.getArchivosAdjuntos())
                .filteredOn(a -> "Microbiológico".equalsIgnoreCase(a.getNombreVisible()))
                .hasSize(1);
    }

    @Test
    void lanzaNotFoundCuandoNoHayResultadosMicro() {
        EvaluacionCalidad evaluacion = EvaluacionCalidad.builder()
                .id(55L)
                .build();

        when(evaluacionRepository.findById(55L)).thenReturn(Optional.of(evaluacion));
        when(resultadoRepository.findByEvaluacionId(55L)).thenReturn(List.of());

        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.obtenerPdfMicro(55L));

        verify(analisisMicroPdfService, never()).generarPdf(anyLong());
        verify(evaluacionRepository, never()).save(any(EvaluacionCalidad.class));
    }

    @Test
    void guardaCumpleCorrectamenteParaEspecificacionConMiles() {
        PlantillaAnalisisMicrobiologico plantilla = PlantillaAnalisisMicrobiologico.builder()
                .id(30L)
                .nombre("Plantilla Micro")
                .build();
        ParametroAnalisisMicrobiologico parametro = ParametroAnalisisMicrobiologico.builder()
                .id(50L)
                .plantilla(plantilla)
                .nombreEnsayo("Mesófilos")
                .tipoResultado(TipoResultadoAnalisis.NUMERICO)
                .especificacion("<10.000 UFC/mL")
                .orden(1)
                .build();
        plantilla.setParametros(List.of(parametro));

        Producto producto = Producto.builder().id(7).nombre("Prod Micro").plantillaAnalisisMicrobiologico(plantilla).build();
        LoteProducto lote = LoteProducto.builder().id(21L).producto(producto).codigoLote("L-10").build();
        EvaluacionCalidad evaluacion = EvaluacionCalidad.builder()
                .id(61L)
                .loteProducto(lote)
                .tipoEvaluacion(com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .archivosAdjuntos(new java.util.ArrayList<>())
                .fechaEvaluacion(LocalDateTime.now())
                .build();

        when(evaluacionRepository.findById(61L)).thenReturn(Optional.of(evaluacion));
        when(resultadoRepository.findByEvaluacionId(61L)).thenReturn(List.of());
        when(resultadoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(analisisMicroPdfService.generarPdf(61L)).thenReturn("pdf".getBytes());
        when(evaluacionRepository.save(any(EvaluacionCalidad.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var payload = List.of(ResultadoAnalisisMicroRequestDTO.builder()
                .parametroId(50L)
                .resultado("1000")
                .build());

        var res = service.guardarResultados(61L, payload);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getCumple()).isTrue();
    }

}

