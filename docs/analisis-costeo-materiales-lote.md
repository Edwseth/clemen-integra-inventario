# Análisis costeo materiales por lote

Documento de implementación técnica para costeo por lote en recepción OC, aplicación de costo en movimientos posteriores y costeo de PS/PT al cierre total de OP.

## Implementado en: commits

- Se implementó en la rama actual en una secuencia de commits (Flyway+entidades, OC fix, costeo recepción/movimientos, cierre OP, tests).

## Endurecimiento adicional
- El promedio ponderado del lote en recepciones adicionales usa acumuladores históricos (`costo_total_material_ingresado` / `total_ingresado_material`) y **no** `stock_lote` para evitar distorsión por consumos previos.
- En regularización posterior a cierre total de OP, se recalcula y persiste el costo unitario del lote producido con `sumarCostoMaterialRealOp / cantidadRealProducida`.
