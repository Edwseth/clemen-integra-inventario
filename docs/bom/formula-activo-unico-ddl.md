# DDL recomendado: solo una fórmula activa por producto

Para garantizar que exista **solo una fórmula activa por producto**, se puede usar una columna generada
y un índice único basado en dicha columna. Esto evita múltiples registros `activo = 1` para el mismo
`producto_id` en la tabla `formula_producto`.

```sql
ALTER TABLE formula_producto
    ADD COLUMN activo_unico BIGINT
        GENERATED ALWAYS AS (CASE WHEN activo = 1 THEN producto_id ELSE NULL END) STORED;

CREATE UNIQUE INDEX uq_formula_producto_activo
    ON formula_producto (activo_unico);
```

> Nota: la columna generada permite que múltiples filas con `activo = 0` compartan el valor `NULL`,
> mientras que solo una fila con `activo = 1` pueda existir por `producto_id`.
