# Reporte de inventario RBAC backend (diagnóstico)

## Resumen ejecutivo
- Se inventariaron **270 endpoints** en controladores: INV (124), QC (70), PROD (33), BOM (19), PO (9), DOC (6), ADMIN (4), AUTH (3), HEALTH (1), OTROS (1).  
- Hay fuerte dependencia de autorización por **roles hardcodeados (`ROL_*`)**: 168 endpoints con `@PreAuthorize` solo por rol; 44 con mezcla rol+permiso; 58 sin anotación de método/clase (normalmente cubiertos por `SecurityConfig` y/o `authenticated`).
- El backend sí construye authorities con **rol + permisos** en cada request autenticado (JWT no trae permisos en claims; se consultan en BD al autenticar el token).
- Catálogo RBAC actual (según migraciones): **35 permisos activos**: BOM=1, DOC=1, INV=12, PO=1, PROD=19, QC=1.
- El gap principal está en **QC, PO, DOC, BOM e INV/PROD para operaciones WRITE/WORKFLOW/EXPORT granulares**.

## A) Matriz obligatoria endpoint->módulo->operación->seguridad actual->permiso propuesto
- Matriz completa (270 filas): `audit/rbac_endpoint_matrix.md`.
- Convenciones usadas en operación: `READ`, `WRITE`, `EXPORT`, `DECIDE`, `WORKFLOW_START`, `WORKFLOW_FINISH`.
- La propuesta de permiso se estandarizó por módulo/operación (ejemplo: `INV_WRITE`, `QC_EXPORT`, `PROD_WORKFLOW`, etc.).

## Inventario de seguridad actual
- Regla global: salvo rutas públicas explícitas, `/api/**` requiere autenticación.
- Administración RBAC: `/api/admin/rbac/**` exige `ROL_SUPER_ADMIN`.
- En controladores hay coexistencia de:
  - `@PreAuthorize(hasAnyAuthority('ROL_*', ...))` (predominante).
  - `@PreAuthorize` mixto (`ROL_*` + permisos `INV_*`, `PROD_*`, `CONTROL_DOCUMENTAL_WRITE`).
  - Endpoints sin `@PreAuthorize` (dependen de `SecurityConfig` por path o de regla `authenticated`).

## B) Catálogo actual de permisos y faltantes

### Permisos existentes (agrupados por módulo)
- Ver detalle completo en `audit/rbac_permisos_actuales.md`.
- Conteo vigente: BOM=1, DOC=1, INV=12, PO=1, PROD=19, QC=1.

### Permisos faltantes necesarios para cubrir endpoints reales (propuesta de catálogo)
- **INV**: `INV_WRITE`, `INV_EXPORT`, `INV_WORKFLOW`, `INV_WORKFLOW_START`, `INV_WORKFLOW_FINISH`, `INV_DECIDE`.
- **QC**: `QC_WRITE`, `QC_EXPORT`, `QC_WORKFLOW`, `QC_WORKFLOW_FINISH`.
- **PO**: `PO_WRITE`, `PO_EXPORT`, `PO_WORKFLOW_FINISH`.
- **DOC**: `DOC_WRITE`, `DOC_WORKFLOW`.
- **BOM**: `BOM_WRITE`, `BOM_EXPORT`, `BOM_WORKFLOW`.
- **PROD**: `PROD_WRITE`, `PROD_EXPORT`, `PROD_WORKFLOW` (manteniendo los granulares existentes donde ya aplican).
- **ADMIN**: `ADMIN_WRITE` (para sustituir dependencia exclusiva de rol en administración RBAC, si se decide permiso explícito).

> Nota: esta propuesta es de **catálogo objetivo**; no implica migraciones en este corte.

## C) Gaps identificados
1. **Endpoints sin anotación de método/clase (`@PreAuthorize`)**: 58. No necesariamente abiertos, pero dependen de reglas URL globales y eso dificulta trazabilidad fina por endpoint.
2. **Uso extensivo de rol hardcodeado**: 168 endpoints dependen solo de `ROL_*`; esto bloquea delegación fina por permisos.
3. **Inconsistencia de granularidad**:
   - PROD ya tiene permisos granulares (`PROD_*`) en varios endpoints.
   - INV mezcla permisos y roles.
   - QC/PO/BOM/DOC están mayormente por roles pese a tener muchos endpoints reales.
4. **Riesgo de mantenimiento**: reglas repartidas entre `SecurityConfig` (path-based) y `@PreAuthorize` (method-based), con duplicidades por ruta/HTTP.

## 3) Cómo se construyen authorities en JWT/Auth
- El token JWT contiene `rol`, `usuarioId`, `sessionVersion`; **no incluye lista de permisos**.
- En cada request con bearer token, el `JwtAuthenticationProvider` valida token, carga usuario desde BD y construye authorities vía `UsuarioAuthoritiesService`.
- `UsuarioAuthoritiesService` agrega:
  1) rol principal `ROL_*`, y
  2) permisos activos por `usuarios_roles -> roles_permisos -> permisos` (fallback a `usuarios.rol` si no hay relación cargada).
- Implicación de refresco/caching:
  - Como authorities se reconstruyen al autenticar cada request, cambios en `roles_permisos`/`usuarios_roles` se reflejan sin relogin estricto.
  - Persisten riesgos de sesión por `sessionVersion`/inactividad, pero no hay caché explícita de permisos en memoria dentro de este flujo.

## 4) Tablas de seguridad y consistencia
- Estructura RBAC presente: `roles`, `permisos`, `roles_permisos`, `usuarios_roles`.
- `permisos` modela `codigo`, `modulo`, `accion`, `activo`; `roles_permisos` y `usuarios_roles` son N:M con PK compuesta.
- Subrepresentación de módulos en catálogo actual:
  - **QC=1** permiso para un módulo con 70 endpoints.
  - **PO=1** con 9 endpoints.
  - **DOC=1** con 6 endpoints.
  - **BOM=1** con 19 endpoints.

## D) Orden de migración recomendado (único)
1. **Inventarios (INV) primero**.
2. Producción (PROD).
3. Calidad (QC).
4. Planeación (PO).
5. BOM.
6. Documental (DOC).
7. Admin RBAC.

**Primer corte sugerido: INV**.
- Razones:
  - Mayor superficie (124 endpoints) y alta criticidad operativa.
  - Ya existe base parcial de permisos INV (`INV_*`) para extender, no iniciar desde cero.
  - Permite validar patrón end-to-end (controlador + SecurityConfig + matriz permisos) antes de pasar a módulos con workflows más complejos (QC/PROD).

## E) Checklist DoD del inventario
- [x] Endpoints listados por método y path (matriz completa).
- [x] Mapeo por módulo y tipo de operación.
- [x] Seguridad actual registrada (anotación/clase/sin anotación).
- [x] Identificado origen de authorities y flujo JWT/Auth.
- [x] Revisión de tablas RBAC y consistencia estructural.
- [x] Catálogo actual agrupado por módulo.
- [x] Catálogo propuesto de faltantes (sin migraciones).
- [x] Lista de gaps priorizados.
- [x] Recomendación única de orden de migración y primer corte.
