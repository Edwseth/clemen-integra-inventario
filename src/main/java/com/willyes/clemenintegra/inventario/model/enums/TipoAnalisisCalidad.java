package com.willyes.clemenintegra.inventario.model.enums;

/**
 * Define qué tipo de evaluación de calidad aplica al producto.
 * <ul>
 *     <li>NINGUNO: el lote no requiere evaluaciones de calidad.</li>
 *     <li>FISICO: equivale al rótulo de interfaz "FISICO_QUIMICO" e indica solo análisis físico/químico.</li>
 *     <li>QUIMICO_MICROBIOLOGICO: equivale al rótulo "MICROBIOLOGICO" e indica solo análisis microbiológico.</li>
 *     <li>AMBOS: se requieren los dos (físico/químico y microbiológico).</li>
 * </ul>
 */
public enum TipoAnalisisCalidad {
    NINGUNO,
    FISICO,
    QUIMICO_MICROBIOLOGICO,
    AMBOS;

    public static TipoAnalisisCalidad fromFlags(boolean fisico, boolean quimico, boolean micro) {
        if (!fisico && !quimico && !micro) {
            return NINGUNO;
        }
        boolean anyQm = quimico || micro;
        if (fisico && anyQm) {
            return AMBOS;
        }
        if (fisico) {
            return FISICO;
        }
        // Para compatibilidad legacy, cualquier combinación sin físico pero con químico y/o micro
        return QUIMICO_MICROBIOLOGICO;
    }
}
