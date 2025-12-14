package com.willyes.clemenintegra.produccion.service;

public interface ReportesProduccionService {

    byte[] generarBatchRecordExcel(Long ordenProduccionId);

    byte[] generarBatchRecordPdf(Long ordenProduccionId);
}

