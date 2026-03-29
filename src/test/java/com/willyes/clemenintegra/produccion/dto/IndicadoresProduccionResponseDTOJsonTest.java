package com.willyes.clemenintegra.produccion.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class IndicadoresProduccionResponseDTOJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializaCamposOriginalesYAliasesCompatibles() throws Exception {
        IndicadoresProduccionResponseDTO dto = IndicadoresProduccionResponseDTO.builder()
                .cantidadTotalPlanificada(new BigDecimal("123.45"))
                .cantidadTotalProducida(new BigDecimal("67.89"))
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        assertThat(json.get("cantidadTotalPlanificada").decimalValue()).isEqualByComparingTo("123.45");
        assertThat(json.get("cantidadTotalProducida").decimalValue()).isEqualByComparingTo("67.89");
        assertThat(json.get("cantidadPlanificadaTotal").decimalValue()).isEqualByComparingTo("123.45");
        assertThat(json.get("cantidadProducidaTotal").decimalValue()).isEqualByComparingTo("67.89");
    }
}
