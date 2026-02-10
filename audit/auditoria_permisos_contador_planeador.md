# Auditoría BE de permisos efectivos (ROL_CONTADOR / ROL_PLANEADOR)

## Alcance y enfoque
- Auditoría estática del backend (sin cambios funcionales): SecurityFilterChain + method security + validaciones manuales.
- Módulos incluidos: Inventario, Producción, Calidad, Compras (endpoints de OC/proveedores/reportes), BOM y Planeación.

## Evidencia de comandos ejecutados (salida completa)
- `git grep -n "SecurityFilterChain\|HttpSecurity\|authorizeHttpRequests\|requestMatchers"` → `audit/grep_security_chain.txt`
- `git grep -n "@PreAuthorize\|@Secured\|@RolesAllowed"` → `audit/grep_method_security.txt`
- `git grep -n "ROL_CONTADOR\|ROL_PLANEADOR\|hasAnyAuthority\|hasRole\|hasAnyRole"` → `audit/grep_roles.txt`

### Extracto `grep_security_chain.txt` (primeras 20 líneas)
```
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:18:import org.springframework.security.config.annotation.web.builders.HttpSecurity;
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:26:import org.springframework.security.web.SecurityFilterChain;
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:57:    public AuthenticationManager authenticationManager(HttpSecurity http,
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:83:    public SecurityFilterChain securityFilterChain(HttpSecurity http,
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:91:                .authorizeHttpRequests(auth -> {
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:92:                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:94:                    auth.requestMatchers(
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:114:                    auth.requestMatchers(HttpMethod.PATCH, "/api/inventario/productos/*/calidad").hasAnyAuthority(
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:119:                    auth.requestMatchers(HttpMethod.GET, "/api/inventario/productos/**").hasAnyAuthority(
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:125:                    auth.requestMatchers(HttpMethod.GET, "/api/categorias", "/api/categorias/**").hasAnyAuthority(
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:135:                    auth.requestMatchers(HttpMethod.GET,
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:147:                    auth.requestMatchers(HttpMethod.GET, "/api/productos/**").hasAnyAuthority(
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:161:                    auth.requestMatchers(
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:181:                    auth.requestMatchers("/api/inventario/ordenes/**").hasAnyAuthority(
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:187:                    auth.requestMatchers(HttpMethod.GET, "/api/inventario/conteos/**").hasAnyAuthority(
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:195:                    auth.requestMatchers(HttpMethod.POST, "/api/inventario/conteos/*/aplicar").hasAnyAuthority(
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:200:                    auth.requestMatchers("/api/inventario/conteos/**").hasAnyAuthority(
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:207:                    auth.requestMatchers(HttpMethod.GET,
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:217:                    auth.requestMatchers("/api/ordenes-compra/**").hasAnyAuthority(
src/main/java/com/willyes/clemenintegra/shared/security/SecurityConfig.java:224:                    auth.requestMatchers(HttpMethod.GET, "/api/movimientos/**").hasAnyAuthority(
```

### Extracto `grep_method_security.txt` (primeras 20 líneas)
```
README.md:98:Se utiliza Spring Security con autenticación JWT y verificación 2FA. Los roles se gestionan mediante anotaciones `@PreAuthorize` en los controladores y la configuración de `SecurityConfig`. Todas las acciones registran el `usuarioId` y la `fechaIngreso` (por ejemplo en `MovimientoInventario`) para trazabilidad.
docs/abastecimiento/F0-backend-radiografia.md:11:| Módulo | Método & ruta | Roles (@PreAuthorize) | DTO entrada/salida | Filtros/Notas |
docs/bom/README_BOM.md:64:Las restricciones se aplican vía anotaciones `@PreAuthorize` en los controladores BOM y se validan mediante las pruebas de seguridad (`FormulaProductoControllerSecurityTest`).
docs/mrp_export_design.md:62:  - `@PreAuthorize("hasAnyAuthority('ROL_PLANEADOR','ROL_JEFE_PLANEACION','ROL_SUPER_ADMIN')")`
docs/mrp_export_design.md:67:  - `@PreAuthorize("hasAnyAuthority('ROL_PLANEADOR','ROL_JEFE_PLANEACION','ROL_SUPER_ADMIN')")`
src/main/java/com/willyes/clemenintegra/bom/controller/BomProductoController.java:25:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DetalleFormulaController.java:25:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DetalleFormulaController.java:33:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DetalleFormulaController.java:42:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DetalleFormulaController.java:52:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DetalleFormulaController.java:67:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DocumentoFormulaController.java:34:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DocumentoFormulaController.java:40:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DocumentoFormulaController.java:55:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DocumentoFormulaController.java:65:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/FormulaProductoController.java:49:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/FormulaProductoController.java:58:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/FormulaProductoController.java:66:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/FormulaProductoController.java:141:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/FormulaProductoController.java:151:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
```

