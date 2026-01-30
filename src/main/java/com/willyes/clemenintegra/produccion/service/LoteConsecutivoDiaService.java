package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.LoteConsecutivoDia;
import com.willyes.clemenintegra.produccion.repository.LoteConsecutivoDiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LoteConsecutivoDiaService {

    private static final int MAX_CONSECUTIVO = 999;

    private final LoteConsecutivoDiaRepository repository;

    @Transactional
    public int obtenerSiguienteConsecutivo(LocalDate fecha) {
        return repository.findByFechaForUpdate(fecha)
                .map(this::incrementarConsecutivo)
                .orElseGet(() -> crearConsecutivoInicial(fecha));
    }

    private int crearConsecutivoInicial(LocalDate fecha) {
        try {
            repository.saveAndFlush(LoteConsecutivoDia.builder()
                    .fecha(fecha)
                    .ultimoValor(0)
                    .build());
            return 0;
        } catch (DataIntegrityViolationException ex) {
            return repository.findByFechaForUpdate(fecha)
                    .map(this::incrementarConsecutivo)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT, "CONSECUTIVO_DIARIO_EXCEDIDO", ex));
        }
    }

    private int incrementarConsecutivo(LoteConsecutivoDia consecutivo) {
        int nuevoValor = consecutivo.getUltimoValor() + 1;
        if (nuevoValor > MAX_CONSECUTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CONSECUTIVO_DIARIO_EXCEDIDO");
        }
        consecutivo.setUltimoValor(nuevoValor);
        repository.save(consecutivo);
        return nuevoValor;
    }
}
