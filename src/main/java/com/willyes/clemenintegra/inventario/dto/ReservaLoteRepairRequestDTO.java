package com.willyes.clemenintegra.inventario.dto;

import java.util.List;

public record ReservaLoteRepairRequestDTO(List<Long> detalleIds,
                                          Long ordenProduccionId,
                                          Integer batchSize) {
}