### Extracto `grep_roles.txt` (primeras 20 líneas)
```
docs/mrp_export_design.md:62:  - `@PreAuthorize("hasAnyAuthority('ROL_PLANEADOR','ROL_JEFE_PLANEACION','ROL_SUPER_ADMIN')")`
docs/mrp_export_design.md:67:  - `@PreAuthorize("hasAnyAuthority('ROL_PLANEADOR','ROL_JEFE_PLANEACION','ROL_SUPER_ADMIN')")`
src/main/java/com/willyes/clemenintegra/bom/controller/BomProductoController.java:25:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DetalleFormulaController.java:25:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DetalleFormulaController.java:33:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DetalleFormulaController.java:42:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DetalleFormulaController.java:52:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DetalleFormulaController.java:67:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DocumentoFormulaController.java:34:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DocumentoFormulaController.java:40:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DocumentoFormulaController.java:55:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/DocumentoFormulaController.java:65:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/FormulaProductoController.java:49:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/FormulaProductoController.java:58:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/FormulaProductoController.java:66:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/FormulaProductoController.java:141:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/FormulaProductoController.java:151:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/FormulaProductoController.java:164:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/FormulaProductoController.java:176:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
src/main/java/com/willyes/clemenintegra/bom/controller/FormulaProductoController.java:183:    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
```

## A) Matriz de permisos BE
- Matriz completa en CSV: `audit/permisos_matriz_contador_planeador.csv` (193 endpoints auditados).
- Columnas: Modulo, Recurso, Operación, Endpoint, Protección, Expresión exacta, ROL_CONTADOR, ROL_PLANEADOR, Notas, Archivo, Línea.

## B) Huecos e inconsistencias detectadas
- Endpoints protegidos **solo por SecurityConfig (sin @PreAuthorize)**: 31.
- Muestras relevantes:
  - `DELETE /api/inventario/bitacora/{id}` → CONTADOR=DENY / PLANEADOR=DENY (src/main/java/com/willyes/clemenintegra/inventario/controller/BitacoraCambiosInventarioController.java:27).
  - `GET /api/inventario/historial-ordenes/orden/{ordenId}` → CONTADOR=ALLOW / PLANEADOR=ALLOW (src/main/java/com/willyes/clemenintegra/inventario/controller/HistorialEstadoOrdenController.java:27).
  - `GET /api/inventario/ordenes/estado` → CONTADOR=DENY / PLANEADOR=DENY (src/main/java/com/willyes/clemenintegra/inventario/controller/OrdenCompraController.java:173).
  - `GET /api/inventario/ordenes/{id}/detalles` → CONTADOR=DENY / PLANEADOR=DENY (src/main/java/com/willyes/clemenintegra/inventario/controller/OrdenCompraController.java:181).
  - `GET /api/inventario/ordenes/{id}/recepciones` → CONTADOR=DENY / PLANEADOR=DENY (src/main/java/com/willyes/clemenintegra/inventario/controller/OrdenCompraController.java:266).
  - `GET /api/inventario/ordenes/{id}/historial` → CONTADOR=DENY / PLANEADOR=DENY (src/main/java/com/willyes/clemenintegra/inventario/controller/OrdenCompraController.java:272).
  - `POST /api/inventario/ordenes-compra-detalle/{ordenCompraId}` → CONTADOR=ALLOW / PLANEADOR=ALLOW (src/main/java/com/willyes/clemenintegra/inventario/controller/OrdenCompraDetalleController.java:34).
  - `GET /api/inventario/ordenes-compra-detalle/{id}` → CONTADOR=ALLOW / PLANEADOR=ALLOW (src/main/java/com/willyes/clemenintegra/inventario/controller/OrdenCompraDetalleController.java:59).
  - `DELETE /api/inventario/ordenes-compra-detalle/{id}` → CONTADOR=ALLOW / PLANEADOR=ALLOW (src/main/java/com/willyes/clemenintegra/inventario/controller/OrdenCompraDetalleController.java:67).
  - `DELETE /api/inventario/tipos-movimiento-detalle/{id}` → CONTADOR=ALLOW / PLANEADOR=ALLOW (src/main/java/com/willyes/clemenintegra/inventario/controller/TipoMovimientoDetalleController.java:35).
  - `GET /api/unidades/{id}` → CONTADOR=ALLOW / PLANEADOR=ALLOW (src/main/java/com/willyes/clemenintegra/inventario/controller/UnidadMedidaController.java:23).
  - `PUT /api/unidades/{id}` → CONTADOR=ALLOW / PLANEADOR=ALLOW (src/main/java/com/willyes/clemenintegra/inventario/controller/UnidadMedidaController.java:34).

