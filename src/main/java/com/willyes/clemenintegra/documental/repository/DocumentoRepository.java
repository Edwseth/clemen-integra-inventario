package com.willyes.clemenintegra.documental.repository;

import com.willyes.clemenintegra.documental.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DocumentoRepository extends JpaRepository<Documento, Long>, JpaSpecificationExecutor<Documento> {

    boolean existsByCodigoIgnoreCase(String codigo);
}
