package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.LoteConsecutivoDia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.Optional;

public interface LoteConsecutivoDiaRepository extends JpaRepository<LoteConsecutivoDia, LocalDate> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LoteConsecutivoDia l where l.fecha = :fecha")
    Optional<LoteConsecutivoDia> findByFechaForUpdate(@Param("fecha") LocalDate fecha);
}
