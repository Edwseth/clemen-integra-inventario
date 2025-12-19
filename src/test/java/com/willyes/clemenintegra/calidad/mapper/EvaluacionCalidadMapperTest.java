package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.ArchivoEvaluacionDTO;
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

        EvaluacionCalidadDetalleDTO dto = mapper.toDetalleDTO(evaluacion, producto,
                List.of(evaluacion), java.util.Set.of(5L), resultadosMicro);

        assertThat(dto.getIdEvaluacion()).isEqualTo(5L);
        assertThat(dto.getCodigoLote()).isEqualTo("L001");
        assertThat(dto.isRequiereAnalisisFisico()).isTrue();
        assertThat(dto.isRequiereAnalisisQuimico()).isTrue();
        assertThat(dto.isRequiereAnalisisQuimicoMicro()).isTrue();
        assertThat(dto.isRequiereAnalisisMicrobiologico()).isTrue();
        assertThat(dto.isTieneEvaluacionFisica()).isFalse();
        assertThat(dto.isTieneEvaluacionQuimicaMicro()).isTrue();
        assertThat(dto.isTieneResultadosQuimicos()).isTrue();
        assertThat(dto.isTieneResultadosMicro()).isTrue();
        assertThat(dto.getResultadosMicro()).hasSize(1);
        assertThat(dto.getArchivosAdjuntos()).hasSize(1);
    }

    @Test
    void consolidaEvaluacionesQMConIdsYAdjuntos() {
        Producto producto = new Producto();
        producto.setNombre("Producto B");
        producto.setRequiereAnalisisFisico(false);
        producto.setRequiereAnalisisQuimico(true);
        producto.setRequiereAnalisisMicrobiologico(true);

        LoteProducto lote = new LoteProducto();
        lote.setId(20L);
        lote.setCodigoLote("L002");
        lote.setProducto(producto);
        lote.setEstado(com.willyes.clemenintegra.inventario.model.enums.EstadoLote.EN_CUARENTENA);

        var usuario = new com.willyes.clemenintegra.shared.model.Usuario();
        usuario.setNombreCompleto("Micro");

        EvaluacionCalidad evalQuimicoMicro = EvaluacionCalidad.builder()
                .id(30L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .usuarioEvaluador(usuario)
                .fechaEvaluacion(LocalDateTime.now())
                .archivosAdjuntos(List.of(
                        ArchivoEvaluacion.builder()
                                .nombreArchivo("quim.pdf")
                                .nombreVisible("Químico")
                                .build(),
                        ArchivoEvaluacion.builder()
                                .nombreArchivo("micro.pdf")
                                .nombreVisible("Microbiológico")
                                .build()))
                .build();

        var dto = mapper.toConsolidadoDTO(lote, List.of(evalQuimicoMicro),
                java.util.Set.of(30L), java.util.Map.of(30L, Boolean.TRUE));

        assertThat(dto.getEvaluacionQuimicoMicroId()).isEqualTo(30L);
        assertThat(dto.isEvaluacionesRequeridasCompletas()).isTrue();
        assertThat(dto.getAdjuntosQuimicoMicro()).extracting(ArchivoEvaluacionDTO::getNombreVisible)
                .containsExactly("Químico", "Microbiológico");
        assertThat(dto.isTieneResultadosMicro()).isTrue();
        assertThat(dto.isTieneAdjuntosQuimicoMicro()).isTrue();
        assertThat(dto.isTienePdfMicro()).isTrue();
        assertThat(dto.isTienePdfQuimico()).isTrue();
        assertThat(dto.getCodigoAnalisis()).isEqualTo("QM");
        assertThat(dto.getQuimicoConforme()).isTrue();
        assertThat(dto.getMicroConforme()).isTrue();
        assertThat(dto.getFisicoConforme()).isNull();
    }

    @Test
    void consolidaResultadosMicroSinPdfMicro() {
        Producto producto = new Producto();
        producto.setNombre("Producto C");
        producto.setRequiereAnalisisFisico(false);
        producto.setRequiereAnalisisQuimico(true);
        producto.setRequiereAnalisisMicrobiologico(true);

        LoteProducto lote = new LoteProducto();
        lote.setId(25L);
        lote.setCodigoLote("L003");
        lote.setProducto(producto);
        lote.setEstado(com.willyes.clemenintegra.inventario.model.enums.EstadoLote.EN_CUARENTENA);

        var usuario = new com.willyes.clemenintegra.shared.model.Usuario();
        usuario.setNombreCompleto("Micro");

        EvaluacionCalidad evalQuimicoMicro = EvaluacionCalidad.builder()
                .id(31L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .usuarioEvaluador(usuario)
                .fechaEvaluacion(LocalDateTime.now())
                .archivosAdjuntos(List.of(ArchivoEvaluacion.builder()
                        .nombreArchivo("quim.pdf")
                        .nombreVisible("Químico")
                        .build()))
                .build();

        var dto = mapper.toConsolidadoDTO(lote, List.of(evalQuimicoMicro),
                java.util.Set.of(31L), java.util.Map.of(31L, Boolean.TRUE));

        assertThat(dto.isTieneResultadosMicro()).isTrue();
        assertThat(dto.isTieneAdjuntosQuimicoMicro()).isFalse();
        assertThat(dto.isTienePdfMicro()).isFalse();
        assertThat(dto.isTienePdfQuimico()).isTrue();
        assertThat(dto.getMicroConforme()).isTrue();
    }
}
