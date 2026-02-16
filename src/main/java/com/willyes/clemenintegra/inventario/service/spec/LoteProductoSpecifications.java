package com.willyes.clemenintegra.inventario.service.spec;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.JoinType;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import java.time.LocalDateTime;

public final class LoteProductoSpecifications {
    private LoteProductoSpecifications() {}

    public static Specification<LoteProducto> productoNombreContains(String texto) {
        return (root, query, cb) -> {
            if (texto == null || texto.isBlank()) return cb.conjunction();
            var p = root.join("producto", JoinType.LEFT);
            return cb.like(cb.upper(p.get("nombre")), "%" + texto.trim().toUpperCase() + "%");
        };
    }

    public static Specification<LoteProducto> equalsEstado(EstadoLote estado) {
        return (root, query, cb) -> (estado == null) ? cb.conjunction() : cb.equal(root.get("estado"), estado);
    }

    public static Specification<LoteProducto> conProductoId(Long productoId) {
        return (root, query, cb) -> (productoId == null)
                ? cb.conjunction()
                : cb.equal(root.get("producto").get("id"), productoId);
    }

    public static Specification<LoteProducto> almacenNombreContains(String texto) {
        return (root, query, cb) -> {
            if (texto == null || texto.isBlank()) return cb.conjunction();
            var a = root.join("almacen", JoinType.LEFT);
            return cb.like(cb.upper(a.get("nombre")), "%" + texto.trim().toUpperCase() + "%");
        };
    }

    public static Specification<LoteProducto> conAlmacenId(Long almacenId) {
        return (root, query, cb) -> (almacenId == null)
                ? cb.conjunction()
                : cb.equal(root.get("almacen").get("id"), almacenId);
    }

    public static Specification<LoteProducto> fechaVencimientoAntesDe(LocalDateTime fecha) {
        return (root, query, cb) -> (fecha == null) ? cb.conjunction() : cb.lessThan(root.get("fechaVencimiento"), fecha);
    }
}
