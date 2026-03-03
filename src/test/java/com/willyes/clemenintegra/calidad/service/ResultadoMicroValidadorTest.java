package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.model.ParametroAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.enums.TipoResultadoAnalisis;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultadoMicroValidadorTest {

    @Test
    void validaNumericosConOperadores() {
        ParametroAnalisisMicrobiologico parametro = ParametroAnalisisMicrobiologico.builder()
                .tipoResultado(TipoResultadoAnalisis.NUMERICO)
                .especificacion("<= 1000")
                .build();

        Boolean cumple = ResultadoMicroValidador.calcularCumplimiento(parametro, "950", null);

        assertThat(cumple).isTrue();
    }

    @Test
    void validaLimiteConMilesCuandoEsMenor() {
        ParametroAnalisisMicrobiologico parametro = ParametroAnalisisMicrobiologico.builder()
                .tipoResultado(TipoResultadoAnalisis.NUMERICO)
                .especificacion("<10.000 UFC/mL")
                .build();

        Boolean cumple = ResultadoMicroValidador.calcularCumplimiento(parametro, "1000", null);

        assertThat(cumple).isTrue();
    }

    @Test
    void validaLimiteConMilesCuandoEsCasiIgual() {
        ParametroAnalisisMicrobiologico parametro = ParametroAnalisisMicrobiologico.builder()
                .tipoResultado(TipoResultadoAnalisis.NUMERICO)
                .especificacion("<10.000 UFC/mL")
                .build();

        Boolean cumple = ResultadoMicroValidador.calcularCumplimiento(parametro, "9999", null);

        assertThat(cumple).isTrue();
    }

    @Test
    void validaLimiteConMilesCuandoEsIgualNoCumplePorOperadorEstricto() {
        ParametroAnalisisMicrobiologico parametro = ParametroAnalisisMicrobiologico.builder()
                .tipoResultado(TipoResultadoAnalisis.NUMERICO)
                .especificacion("<10.000 UFC/mL")
                .build();

        Boolean cumple = ResultadoMicroValidador.calcularCumplimiento(parametro, "10000", null);

        assertThat(cumple).isFalse();
    }

    @Test
    void validaNumericoConUnidadComoRegresion() {
        ParametroAnalisisMicrobiologico parametro = ParametroAnalisisMicrobiologico.builder()
                .tipoResultado(TipoResultadoAnalisis.NUMERICO)
                .especificacion("< 3 NMP/mL")
                .build();

        Boolean cumple = ResultadoMicroValidador.calcularCumplimiento(parametro, "2", null);

        assertThat(cumple).isTrue();
    }

    @Test
    void validaPresenciaAusencia() {
        ParametroAnalisisMicrobiologico parametro = ParametroAnalisisMicrobiologico.builder()
                .tipoResultado(TipoResultadoAnalisis.PRESENCIA_AUSENCIA)
                .especificacion("Ausencia en 1 g")
                .build();

        Boolean cumple = ResultadoMicroValidador.calcularCumplimiento(parametro, "presencia", true);

        assertThat(cumple).isFalse();
    }

    @Test
    void validaPresenciaAusenciaComoRegresion() {
        ParametroAnalisisMicrobiologico parametro = ParametroAnalisisMicrobiologico.builder()
                .tipoResultado(TipoResultadoAnalisis.PRESENCIA_AUSENCIA)
                .especificacion("AUSENCIA")
                .build();

        Boolean cumple = ResultadoMicroValidador.calcularCumplimiento(parametro, "AUSENCIA", false);

        assertThat(cumple).isTrue();
    }
}
