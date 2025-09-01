package com.willyes.clemenintegra.produccion.service.spec;

import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Utilidades para construir Specification de {@link OrdenProduccion} de forma
 * null-safe.
 */
public final class OrdenProduccionSpecifications {

    private OrdenProduccionSpecifications() {
    }

    public static Specification<OrdenProduccion> byCodigo(String codigo) {
        return (root, query, cb) -> {
            if (codigo == null || codigo.isBlank()) return cb.conjunction();
            return cb.like(cb.upper(root.get("codigoOrden")), "%" + codigo.trim().toUpperCase() + "%");
        };
    }

    public static Specification<OrdenProduccion> byEstado(EstadoProduccion estado) {
        return (root, query, cb) -> estado == null ? cb.conjunction() : cb.equal(root.get("estado"), estado);
    }

    public static Specification<OrdenProduccion> byResponsable(String responsable) {
        return (root, query, cb) -> {
            if (responsable == null || responsable.isBlank()) return cb.conjunction();
            var r = root.join("responsable", JoinType.LEFT);
            String like = "%" + responsable.trim().toUpperCase() + "%";
            return cb.or(
                    cb.like(cb.upper(r.get("nombreUsuario")), like),
                    cb.like(cb.upper(r.get("nombreCompleto")), like)
            );
        };
    }

    public static Specification<OrdenProduccion> byFechaBetween(LocalDateTime inicio, LocalDateTime fin) {
        return (root, query, cb) -> {
            if (inicio == null && fin == null) return cb.conjunction();
            if (inicio != null && fin != null) return cb.between(root.get("fechaInicio"), inicio, fin);
            if (inicio != null) return cb.greaterThanOrEqualTo(root.get("fechaInicio"), inicio);
            return cb.lessThanOrEqualTo(root.get("fechaInicio"), fin);
        };
    }

    @SafeVarargs
    public static Specification<OrdenProduccion> and(Specification<OrdenProduccion>... specs) {
        Specification<OrdenProduccion> result = Specification.where(null);
        if (specs != null) {
            for (Specification<OrdenProduccion> spec : specs) {
                if (spec != null) {
                    result = (result == null) ? Specification.where(spec) : result.and(spec);
                }
            }
        }
        return result == null ? (root, query, cb) -> cb.conjunction() : result;
    }
}

