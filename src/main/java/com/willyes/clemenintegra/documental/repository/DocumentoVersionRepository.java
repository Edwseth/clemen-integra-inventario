package com.willyes.clemenintegra.documental.repository;

import com.willyes.clemenintegra.documental.model.DocumentoVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentoVersionRepository extends JpaRepository<DocumentoVersion, Long> {

    List<DocumentoVersion> findByDocumentoIdOrderByNumeroVersionDesc(Long documentoId);

    Optional<DocumentoVersion> findFirstByDocumentoIdAndVigenteTrue(Long documentoId);

    Optional<DocumentoVersion> findTopByDocumentoIdOrderByNumeroVersionDesc(Long documentoId);

    @Query("select max(v.numeroVersion) from DocumentoVersion v where v.documento.id = :documentoId")
    Optional<Integer> findMaxNumeroVersionByDocumentoId(@Param("documentoId") Long documentoId);
}
