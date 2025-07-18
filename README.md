# Clemen-Integra ERP – Backend

Clemen-Integra es un ERP para laboratorios de productos homeopáticos y suplementos alimenticios. El backend está desarrollado en Java e integra los módulos de Inventario, Producción, Calidad, Fórmulas (BOM) y Seguridad con autenticación 2FA.

## Módulos Funcionales
- **Inventario:** control de productos, almacenes, lotes, movimientos y órdenes de compra.
- **Producción:** gestión de órdenes, etapas y trazabilidad de lotes.
- **Calidad:** registro de no conformidades, acciones correctivas/preventivas y liberación de lotes.
- **Fórmulas (BOM):** definición de recetas e insumos por producto.
- **Seguridad:** control de usuarios, roles y autenticación con JWT y código 2FA.

## Arquitectura Técnica
El proyecto sigue una arquitectura **monolito modular evolutivo**. Cada dominio funcional se implementa en paquetes independientes bajo `com.willyes.clemenintegra`:

```
com.willyes.clemenintegra
├── inventario
├── produccion
├── calidad
├── bom
└── shared    (configuración común, seguridad y excepciones)
```

## Tecnologías Principales
- Spring Boot 3
- Spring Security y JWT
- Autenticación 2FA
- Jakarta Validation
- Lombok y MapStruct
- JPA (Hibernate)
- SLF4J para registros
- MySQL

## Patrones Aplicados
- Inyección de dependencias por constructor.
- Separación de interfaces para servicios y repositorios.
- Centralización de manejo de excepciones.

## Estructura de Paquetes
Cada módulo mantiene un patrón uniforme:
- `controller` – expone los endpoints REST.
- `service` y `service/impl` – lógica de negocio y validaciones.
- `repository` – interfaces JPA.
- `dto` – objetos de transferencia para peticiones y respuestas.
- `mapper` – conversiones con MapStruct.
- `model` – entidades del dominio.
- `security` – filtros y configuración de Spring Security.

## Calidad de Código
El código ha sido refactorizado mediante **Codex Workspace**, mejorando la legibilidad y eliminando clases obsoletas.

## Entidades Principales
**Inventario:** Producto, LoteProducto, MovimientoInventario, Almacen, OrdenCompra, HistorialEstadoOrden, AjusteInventario, BitacoraCambiosInventario, CategoriaProducto, Proveedor, TipoMovimientoDetalle, MotivoMovimiento, UnidadMedida.

**Producción:** OrdenProduccion, DetalleEtapa, EtapaProduccion, ControlCalidadProceso, Produccion.

**Calidad:** EvaluacionCalidad, Capa, NoConformidad, ChecklistCalidad, ItemChecklist, EspecificacionCalidad, RetencionLote.

**BOM:** FormulaProducto, DetalleFormula, DocumentoFormula.

## API REST Principal
### Inventario
- **Productos** `/api/productos` – CRUD y `/reporte-stock` (Excel).
- **Lotes** `/api/lotes` – CRUD, `/reporte-vencimiento` y `/reporte-alertas` (Excel).
- **Movimientos** `/api/movimientos` – registrar y consultar con filtros `productoId`, `almacenId`, `tipoMovimiento`, `clasificacion`, `fechaInicio`, `fechaFin`. Exportación en `/reporte-excel`.
- **Ajustes de inventario** `/api/inventario/ajustes` – listar, crear y eliminar.
- **Reportes** `/api/reportes` – alta/baja rotación (`fechaInicio`, `fechaFin`), productos más costosos (`categoria`), trazabilidad por lote (`codigoLote`), productos en retención o liberación (`estadoLote`, `desde`, `hasta`), no conformidades (`tipo`, `area`, `desde`, `hasta`), CAPA (`estado`, `desde`, `hasta`), stock actual, productos por vencer, alertas de inventario y movimientos. Todos devuelven Excel.

### Producción
- **Órdenes de producción** `/api/produccion/ordenes` – CRUD.

### Calidad
- **Evaluaciones de calidad** `/api/calidad/evaluaciones` – CRUD de evaluaciones.

### BOM
- **Fórmulas de producto** `/api/bom/formulas` – CRUD de recetas.

## Seguridad
Se utiliza Spring Security con autenticación JWT y verificación 2FA. Los roles se gestionan mediante anotaciones `@PreAuthorize` en los controladores y la configuración de `SecurityConfig`. Todas las acciones registran el `usuarioId` y la `fechaIngreso` (por ejemplo en `MovimientoInventario`) para trazabilidad.

## Requisitos de Compilación y Despliegue
- JDK 21
- Maven 3.8+
- Base de datos MySQL en funcionamiento

Configurar las credenciales de base de datos en `src/main/resources/application.properties` mediante las variables `DB_USERNAME` y `DB_PASS`.

## Puesta en Marcha
1. Clonar el repositorio y configurar MySQL.
2. Definir `DB_USERNAME` y `DB_PASS` en `application.properties`.
3. Ejecutar:
```bash
mvn clean install
mvn spring-boot:run
```
La aplicación inicia por defecto en el puerto `8080` y la documentación Swagger se encuentra en `/swagger-ui.html`.
