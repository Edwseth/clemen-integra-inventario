package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.CapaArchivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CapaArchivoRepository extends JpaRepository<CapaArchivo, Long> {
    List<CapaArchivo> findByCapa_Id(Long capaId);

    List<CapaArchivo> findByCapa_IdIn(List<Long> capaIds);

    Optional<CapaArchivo> findByIdAndCapa_Id(Long archivoId, Long capaId);
}
