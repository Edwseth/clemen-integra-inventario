package com.willyes.clemenintegra.inventario.service.spec;

import com.willyes.clemenintegra.inventario.model.AjusteInventario;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AjusteInventarioSpecifications {
    private AjusteInventarioSpecifications() {}

    public static Specification<AjusteInventario> hasProductoId(Long productoId) {
        return (root, query, cb) -> (productoId == null)
                ? cb.conjunction()
                : cb.equal(root.get("producto").get("id"), productoId.intValue());
    }

    public static Specification<AjusteInventario> hasAlmacenId(Long almacenId) {
        return (root, query, cb) -> (almacenId == null)
                ? cb.conjunction()
                : cb.equal(root.get("almacen").get("id"), almacenId.intValue());
    }

    public static Specification<AjusteInventario> fechaBetween(LocalDate fechaInicio, LocalDate fechaFin) {
        return (root, query, cb) -> {
            if (fechaInicio == null && fechaFin == null) {
                return cb.conjunction();
            }

            LocalDateTime inicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
            LocalDateTime fin = fechaFin != null ? fechaFin.atTime(23, 59, 59, 999_999_999);

            if (inicio != null && fin != null) {
                return cb.between(root.get("fecha"), inicio, fin);
            }
            if (inicio != null) {
                return cb.greaterThanOrEqualTo(root.get("fecha"), inicio);
            }
            return cb.lessThanOrEqualTo(root.get("fecha"), fin);
        };
    }
}
