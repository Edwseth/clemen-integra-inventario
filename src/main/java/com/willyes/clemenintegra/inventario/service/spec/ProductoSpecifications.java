package com.willyes.clemenintegra.inventario.service.spec;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import com.willyes.clemenintegra.inventario.model.Producto;

public final class ProductoSpecifications {

    private ProductoSpecifications() {}

    public static Specification<Producto> nombreContains(String q) {
        return (root, cq, cb) -> (q == null || q.isBlank())
                ? cb.conjunction()
                : cb.like(cb.upper(root.get("nombre")), "%" + q.trim().toUpperCase() + "%");
    }

    public static Specification<Producto> skuContains(String sku) {
        return (root, cq, cb) -> (sku == null || sku.isBlank())
                ? cb.conjunction()
                : cb.like(cb.upper(root.get("codigoSku")), "%" + sku.trim().toUpperCase() + "%");
    }

    /**
     * Filtra por id de categoría. Si la entidad Producto posee un campo
     * categoriaProductoId simple, se accede directamente; de lo contrario se
     * realiza un join con la entidad relacionada.
     */
    public static Specification<Producto> categoriaProductoIdEquals(Long id) {
        return (root, cq, cb) -> {
            if (id == null) return cb.conjunction();
            try {
                // Versión campo plano:
                return cb.equal(root.get("categoriaProductoId"), id);
            } catch (IllegalArgumentException ex) {
                // Fallback a relación:
                Join<Object, Object> cat = root.join("categoriaProducto");
                return cb.equal(cat.get("id"), id);
            }
        };
    }

    public static Specification<Producto> activoEquals(Boolean activo) {
        return (root, cq, cb) -> (activo == null)
                ? cb.conjunction()
                : (activo ? cb.isTrue(root.get("activo")) : cb.isFalse(root.get("activo")));
    }
}
