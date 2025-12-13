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
    void validaPresenciaAusencia() {
        ParametroAnalisisMicrobiologico parametro = ParametroAnalisisMicrobiologico.builder()
                .tipoResultado(TipoResultadoAnalisis.PRESENCIA_AUSENCIA)
                .especificacion("Ausencia en 1 g")
                .build();

        Boolean cumple = ResultadoMicroValidador.calcularCumplimiento(parametro, "presencia", true);

        assertThat(cumple).isFalse();
    }
}
