# Convenciones API Calidad e Inventarios

## Identificadores canónicos
- Todos los DTOs de respuesta exponen el campo `id` como identificador principal.
- Alias históricos como `idLote` se exponen únicamente como propiedades de lectura y están marcados como **DEPRECATED**. Serán removidos el 31/12/2024; actualice los consumidores para usar `id`.
- Para cargas útiles de entrada se aceptan alias mediante `@JsonAlias`, pero las respuestas siempre incluirán el campo canónico.

## Códigos de error funcionales
| Código | HTTP | Descripción |
|--------|------|-------------|
| `BLOQUEO_RETENCION_NC` | 409 | El lote permanece retenido por una no conformidad activa o sin detalle asociado. |
| `BLOQUEO_ESTADO_CUARENTENA` | 409 | La operación requiere que el lote continúe en CUARENTENA/RETENIDO. |
| `NC_ABIERTA` | 409 | Existen no conformidades abiertas asociadas al lote. |
| `EVALUACIONES_FALTANTES` | 422 | Faltan evaluaciones previas o datos complementarios (condición/severidad). |
| `ROL_INSUFICIENTE` | 403 | El usuario autenticado no cuenta con el rol requerido para la acción. |
| `SOLICITUD_INVALIDA` | 400 | Datos faltantes o malformados en la solicitud. |
| `RECURSO_NO_ENCONTRADO` | 404 | El recurso referenciado no existe o ya fue atendido. |
| `OPERACION_NO_PERMITIDA` | 409 | Reglas de negocio que impiden completar la operación. |
| `ERROR_INTERNO` | 500 | Error inesperado no controlado. |

Todas las respuestas de error siguen la estructura:

```json
{
  "code": "BLOQUEO_RETENCION_NC",
  "message": "El lote está retenido por una no conformidad abierta.",
  "details": {
    "loteId": 98,
    "retencionId": 312
  }
}
```

## Ejemplos de contrato

### Registrar evaluación de calidad
**Request**
```json
{
  "loteProductoId": 15,
  "tipoEvaluacion": "QUIMICO_MICROBIOLOGICO",
  "resultado": "CONDICIONADO",
  "observaciones": "Se requiere seguimiento",
  "condicion": {
    "tipo": "FECHA_LIMITE",
    "parametroFecha": "2024-09-30T00:00:00",
    "descripcion": "Liberar solo después del reproceso"
  },
  "archivosAdjuntos": [
    {"nombreArchivo": "rep-qa.pdf", "nombreVisible": "Reporte QA"}
  ]
}
```
**Response (201)**
```json
{
  "id": 1204,
  "resultado": "CONDICIONADO",
  "tipoEvaluacion": "FISICO",
  "fechaEvaluacion": "2024-09-01T09:14:22",
  "observaciones": "Se requiere seguimiento",
  "archivosAdjuntos": [
    {"nombreArchivo": "rep-qa.pdf", "nombreVisible": "Reporte QA"}
  ],
  "nombreLote": "L-2408-01",
  "nombreProducto": "Jarabe expectorante",
  "nombreEvaluador": "Lucía Torres"
}
```

### Liberación de lote retenido
**Respuesta exitosa (caso requerido)**
```json
{
  "id": 42,
  "codigoLote": "LT-2024-00042",
  "estado": "LIBERADO",
  "almacenNombre": "Producto Terminado",
  "stockDisponible": 125.0,
  "fechaLiberacion": "2024-09-02T11:45:00",
  "nombreUsuarioLiberador": "Jefe Calidad"
}
```

**Error `BLOQUEO_RETENCION_NC`**
```json
{
  "code": "BLOQUEO_RETENCION_NC",
  "message": "El lote está retenido por una no conformidad abierta.",
  "details": {
    "loteId": 42,
    "retencionId": 818
  }
}
```

**Error `NC_ABIERTA`**
```json
{
  "code": "NC_ABIERTA",
  "message": "Debe cerrar la no conformidad NC-2024-005 antes de liberar el lote.",
  "details": {
    "loteId": 42,
    "ncId": 995
  }
}
```

**Error `ROL_INSUFICIENTE`**
```json
{
  "code": "ROL_INSUFICIENTE",
  "message": "Solo el Jefe de Calidad puede liberar lotes."
}
```

### Estado de calidad de un lote
**GET `/api/calidad/lotes/{loteId}/estado-calidad`**
```json
{
  "loteId": 42,
  "estadoLote": "RETENIDO",
  "retencionActiva": true,
  "motivoRetencion": "NO_CONFORMIDAD",
  "nc": {
    "id": 995,
    "severidad": "MAYOR",
    "estado": "ABIERTA"
  },
  "condicionUsoActiva": false,
  "condicionUso": null
}
```

> Nota: Los campos `motivoRetencion`, `nc` y `condicionUso` son opcionales y se omiten cuando no aplican.
