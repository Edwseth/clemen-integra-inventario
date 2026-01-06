## Ajustes de esquema para `ddl-auto=validate` (V20251235)

Migración SQL: `src/main/resources/db/migration/inventario/V20251235__normalize_enums_and_align_schema.sql`

### Columnas/Tablas corregidas
- `conteos_ciclicos.estado` → ENUM(`BORRADOR`,`EN_CONTEO`,`CERRADO`,`APLICADO`)
- `orden_compra_documentos.tipo_documento` → ENUM(`CERTIFICADO`,`FACTURA`,`GUIA`)
- `productos.tipo_analisis_calidad` → ENUM(`NINGUNO`,`FISICO`,`QUIMICO_MICROBIOLOGICO`,`AMBOS`)
- `productos.modo_control_inventario` → ENUM(`CONTROL_STOCK`,`SIN_CONTROL_STOCK`)
- `almacenes.categoria_almacen` → ENUM(`MATERIA_PRIMA`,`PRODUCTO_TERMINADO`,`MATERIAL_EMPAQUE`,`SUMINISTROS`,`REPUESTOS`,`OBSOLETOS`,`PRODUCTO_SEMI_ELABORADO`)
- `solicitudes_movimiento.estado` → ENUM(`PENDIENTE`,`AUTORIZADA`,`PARCIAL`,`RECHAZADO`,`EJECUTADA`,`RESERVADA`,`ATENDIDA`,`CERRADA`,`CANCELADA`)
- `orden_produccion.batch_record_estado` → ENUM(`BORRADOR`,`EN_REVISION_CALIDAD`,`APROBADO`,`RECHAZADO`)
- `plan_produccion_semanal.estado` → ENUM(`BORRADOR`,`CONFIRMADO`,`CERRADO`)
- `corridas_mrp.estado` → ENUM(`EN_PROCESO`,`COMPLETADA`,`ERROR`)
- `sugerencias_abastecimiento.tipo` → ENUM(`COMPRA`,`FABRICAR`)
- `sugerencias_abastecimiento.estado` → ENUM(`PENDIENTE`,`APROBADA`,`DESCARTADA`,`CONVERTIDA`)
- `documentos.tipo` → ENUM(`PROCEDIMIENTO`,`FORMATO`,`REGISTRO`,`BITACORA_SANITIZACION`,`BITACORA_CALIBRACION`,`OTRO`)
- `documentos.area` → ENUM(`CALIDAD`,`PRODUCCION`,`INVENTARIOS`,`MANTENIMIENTO`,`COMPRAS`,`GENERAL`)
- `documentos.estado` → ENUM(`EN_ELABORACION`,`VIGENTE`,`OBSOLETO`)
- Nuevas columnas: `ordenes_compra.condiciones_pago` (ENUM `ANTICIPADO`,`CONTADO`,`DIAS_30`,`DIAS_60`) y `ordenes_compra.comprador` (VARCHAR(100)).

### Reglas de normalización aplicadas
- Todas las columnas de texto convertidas a MAYÚSCULA (`UPPER`) antes del `ALTER`.
- Fallback seguro por columna:
  - Estados genéricos y MRP: `BORRADOR` (planes, conteos), `PENDIENTE` (sugerencias, solicitudes), `EN_PROCESO` (corridas), `CONTROL_STOCK` (productos), `MATERIA_PRIMA` (almacenes), `BORRADOR` (batch record).
  - Documentos: `OTRO` (tipo), `GENERAL` (area), `EN_ELABORACION` (estado).
  - Orden de compra documentos: `FACTURA`.
  - Calidad producto legacy: `FISICO_QUIMICO`/`FISICOQUIMICO`→`FISICO`, `MICROBIOLOGICO`→`QUIMICO_MICROBIOLOGICO`, cualquier otro → `NINGUNO`.

### Resultado esperado
Las columnas mencionadas quedan tipadas como ENUM en MySQL con valores en mayúscula alineados a los enums Java. También se agregan los campos faltantes en `ordenes_compra`, eliminando errores de `SchemaManagementException` al validar en el perfil `prod`.
