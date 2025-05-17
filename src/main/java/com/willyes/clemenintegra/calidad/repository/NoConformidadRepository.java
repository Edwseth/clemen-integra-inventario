package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.NoConformidad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoConformidadRepository extends JpaRepository<NoConformidad, Long> {
    boolean existsByCodigo(String codigo);
}