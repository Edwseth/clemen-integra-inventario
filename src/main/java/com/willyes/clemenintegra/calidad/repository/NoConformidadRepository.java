package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.OrigenNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.TipoIncidente;
import com.willyes.clemenintegra.calidad.dto.NoConformidadDetalleDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NoConformidadRepository extends JpaRepository<NoConformidad, Long> {
    boolean existsByCodigo(String codigo);

    Optional<NoConformidad> findFirstByCodigoStartingWithOrderByCodigoDesc(String codigoPrefix);

    @EntityGraph(attributePaths = {"lote", "producto", "evaluacion"})
    Page<NoConformidad> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"lote", "producto", "evaluacion"})
    Optional<NoConformidad> findById(Long id);

    @EntityGraph(attributePaths = {"lote", "producto", "evaluacion"})
    Page<NoConformidad> findBySeveridad(SeveridadNoConformidad severidad, Pageable pageable);

    @EntityGraph(attributePaths = {"lote", "producto", "evaluacion"})
    Page<NoConformidad> findByOrigen(OrigenNoConformidad origen, Pageable pageable);

    @EntityGraph(attributePaths = {"lote", "producto", "evaluacion"})
    Page<NoConformidad> findBySeveridadAndOrigen(SeveridadNoConformidad severidad,
                                                 OrigenNoConformidad origen,
                                                 Pageable pageable);

    @EntityGraph(attributePaths = {"lote", "producto", "evaluacion"})
    Page<NoConformidad> findByTipoIncidente(TipoIncidente tipoIncidente, Pageable pageable);

    @EntityGraph(attributePaths = {"lote", "producto", "evaluacion"})
    Page<NoConformidad> findBySeveridadAndTipoIncidente(SeveridadNoConformidad severidad,
                                                        TipoIncidente tipoIncidente,
                                                        Pageable pageable);

    @EntityGraph(attributePaths = {"lote", "producto", "evaluacion"})
    Page<NoConformidad> findByOrigenAndTipoIncidente(OrigenNoConformidad origen,
                                                     TipoIncidente tipoIncidente,
                                                     Pageable pageable);

    @EntityGraph(attributePaths = {"lote", "producto", "evaluacion"})
    Page<NoConformidad> findBySeveridadAndOrigenAndTipoIncidente(SeveridadNoConformidad severidad,
                                                                 OrigenNoConformidad origen,
                                                                 TipoIncidente tipoIncidente,
                                                                 Pageable pageable);

    Optional<NoConformidad> findFirstByLote_IdAndEstadoOrderByFechaRegistroDesc(Long loteId, EstadoNoConformidad estado);

    Optional<NoConformidad> findFirstByLote_IdAndEvaluacion_IdAndEstado(Long loteId,
                                                                        Long evaluacionId,
                                                                        EstadoNoConformidad estado);

    java.util.List<NoConformidad> findByLote_Id(Long loteId);

    @Query(value = "SELECT n.id as id, n.codigo as codigo, n.origen as origen, n.severidad as severidad, " +
            "n.tipoIncidente as tipoIncidente, n.estado as estado, n.descripcion as descripcion, " +
            "n.evidencia as evidencia, n.fechaRegistro as fechaRegistro, n.fechaCierre as fechaCierre, " +
            "u.id as usuarioReportaId, l.id as loteId, p.id as productoId, e.id as evaluacionId, " +
            "l.codigoLote as codigoLote, u.nombreCompleto as reportadoPorNombre, p.nombre as productoNombre, " +
            "n.creadoPor as creadoPor, n.actualizadoPor as actualizadoPor, n.actualizadoEn as actualizadoEn " +
            "FROM NoConformidad n " +
            "JOIN n.usuarioReporta u " +
            "LEFT JOIN n.lote l " +
            "LEFT JOIN n.producto p " +
            "LEFT JOIN n.evaluacion e " +
            "WHERE (:severidad IS NULL OR n.severidad = :severidad) " +
            "AND (:origen IS NULL OR n.origen = :origen) " +
            "AND (:tipoIncidente IS NULL OR n.tipoIncidente = :tipoIncidente)",
            countQuery = "SELECT COUNT(n.id) FROM NoConformidad n " +
                    "WHERE (:severidad IS NULL OR n.severidad = :severidad) " +
                    "AND (:origen IS NULL OR n.origen = :origen) " +
                    "AND (:tipoIncidente IS NULL OR n.tipoIncidente = :tipoIncidente)")
    Page<NoConformidadListadoProjection> findListado(@Param("severidad") SeveridadNoConformidad severidad,
                                                     @Param("origen") OrigenNoConformidad origen,
                                                     @Param("tipoIncidente") TipoIncidente tipoIncidente,
                                                     Pageable pageable);

    @Query("""
        SELECT 
                n.id as id,
                n.codigo as codigo,
                n.origen as origen,
                n.severidad as severidad,
                n.tipoIncidente as tipoIncidente,
                n.estado as estado,
                n.descripcion as descripcion,
                n.evidencia as evidencia,
                n.fechaRegistro as fechaRegistro,
                n.fechaCierre as fechaCierre,
                u.id as usuarioReportaId,
                l.id as loteId,
                p.id as productoId,
                e.id as evaluacionId,
                l.codigoLote as codigoLote,
                u.nombreCompleto as reportadoPorNombre,
                p.nombre as productoNombre,
                n.creadoPor as creadoPor,
                n.actualizadoPor as actualizadoPor,
                n.actualizadoEn as actualizadoEn
            FROM NoConformidad n
            JOIN n.usuarioReporta u
            LEFT JOIN n.lote l
            LEFT JOIN n.producto p
            LEFT JOIN n.evaluacion e
            WHERE n.id = :id
        """)
    Optional<NoConformidadDetalleProjection> findDetalleProjectionById(@Param("id") Long id);
}
