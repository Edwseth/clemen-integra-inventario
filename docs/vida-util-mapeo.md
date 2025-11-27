# Mapeo de `VidaUtilProducto`

- **El @Id real es `productoId`** (tipo `Integer`).
- La columna PK en la tabla `vida_util_productos` es `producto_id`.
- El atributo `producto` es una relación `@OneToOne` marcada como `insertable = false` y `updatable = false`; se usa solo para navegación y no define la clave primaria.
