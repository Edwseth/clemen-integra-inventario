package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.DisciplinaEstado;
import com.willyes.clemenintegra.calidad.service.ArchivoEvaluacionConstants;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AnalisisCalidadHelperTest {

    @Test
    void validaSoloFisico() {
        Producto producto = new Producto();
        producto.setRequiereAnalisisFisico(true);
        LoteProducto lote = new LoteProducto();
        lote.setProducto(producto);

        AnalisisCalidadHelper.ResultadoValidacionDisciplinas resultado = AnalisisCalidadHelper
                .validarDisciplinasCompletas(lote, List.of(), id -> false);

        assertThat(resultado.esValido()).isFalse();
        assertThat(resultado.getPrimerMensaje()).isEqualTo("Falta evaluación física");

        EvaluacionCalidad evalFisico = EvaluacionCalidad.builder()
                .tipoEvaluacion(TipoEvaluacion.FISICO)
                .build();

        resultado = AnalisisCalidadHelper.validarDisciplinasCompletas(lote, List.of(evalFisico), id -> false);
        assertThat(resultado.esValido()).isTrue();
    }

    @Test
    void validaQuimicoYMicro() {
        Producto producto = new Producto();
        producto.setRequiereAnalisisQuimico(true);
        producto.setRequiereAnalisisMicrobiologico(true);
        LoteProducto lote = new LoteProducto();
        lote.setProducto(producto);

        EvaluacionCalidad evaluacionQuimica = EvaluacionCalidad.builder()
                .id(3L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .archivosAdjuntos(List.of(ArchivoEvaluacion.builder()
                        .nombreVisible(ArchivoEvaluacionConstants.NOMBRE_VISIBLE_MICRO)
                        .build()))
                .build();

        AnalisisCalidadHelper.ResultadoValidacionDisciplinas resultado = AnalisisCalidadHelper
                .validarDisciplinasCompletas(lote, List.of(evaluacionQuimica), id -> false);

        assertThat(resultado.esValido()).isFalse();
        assertThat(resultado.isFaltanResultadosMicro()).isTrue();

        resultado = AnalisisCalidadHelper
                .validarDisciplinasCompletas(lote, List.of(evaluacionQuimica), id -> id == 3L);

        assertThat(resultado.esValido()).isTrue();
    }

    @Test
    void validaSinRequerimientos() {
        Producto producto = new Producto();
        LoteProducto lote = new LoteProducto();
        lote.setProducto(producto);

        AnalisisCalidadHelper.ResultadoValidacionDisciplinas resultado = AnalisisCalidadHelper
                .validarDisciplinasCompletas(lote, List.of(), id -> false);

        assertThat(resultado.esValido()).isTrue();
        assertThat(resultado.getPrimerMensaje()).isNull();
    }

    @Test
    void validaFisicoQuimicoMicroCompletos() {
        Producto producto = new Producto();
        producto.setRequiereAnalisisFisico(true);
        producto.setRequiereAnalisisQuimico(true);
        producto.setRequiereAnalisisMicrobiologico(true);

        LoteProducto lote = new LoteProducto();
        lote.setProducto(producto);

        EvaluacionCalidad evalFisico = EvaluacionCalidad.builder()
                .tipoEvaluacion(TipoEvaluacion.FISICO)
                .build();
        EvaluacionCalidad evalMicro = EvaluacionCalidad.builder()
                .id(15L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(ResultadoEvaluacion.CONDICIONADO)
                .archivosAdjuntos(List.of(ArchivoEvaluacion.builder()
                        .nombreVisible(ArchivoEvaluacionConstants.NOMBRE_VISIBLE_MICRO)
                        .build()))
                .build();

        AnalisisCalidadHelper.ResultadoValidacionDisciplinas resultado = AnalisisCalidadHelper
                .validarDisciplinasCompletas(lote, List.of(evalFisico, evalMicro), id -> id == 15L);

        assertThat(resultado.esValido()).isTrue();
    }

    @Test
    void validaMicroSinResultadosBloqueaLiberacion() {
        Producto producto = new Producto();
        producto.setRequiereAnalisisMicrobiologico(true);
        LoteProducto lote = new LoteProducto();
        lote.setProducto(producto);

        EvaluacionCalidad evalMicro = EvaluacionCalidad.builder()
                .id(99L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .archivosAdjuntos(List.of(ArchivoEvaluacion.builder()
                        .nombreVisible(ArchivoEvaluacionConstants.NOMBRE_VISIBLE_MICRO)
                        .build()))
                .build();

        AnalisisCalidadHelper.ResultadoValidacionDisciplinas resultado = AnalisisCalidadHelper
                .validarDisciplinasCompletas(lote, List.of(evalMicro), id -> false);

        assertThat(resultado.esValido()).isFalse();
        assertThat(resultado.getPrimerMensaje()).isEqualTo("Faltan resultados microbiológicos");
    }

    @Test
    void validaMicroSinPdfDevuelveMensajeCorrecto() {
        Producto producto = new Producto();
        producto.setRequiereAnalisisMicrobiologico(true);
        LoteProducto lote = new LoteProducto();
        lote.setProducto(producto);

        EvaluacionCalidad evalMicro = EvaluacionCalidad.builder()
                .id(100L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .build();

        AnalisisCalidadHelper.ResultadoValidacionDisciplinas resultado = AnalisisCalidadHelper
                .validarDisciplinasCompletas(lote, List.of(evalMicro), id -> id == 100L);

        assertThat(resultado.esValido()).isFalse();
        assertThat(resultado.getPrimerMensaje()).isEqualTo("Falta PDF microbiológico");
    }

    @Test
    void calculaEstadoNoRequiere() {
        var estado = AnalisisCalidadHelper.calcularEstadoEvaluacion(false, false, false, false, false, false);

        assertThat(estado.name()).isEqualTo("NO_REQUIERE");
    }

    @Test
    void calculaEstadoEvaluadoCuandoTodoCompleto() {
        var estado = AnalisisCalidadHelper.calcularEstadoEvaluacion(true, true, true, true, true, true);

        assertThat(estado.name()).isEqualTo("EVALUADO");
    }

    @Test
    void calculaEstadoPendienteCuandoFaltaUnaDisciplina() {
        var estado = AnalisisCalidadHelper.calcularEstadoEvaluacion(true, false, true, true, true, true);

        assertThat(estado.name()).isEqualTo("PENDIENTE");
    }

    @Test
    void calculaEstadoPendienteSoloMicroRequeridoSinResultados() {
        var estado = AnalisisCalidadHelper.calcularEstadoEvaluacion(false, false, false, false, true, false);

        assertThat(estado.name()).isEqualTo("PENDIENTE");
    }

    @Test
    void calculaCodigoAnalisisParaTodasLasCombinaciones() {
        assertThat(AnalisisCalidadHelper.calcularCodigoAnalisis(true, true, true)).isEqualTo("FQM");
        assertThat(AnalisisCalidadHelper.calcularCodigoAnalisis(false, true, true)).isEqualTo("QM");
        assertThat(AnalisisCalidadHelper.calcularCodigoAnalisis(true, false, true)).isEqualTo("FM");
        assertThat(AnalisisCalidadHelper.calcularCodigoAnalisis(true, true, false)).isEqualTo("FQ");
        assertThat(AnalisisCalidadHelper.calcularCodigoAnalisis(true, false, false)).isEqualTo("F");
        assertThat(AnalisisCalidadHelper.calcularCodigoAnalisis(false, true, false)).isEqualTo("Q");
        assertThat(AnalisisCalidadHelper.calcularCodigoAnalisis(false, false, true)).isEqualTo("M");
        assertThat(AnalisisCalidadHelper.calcularCodigoAnalisis(false, false, false)).isEqualTo("—");
    }

    @Test
    void calculaEstadoMicroNoRequerido() {
        Producto producto = new Producto();

        var estado = AnalisisCalidadHelper.calcularEstadoMicro(producto, List.of(), Set.of());

        assertThat(estado.estado()).isEqualTo(DisciplinaEstado.NO_REQUERIDO);
        assertThat(estado.tieneResultadosMicro()).isFalse();
        assertThat(estado.tienePdfMicro()).isFalse();
    }

    @Test
    void calculaEstadoMicroPendienteSinResultados() {
        Producto producto = new Producto();
        producto.setRequiereAnalisisMicrobiologico(true);

        EvaluacionCalidad evalMicro = EvaluacionCalidad.builder()
                .id(1L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .build();

        var estado = AnalisisCalidadHelper.calcularEstadoMicro(producto, List.of(evalMicro), Set.of());

        assertThat(estado.estado()).isEqualTo(DisciplinaEstado.PENDIENTE);
        assertThat(estado.tieneResultadosMicro()).isFalse();
        assertThat(estado.tienePdfMicro()).isFalse();
    }

    @Test
    void calculaEstadoMicroEvaluadoConResultadosYPdf() {
        Producto producto = new Producto();
        producto.setRequiereAnalisisMicrobiologico(true);

        EvaluacionCalidad evalMicro = EvaluacionCalidad.builder()
                .id(2L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .archivosAdjuntos(List.of(ArchivoEvaluacion.builder()
                        .nombreVisible(ArchivoEvaluacionConstants.NOMBRE_VISIBLE_MICRO)
                        .build()))
                .build();

        var estado = AnalisisCalidadHelper.calcularEstadoMicro(producto, List.of(evalMicro), Set.of(2L));

        assertThat(estado.estado()).isEqualTo(DisciplinaEstado.EVALUADO);
        assertThat(estado.tieneResultadosMicro()).isTrue();
        assertThat(estado.tienePdfMicro()).isTrue();
    }
}
