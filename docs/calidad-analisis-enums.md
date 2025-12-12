# Calidad - Fuente de verdad de análisis

Esta guía resume las reglas vigentes para análisis de calidad en el ERP.

- Las banderas booleanas por disciplina (`requiereAnalisisFisico`, `requiereAnalisisQuimico`, `requiereAnalisisMicrobiologico`) son la única fuente de verdad para determinar requerimientos y flujos de negocio.
- El enum `TipoAnalisisCalidad` se mantiene únicamente como campo **LEGACY** para compatibilidad (filtros/DTOs), siempre derivado a partir de las banderas.
- Ningún flujo de negocio debe tomar decisiones basadas en `TipoAnalisisCalidad` directamente.
