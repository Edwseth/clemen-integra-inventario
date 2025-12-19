package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.DocumentoCalidadVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentoCalidadVersionRepository extends JpaRepository<DocumentoCalidadVersion, Long> {

    List<DocumentoCalidadVersion> findByDocumento_IdOrderByVersionDesc(Long documentoId);

    Optional<DocumentoCalidadVersion> findTopByDocumento_IdOrderByVersionDesc(Long documentoId);

    List<DocumentoCalidadVersion> findByDocumento_IdInOrderByVersionDesc(List<Long> documentoIds);
}
