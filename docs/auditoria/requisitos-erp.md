# Clemen Integra ERP
## Matriz de Requisitos (URS)

Fuente: Plantilla_URS_ERP_Todos_los_Modulos.xlsx

Este documento contiene los requisitos funcionales del ERP Clemen Integra.
Se utiliza como base para auditorías técnicas y funcionales del sistema.

Los requisitos se agrupan por módulo y servirán como base para generar
la **Matriz de Trazabilidad (RTM)** del sistema.

---

# Requisitos Transversales

| Req ID | Descripción | Estado |
|------|-------------|-------|
| SEC-01 | El sistema debe requerir autenticación para acceder | PENDIENTE |
| SEC-02 | El sistema debe gestionar roles y permisos | PENDIENTE |
| SEC-04 | El sistema debe registrar auditoría de acciones | PENDIENTE |
| SEC-05 | El sistema debe proteger información sensible | PENDIENTE |

---

# Módulo Inventarios

| Req ID | Descripción | Estado |
|------|-------------|-------|
| INV-TR-TR-GEN-001 | El sistema debe permitir transferencias entre almacenes | PENDIENTE |
| INV-TR-TR-GEN-002 | El sistema debe registrar movimientos de inventario | PENDIENTE |
| INV-TR-TR-GEN-003 | El sistema debe validar disponibilidad de stock | PENDIENTE |
| INV-TR-TR-GEN-004 | El sistema debe registrar lote del producto | PENDIENTE |
| INV-TR-TR-GEN-005 | El sistema debe permitir consultar historial de movimientos | PENDIENTE |

---

# Módulo Producción

| Req ID | Descripción | Estado |
|------|-------------|-------|
| PROD-001 | El sistema debe permitir crear órdenes de producción | PENDIENTE |
| PROD-002 | El sistema debe validar disponibilidad de insumos | PENDIENTE |
| PROD-003 | El sistema debe registrar consumo de materias primas | PENDIENTE |
| PROD-004 | El sistema debe registrar etapas de producción | PENDIENTE |
| PROD-005 | El sistema debe permitir cierre de orden de producción | PENDIENTE |

---

# Módulo BOM

| Req ID | Descripción | Estado |
|------|-------------|-------|
| BOM-001 | El sistema debe permitir definir fórmulas de productos | PENDIENTE |
| BOM-002 | El sistema debe permitir versionar fórmulas | PENDIENTE |
| BOM-003 | El sistema debe permitir consultar insumos requeridos | PENDIENTE |
| BOM-004 | El sistema debe permitir validar fórmula activa | PENDIENTE |

---

# Módulo Calidad

| Req ID | Descripción | Estado |
|------|-------------|-------|
| QC-001 | El sistema debe registrar evaluaciones de calidad | PENDIENTE |
| QC-002 | El sistema debe permitir adjuntar documentos de análisis | PENDIENTE |
| QC-003 | El sistema debe permitir aprobar o rechazar lotes | PENDIENTE |
| QC-004 | El sistema debe bloquear lotes no liberados | PENDIENTE |
| QC-005 | El sistema debe registrar trazabilidad de evaluaciones | PENDIENTE |

---

# Módulo Compras

| Req ID | Descripción | Estado |
|------|-------------|-------|
| COM-001 | El sistema debe permitir crear órdenes de compra | PENDIENTE |
| COM-002 | El sistema debe registrar proveedor | PENDIENTE |
| COM-003 | El sistema debe registrar recepción de mercancía | PENDIENTE |
| COM-004 | El sistema debe controlar cantidades recibidas | PENDIENTE |

---

# Módulo Usuarios

| Req ID | Descripción | Estado |
|------|-------------|-------|
| USR-001 | El sistema debe permitir crear usuarios | PENDIENTE |
| USR-002 | El sistema debe permitir asignar roles | PENDIENTE |
| USR-003 | El sistema debe permitir activar/desactivar usuarios | PENDIENTE |

---

# Módulo Ayuda

| Req ID | Descripción | Estado |
|------|-------------|-------|
| HELP-001 | El sistema debe proporcionar documentación por módulo | PENDIENTE |
| HELP-002 | El sistema debe permitir acceder a ayuda contextual | PENDIENTE |

---

# Centro de Comunicación

| Req ID | Descripción | Estado |
|------|-------------|-------|
| COMC-001 | El sistema debe permitir notificaciones internas | PENDIENTE |
| COMC-002 | El sistema debe registrar mensajes del sistema | PENDIENTE |

---

# Uso de este documento

Este documento sirve para:

1. Auditoría funcional del ERP
2. Verificación de cobertura de requisitos
3. Identificación de funcionalidades faltantes
4. Generación de matriz de trazabilidad (RTM)

---

# Relación con el código

Los requisitos deben ser trazables contra los repositorios oficiales.

## Backend


src/main/java/com/willyes/clemenintegra/modules/inventario
src/main/java/com/willyes/clemenintegra/modules/produccion
src/main/java/com/willyes/clemenintegra/modules/calidad
src/main/java/com/willyes/clemenintegra/modules/compras
src/main/java/com/willyes/clemenintegra/modules/bom
src/main/java/com/willyes/clemenintegra/modules/usuarios


## Frontend


src/modules/inventarios
src/modules/produccion
src/modules/calidad
src/modules/compras
src/modules/bom
src/modules/usuarios


---

# Criterios de Auditoría

Durante la auditoría cada requisito será clasificado como:

| Estado | Definición |
|------|-------------|
| IMPLEMENTADO | Existe endpoint backend + lógica + interfaz frontend |
| PARCIAL | Existe backend pero no interfaz o viceversa |
| NO_IMPLEMENTADO | No existe implementación |
| NO_APLICA | El requisito no aplica a la versión actual |

---

# Próximo paso

Generar la **Matriz de Trazabilidad (RTM)**:


docs/auditoria/rtm-inventarios.md
docs/auditoria/rtm-produccion.md
docs/auditoria/rtm-calidad.md
docs/auditoria/rtm-compras.md
docs/auditoria/rtm-bom.md


Estos documentos permitirán auditar si cada requisito está implementado
en el backend y frontend del ERP.