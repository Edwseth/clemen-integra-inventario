package com.willyes.clemenintegra.inventario.dto;

import java.util.List;

public record ReservaLoteRepairResultDTO(int totalEvaluadas,
                                         int actualizadas,
                                         int sinCambios,
                                         int pendientes,
                                         List<Long> reservasActualizadas) {
}
