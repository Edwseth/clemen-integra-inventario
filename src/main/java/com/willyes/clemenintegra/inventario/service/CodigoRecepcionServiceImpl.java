package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.SecuenciaRecepcion;
import com.willyes.clemenintegra.inventario.repository.SecuenciaRecepcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodigoRecepcionServiceImpl implements CodigoRecepcionService {

    private static final DateTimeFormatter PREFIJO_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private final SecuenciaRecepcionRepository repository;
    private final PlatformTransactionManager transactionManager;

    @Override
    @Transactional
    public String generarCodigo(LocalDate fechaNegocio) {
        if (fechaNegocio == null) {
            throw new IllegalArgumentException("La fecha de negocio es obligatoria");
        }
        int anio = fechaNegocio.getYear();
        String prefijo = fechaNegocio.format(PREFIJO_FORMATTER);

        SecuenciaRecepcion secuencia = repository.findByAnioAndPrefijoForUpdate(anio, prefijo)
                .orElse(null);
        if (secuencia == null) {
            crearSecuenciaInicial(anio, prefijo);
            secuencia = repository.findByAnioAndPrefijoForUpdate(anio, prefijo)
                    .orElseThrow(() -> new IllegalStateException("No fue posible inicializar la secuencia de recepción"));
        }

        long siguiente = secuencia.getSecuenciaActual() == null ? 1L : secuencia.getSecuenciaActual() + 1L;
        secuencia.setSecuenciaActual(siguiente);
        secuencia.setActualizadoEn(LocalDateTime.now());
        String codigo = String.format("RC-%s-%02d", prefijo, siguiente);
        log.debug("Secuencia recepción actualizada anio={} prefijo={} valor={}", anio, prefijo, siguiente);
        return codigo;
    }

    private void crearSecuenciaInicial(int anio, String prefijo) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        template.execute(status -> {
            try {
                SecuenciaRecepcion nueva = SecuenciaRecepcion.builder()
                        .anio(anio)
                        .prefijo(prefijo)
                        .secuenciaActual(0L)
                        .build();
                nueva.setActualizadoEn(LocalDateTime.now());
                repository.saveAndFlush(nueva);
            } catch (DataIntegrityViolationException ex) {
                status.setRollbackOnly();
                log.debug("Colisión creando secuencia recepción anio={} prefijo={}, reintentando", anio, prefijo, ex);
            }
            return null;
        });
    }
}
