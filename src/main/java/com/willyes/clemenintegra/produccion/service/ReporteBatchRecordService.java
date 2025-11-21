package com.willyes.clemenintegra.produccion.service;

public interface ReporteBatchRecordService {

    byte[] generarPdfBatchRecord(Long ordenProduccionId);
}