- Endpoints con protección duplicada (matcher + @PreAuthorize): predominante en el backend; revisar acoplamiento en FE para evitar falsa expectativa de acceso.
- Endpoints críticos (editar/anular/cambiar estado/aprobar/rechazar) con ALLOW para alguno de los 2 roles: 6.
  - `DELETE /api/inventario/ajustes/{id}` (ANULAR) → CONTADOR=ALLOW, PLANEADOR=DENY [SecurityConfig + @PreAuthorize].
  - `DELETE /api/inventario/ordenes-compra-detalle/{id}` (ANULAR) → CONTADOR=ALLOW, PLANEADOR=ALLOW [SecurityConfig].
  - `DELETE /api/inventario/tipos-movimiento-detalle/{id}` (ANULAR) → CONTADOR=ALLOW, PLANEADOR=ALLOW [SecurityConfig].
  - `PUT /api/unidades/{id}` (EDITAR) → CONTADOR=ALLOW, PLANEADOR=ALLOW [SecurityConfig].
  - `DELETE /api/unidades/{id}` (ANULAR) → CONTADOR=ALLOW, PLANEADOR=ALLOW [SecurityConfig].
  - `GET /api/calidad/lotes/{loteId}/estado-calidad` (CAMBIAR_ESTADO) → CONTADOR=DENY, PLANEADOR=ALLOW [SecurityConfig + @PreAuthorize].

- Inconsistencias de prefijo detectadas (`/api/inventarios` vs `/api/inventario`): **resueltas para SolicitudMovimientoController** migrando el contrato oficial a `/api/inventario/solicitudes`.
  - Se conserva alias temporal en `SolicitudPorOrdenController` para `/por-orden` y `/ordenes` por compatibilidad con FE.

- Control manual a nivel Service detectado: `RetencionLoteServiceImpl` consulta authorities desde `SecurityContextHolder` (validación adicional fuera de anotaciones).

## C) Delta sugerido de alineación FE↔BE (sin implementar)
- FE debe basar visibilidad de acciones en la intersección de: matcher global + @PreAuthorize (no solo en rutas).
- Para endpoints sin @PreAuthorize, FE debe consumir la matriz y no inferir permisos por módulo.
- Normalizar consumo de rutas con prefijos `/api/inventario` y `/api/inventarios` para evitar 404/403 inconsistentes entre ambientes.
- Etiquetar en FE operaciones críticas actualmente ALLOW para CONTADOR/PLANEADOR (ej. unidades, detalles de OC) para revisión funcional con negocio.

## D) Seguimiento de endurecimiento (implementado)
- Endpoints críticos removidos de matcher global de `SecurityConfig` y movidos a `@PreAuthorize` por controlador:
  - `PUT/DELETE /api/unidades/{id}` -> solo `ROL_JEFE_ALMACENES` y `ROL_SUPER_ADMIN`.
  - `DELETE /api/inventario/ordenes-compra-detalle/{id}` -> `ROL_COMPRADOR`, `ROL_JEFE_ALMACENES`, `ROL_SUPER_ADMIN`.
  - `DELETE /api/inventario/tipos-movimiento-detalle/{id}` -> `ROL_JEFE_ALMACENES`, `ROL_SUPER_ADMIN`.
- Reducción de endpoints “solo SecurityConfig”: los endpoints anteriores ahora dependen de anotaciones de método para evitar ALLOW accidental por matchers amplios.
