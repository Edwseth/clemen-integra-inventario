package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadDetalleDTO;
import com.willyes.clemenintegra.calidad.dto.ResultadoAnalisisMicroDetalleDTO;
import com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluacionCalidadMapperTest {

    private final EvaluacionCalidadMapper mapper = new EvaluacionCalidadMapper();

    @Test
    void debeConstruirDetalleConMicroResultados() {
        Producto producto = new Producto();
        producto.setNombre("Producto A");
        producto.setRequiereAnalisisFisico(true);
        producto.setRequiereAnalisisQuimico(true);
        producto.setRequiereAnalisisMicrobiologico(true);

        LoteProducto lote = new LoteProducto();
        lote.setId(10L);
        lote.setCodigoLote("L001");
        lote.setProducto(producto);

        EvaluacionCalidad evaluacion = EvaluacionCalidad.builder()
                .id(5L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .observaciones("Todo OK")
                .fechaEvaluacion(LocalDateTime.now())
                .loteProducto(lote)
                .archivosAdjuntos(List.of(ArchivoEvaluacion.builder()
                        .nombreArchivo("archivo.pdf")
                        .nombreVisible("Archivo")
                        .build()))
                .build();

        List<ResultadoAnalisisMicroDetalleDTO> resultadosMicro = List.of(
                ResultadoAnalisisMicroDetalleDTO.builder()
                        .parametroId(1L)
                        .nombreEnsayo("Ensayo 1")
                        .resultado("10")
                        .build()
        );

        EvaluacionCalidadDetalleDTO dto = mapper.toDetalleDTO(evaluacion, producto, resultadosMicro);

        assertThat(dto.getIdEvaluacion()).isEqualTo(5L);
        assertThat(dto.getCodigoLote()).isEqualTo("L001");
        assertThat(dto.isRequiereAnalisisFisico()).isTrue();
        assertThat(dto.isRequiereAnalisisQuimico()).isTrue();
        assertThat(dto.isRequiereAnalisisMicrobiologico()).isTrue();
        assertThat(dto.isTieneResultadosQuimicos()).isTrue();
        assertThat(dto.isTieneResultadosMicro()).isTrue();
        assertThat(dto.getResultadosMicro()).hasSize(1);
        assertThat(dto.getArchivosAdjuntos()).hasSize(1);
    }
}
