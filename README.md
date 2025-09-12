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
- **Productos** `/api/productos` – CRUD.
  - El identificador de catálogo se expone ahora como `sku`. Por compatibilidad, las peticiones aún aceptan y las respuestas incluyen el alias `codigoSku`.
- **Lotes** `/api/lotes` – CRUD.
  - `/api/lotes/por-evaluar` lista los lotes aún pendientes de evaluación. Cada elemento incluye el arreglo `evaluaciones` con los tipos de evaluación ya realizados.
- **Movimientos** `/api/movimientos` – registrar y consultar con filtros `productoId`, `almacenId`, `tipoMovimiento`, `clasificacion`, `fechaInicio`, `fechaFin`.
- **Órdenes de compra** `/api/inventario/ordenes` – crear una orden con sus detalles.
  - En la creación, el servidor asigna automáticamente la orden a cada detalle, por lo que no se requiere `ordenCompraId` en los elementos enviados.
- **Ajustes de inventario** `/api/inventario/ajustes` – listar, crear y eliminar.
- **Reportes** `/api/reportes` – alta/baja rotación (`fechaInicio`, `fechaFin`), productos más costosos (`categoria`), trazabilidad por lote (`codigoLote`), productos en retención o liberación (`estadoLote`, `desde`, `hasta`), no conformidades (`tipo`, `area`, `desde`, `hasta`), CAPA (`estado`, `desde`, `hasta`), stock actual, productos por vencer, alertas de inventario y movimientos. Todos devuelven Excel.

Los antiguos endpoints `/api/productos/reporte-stock`, `/api/lotes/reporte-vencimiento`, `/api/lotes/reporte-alertas` y `/api/movimientos/reporte-excel` fueron eliminados en favor de las rutas unificadas bajo `/api/reportes`.

### Producción
- **Órdenes de producción** `/api/produccion/ordenes` – CRUD.
  - El campo `unidadMedidaSimbolo` indica la unidad de la `cantidadProgramada`
    (por ejemplo, "kg" o "L"). Si no se envía, la API utilizará la unidad
    definida para el producto asociado.

### Calidad
- **Evaluaciones de calidad** `/api/calidad/evaluaciones` – CRUD de evaluaciones.

### BOM
- **Fórmulas de producto** `/api/bom/formulas` – CRUD de recetas.

## Seguridad
Se utiliza Spring Security con autenticación JWT y verificación 2FA. Los roles se gestionan mediante anotaciones `@PreAuthorize` en los controladores y la configuración de `SecurityConfig`. Todas las acciones registran el `usuarioId` y la `fechaIngreso` (por ejemplo en `MovimientoInventario`) para trazabilidad.

## Trazabilidad temporal y uso de LocalDateTime
Para asegurar una trazabilidad precisa de los eventos, los campos `fechaFabricacion`, `fechaVencimiento`, `fechaLiberacion` y `fechaEvaluacion` fueron migrados de `LocalDate` a `LocalDateTime` en las entidades y DTOs. Esto permite almacenar también la hora exacta en la que ocurrió cada acción.

Los repositorios se ajustaron para recibir `LocalDateTime` en sus consultas, y los servicios convierten cualquier fecha recibida sin hora usando `atStartOfDay()` o `atTime(23, 59, 59)` según corresponda. El mapper `LoteProductoMapper` de MapStruct asigna explícitamente cada campo temporal desde el DTO:

```java
@Mapping(target = "fechaFabricacion", expression = "java(dto.getFechaFabricacion())")
@Mapping(target = "fechaVencimiento", expression = "java(dto.getFechaVencimiento())")
@Mapping(target = "fechaLiberacion", expression = "java(dto.getFechaLiberacion())")
LoteProducto toEntity(LoteProductoRequestDTO dto, Producto producto, Almacen almacen, Usuario usuario);
```

Al trabajar con `LocalDateTime` se evita la pérdida de información al registrar movimientos o cambios de estado. **No se debe volver a utilizar `LocalDate`** en estos contextos, ya que comprometería la precisión temporal que requiere el sistema.

### Formato de fechas desde el frontend
Si el usuario ingresa la fecha mediante un control que sólo muestra `dd/mm/aaaa`, el frontend puede construir el `LocalDateTime` antes de enviarlo:

```javascript
const fecha = document.getElementById('fecha').value; // "2025-09-10" u "10/09/2025"
const fechaCompleta = `${fecha}T00:00:00`;           // "2025-09-10T00:00:00"
```

El utilitario `DateParser` acepta tanto `yyyy-MM-dd` como `dd/MM/yyyy` y convierte cualquiera de ellos a `LocalDateTime`, usando `00:00:00` o `23:59:59` según se invoque `parseStart` o `parseEnd`.

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
