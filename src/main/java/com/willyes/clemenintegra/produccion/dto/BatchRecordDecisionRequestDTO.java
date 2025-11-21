package com.willyes.clemenintegra.produccion.dto;

import com.willyes.clemenintegra.produccion.model.enums.EstadoBatchRecord;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BatchRecordDecisionRequestDTO {

    @NotNull
    private EstadoBatchRecord decision;

    @Size(max = 2000)
    private String observacionesCalidad;
}
