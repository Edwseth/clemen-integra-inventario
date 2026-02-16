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

    public static Specification<LoteProducto> conProductoNombreOrSkuLike(String texto) {
        return (root, query, cb) -> {
            if (texto == null || texto.isBlank()) return cb.conjunction();

            var producto = root.join("producto", JoinType.LEFT);
            String termino = texto.trim().toUpperCase();

            if (termino.contains("-")) {
                String[] partes = termino.contains(" - ")
                        ? termino.split("\\s-\\s", 2)
                        : termino.split("-", 2);

                String skuPart = partes.length > 0 ? partes[0].trim() : "";
                String nombrePart = partes.length > 1 ? partes[1].trim() : "";

                if (!skuPart.isBlank() && !nombrePart.isBlank()) {
                    return cb.or(
                            cb.like(cb.upper(producto.get("codigoSku")), "%" + skuPart + "%"),
                            cb.like(cb.upper(producto.get("nombre")), "%" + nombrePart + "%")
                    );
                }
            }

            return cb.or(
                    cb.like(cb.upper(producto.get("codigoSku")), "%" + termino + "%"),
                    cb.like(cb.upper(producto.get("nombre")), "%" + termino + "%")
            );
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
