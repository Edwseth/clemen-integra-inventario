## Perfil `prod-bootstrap` para instalación/actualización

Usa este perfil **solo** durante la fase de instalación o actualización de esquema en producción. Habilita Flyway con las migraciones bajo `db/migration/inventario` y hereda la configuración de `prod` (puertos, datasource, CORS, logging).

### Ejecución

```bash
SPRING_PROFILES_ACTIVE=prod-bootstrap \
DB_URL="jdbc:mysql://<host>:3306/clemen_integra_erp?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota" \
DB_USERNAME=<usuario> \
DB_PASS=<password> \
java -jar clemen-integra-backend.jar
```

### Flujo operativo sugerido
1. Ejecuta el backend con `SPRING_PROFILES_ACTIVE=prod-bootstrap` para aplicar/validar migraciones.
2. Detén el servicio.
3. Arranca normalmente con `SPRING_PROFILES_ACTIVE=prod` (Flyway deshabilitado) una vez la base esté alineada.
