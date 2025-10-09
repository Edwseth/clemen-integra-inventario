# Pruebas manuales - Evaluaciones de Calidad

## 1. Registro CONFORME
```bash
curl -X POST "http://localhost:8080/api/calidad/evaluaciones" \
  -H "Content-Type: multipart/form-data" \
  -F "dto={\"resultado\":\"CONFORME\",\"tipoEvaluacion\":\"FISICO_QUIMICO\",\"observaciones\":\"Cumple especificaciones\",\"loteProductoId\":1,\"usuarioEvaluadorId\":5};type=application/json" \
  -F "archivos=@/path/informe.pdf"
```
_Verificar que no se crean Condiciones de uso ni Retenciones._

## 2. Registro CONDICIONADO
```bash
curl -X POST "http://localhost:8080/api/calidad/evaluaciones" \
  -H "Content-Type: multipart/form-data" \
  -F "dto={\"resultado\":\"CONDICIONADO\",\"tipoEvaluacion\":\"FISICO_QUIMICO\",\"observaciones\":\"Liberar bajo restricción\",\"loteProductoId\":1,\"usuarioEvaluadorId\":5,\"condicion\":{\"tipo\":\"LIMITACION_USO\",\"descripcion\":\"Solo muestras internas\"}};type=application/json" \
  -F "archivos=@/path/informe.pdf"
```
_Verificar en `/api/calidad/lotes/1/estado-calidad` que `condicionUsoActiva=true`._

## 3. Registro NO_CONFORME
```bash
curl -X POST "http://localhost:8080/api/calidad/evaluaciones" \
  -H "Content-Type: multipart/form-data" \
  -F "dto={\"resultado\":\"NO_CONFORME\",\"tipoEvaluacion\":\"FISICO_QUIMICO\",\"observaciones\":\"pH fuera de rango\",\"loteProductoId\":1,\"usuarioEvaluadorId\":5,\"severidadNc\":\"CRITICA\"};type=application/json" \
  -F "archivos=@/path/informe.pdf"
```
_Verificar que el lote pasa a `RETENIDO`, se crea la NC y la retención. Consultar estado agregado con:_
```bash
curl http://localhost:8080/api/calidad/lotes/1/estado-calidad
```
