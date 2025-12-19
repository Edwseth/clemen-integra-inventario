package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.DocumentoCalidad;
import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadEstado;
import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadTipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DocumentoCalidadRepository extends JpaRepository<DocumentoCalidad, Long>, JpaSpecificationExecutor<DocumentoCalidad> {

    List<DocumentoCalidad> findByLote_IdAndEstado(Long loteId, DocumentoCalidadEstado estado);

    List<DocumentoCalidad> findByTipoAndEstado(DocumentoCalidadTipo tipo, DocumentoCalidadEstado estado);
}
