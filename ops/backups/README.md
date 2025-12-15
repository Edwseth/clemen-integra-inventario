# Backups MySQL (Ops)

Solución operativa para respaldos diarios y verificación semanal de restauración de la base de datos MySQL usada por el backend Spring Boot. Todo está pensado para ejecutarse en servidores/PC Windows con PowerShell y clientes MySQL 8 instalados.

## Alcance
- Protege la base de datos principal (por defecto `clemen_integra_erp`).
- Genera respaldos diarios comprimidos en ZIP con hash SHA256 y rotación.
- Verifica semanalmente que el último respaldo pueda restaurarse en una base temporal y que existan datos en tablas clave.
- No almacena credenciales en el repositorio. Usa `mysql_config_editor` (`login-path`) o un `defaults-extra-file` ubicado fuera del repo.

## Estructura
```
ops/backups/
├─ README.md                   <- este archivo
└─ mysql/
   └─ windows/
      ├─ backup_mysql.ps1
      ├─ verify_restore_mysql.ps1
      ├─ README.md            <- guía paso a paso en Windows
      └─ task-scheduler/      <- plantillas XML de tareas programadas
```

## Parámetros principales
- **Directorio de destino**: `ops/backups/mysql/windows/data` (configurable en cada script).
- **Retención**: 30 días por defecto.
- **Programación sugerida**:
  - Backup diario a las 02:00 AM (`backup_mysql.ps1`).
  - Verificación de restauración cada domingo 03:00 AM (`verify_restore_mysql.ps1`).
- **Salida de cada ejecución**:
  - `NOMBREDB_YYYYMMDD_HHMM.zip` + `NOMBREDB_YYYYMMDD_HHMM.zip.sha256`
  - `backup_YYYYMMDD.log` o `verify_restore_YYYYMMDD.log`

## Restauración manual rápida
1. Ubicar el ZIP más reciente en la carpeta de backups y validar su hash:
   ```powershell
   Get-FileHash .\clemen_integra_erp_YYYYMMDD_HHMM.zip -Algorithm SHA256
   Get-Content .\clemen_integra_erp_YYYYMMDD_HHMM.zip.sha256
   ```
2. Descomprimir el ZIP para obtener el `.sql`.
3. Restaurar con el login-path configurado:
   ```powershell
   mysql --login-path=clemen_backup -e "DROP DATABASE IF EXISTS clemen_integra_erp;"
   mysql --login-path=clemen_backup -e "CREATE DATABASE clemen_integra_erp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
   mysql --login-path=clemen_backup clemen_integra_erp < clemen_integra_erp_YYYYMMDD_HHMM.sql
   ```
   > Alternativa: usar `--defaults-extra-file` apuntando a un archivo de credenciales **fuera** del repositorio.

## Validación manual
- Ejecutar `verify_restore_mysql.ps1` para validar restauración y consultas de salud sobre tablas clave (`productos`, `lotes_productos`, `movimientos_inventario`).
- Revisar el log generado en la carpeta de backups para confirmar `INFO Verificación de restauración completada exitosamente`.

## Monitoreo y logs
- Cada script genera un log por día en la carpeta de backups (`backup_YYYYMMDD.log` y `verify_restore_YYYYMMDD.log`).
- Buscar líneas con `[ERROR]` o códigos de salida distintos de `0` en el Programador de tareas.

## Recomendación offsite
Copiar periódicamente la carpeta de backups (ZIP + logs) a almacenamiento externo o carpeta de red segura para protegerse contra fallas locales. El proceso puede ser manual o automatizado fuera del alcance de estos scripts.

## Más detalles
Consulta `ops/backups/mysql/windows/README.md` para pasos exactos de configuración, ejemplos y plantillas del Programador de tareas.
