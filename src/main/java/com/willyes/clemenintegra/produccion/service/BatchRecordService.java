package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.BatchRecordDTO;
import com.willyes.clemenintegra.produccion.dto.BatchRecordDecisionRequestDTO;
import org.springframework.security.core.Authentication;

public interface BatchRecordService {
    BatchRecordDTO buildByOrdenProduccion(Long ordenProduccionId);

    void decidirBatchRecord(Long ordenProduccionId,
                            BatchRecordDecisionRequestDTO request,
                            Authentication auth);
}
