package com.willyes.clemenintegra.inventario.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TipoAnalisisCalidadTest {

    @Test
    @DisplayName("fromFlags mapea NINGUNO cuando todas las banderas son falsas")
    void fromFlagsNinguno() {
        assertThat(TipoAnalisisCalidad.fromFlags(false, false, false))
                .isEqualTo(TipoAnalisisCalidad.NINGUNO);
    }

    @Test
    @DisplayName("fromFlags mapea FISICO cuando solo fisico es verdadero")
    void fromFlagsFisico() {
        assertThat(TipoAnalisisCalidad.fromFlags(true, false, false))
                .isEqualTo(TipoAnalisisCalidad.FISICO);
    }

    @Test
    @DisplayName("fromFlags mapea QUIMICO_MICROBIOLOGICO cuando solo quimico o micro son verdaderos")
    void fromFlagsQuimicoMicro() {
        assertThat(TipoAnalisisCalidad.fromFlags(false, true, false))
                .isEqualTo(TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO);
        assertThat(TipoAnalisisCalidad.fromFlags(false, false, true))
                .isEqualTo(TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO);
    }

    @Test
    @DisplayName("fromFlags mapea AMBOS cuando se requiere físico y otra disciplina")
    void fromFlagsAmbos() {
        assertThat(TipoAnalisisCalidad.fromFlags(true, true, false))
                .isEqualTo(TipoAnalisisCalidad.AMBOS);
        assertThat(TipoAnalisisCalidad.fromFlags(true, false, true))
                .isEqualTo(TipoAnalisisCalidad.AMBOS);
        assertThat(TipoAnalisisCalidad.fromFlags(true, true, true))
                .isEqualTo(TipoAnalisisCalidad.AMBOS);
    }
}
