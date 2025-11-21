package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.BatchRecordDTO;

public interface BatchRecordService {
    BatchRecordDTO buildByOrdenProduccion(Long ordenProduccionId);
}
