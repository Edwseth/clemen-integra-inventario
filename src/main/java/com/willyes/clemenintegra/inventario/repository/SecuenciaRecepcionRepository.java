package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.SecuenciaRecepcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SecuenciaRecepcionRepository extends JpaRepository<SecuenciaRecepcion, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SecuenciaRecepcion s where s.anio = :anio and s.prefijo = :prefijo")
    Optional<SecuenciaRecepcion> findByAnioAndPrefijoForUpdate(@Param("anio") int anio, @Param("prefijo") String prefijo);

    Optional<SecuenciaRecepcion> findByAnioAndPrefijo(int anio, String prefijo);
}
