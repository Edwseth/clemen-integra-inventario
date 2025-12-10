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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultadoAnalisisMicroServiceImplTest {

    @Mock
    private EvaluacionCalidadRepository evaluacionRepository;

    @Mock
    private ResultadoAnalisisMicrobiologicoRepository resultadoRepository;

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
        EvaluacionCalidad evaluacion = EvaluacionCalidad.builder().id(3L).loteProducto(lote)
                .fechaEvaluacion(LocalDateTime.now()).build();

        when(evaluacionRepository.findById(3L)).thenReturn(Optional.of(evaluacion));
        when(resultadoRepository.findByEvaluacionId(3L)).thenReturn(List.of());
        ArgumentCaptor<List<ResultadoAnalisisMicrobiologico>> captor = ArgumentCaptor.forClass(List.class);
        when(resultadoRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

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
}

