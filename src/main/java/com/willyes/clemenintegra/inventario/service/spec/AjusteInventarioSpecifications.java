package com.willyes.clemenintegra.inventario.service.spec;

import com.willyes.clemenintegra.inventario.model.AjusteInventario;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class AjusteInventarioSpecifications {

    private AjusteInventarioSpecifications() {
    }

    public static Specification<AjusteInventario> hasProductoId(Long productoId) {
        return (root, query, cb) -> {
            if (productoId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("producto").get("id"), productoId);
        };
    }

    public static Specification<AjusteInventario> hasAlmacenId(Long almacenId) {
        return (root, query, cb) -> {
            if (almacenId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("almacen").get("id"), almacenId);
        };
    }

    public static Specification<AjusteInventario> fechaBetween(LocalDate fechaInicio, LocalDate fechaFin) {
        return (root, query, cb) -> {
            if (fechaInicio == null && fechaFin == null) {
                return cb.conjunction();
            }

            LocalDateTime inicio = null;
            if (fechaInicio != null) {
                inicio = fechaInicio.atStartOfDay();
            }

            LocalDateTime fin = null;
            if (fechaFin != null) {
                fin = fechaFin.atTime(LocalTime.MAX);
            }

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
