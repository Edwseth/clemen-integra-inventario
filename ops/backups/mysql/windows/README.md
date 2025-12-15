# Backups MySQL en Windows

Scripts operativos para crear backups diarios y verificar restauración semanal en MySQL 8, sin exponer credenciales en el repositorio.

## Prerrequisitos
- Windows con PowerShell 5+.
- Cliente MySQL 8 (`mysqldump.exe` y `mysql.exe`) en el PATH.
- Permisos para crear bases de datos y ejecutar `mysql_config_editor`.
- Carpeta de destino accesible para escribir backups (por defecto `ops/backups/mysql/windows/data`).

## Configurar credenciales seguras
### Opción A (recomendada): `mysql_config_editor`
1. Abrir PowerShell en el servidor.
2. Registrar el login-path (ejemplo con usuario `backup_user`):
   ```powershell
   mysql_config_editor set --login-path=clemen_backup --host=localhost --user=backup_user --password
   ```
   Se solicitará la contraseña y quedará cifrada en `%APPDATA%\MySQL\.mylogin.cnf`.
3. Probar conexión:
   ```powershell
   mysql --login-path=clemen_backup -e "SELECT NOW();"
   ```

### Opción B: `--defaults-extra-file`
1. Crear un archivo fuera del repositorio, por ejemplo `C:\secure\mysql-backup.cnf`:
   ```
   [client]
   user=backup_user
   password=********
   host=localhost
   ```
2. Probar conexión:
   ```powershell
   mysql --defaults-extra-file=C:\secure\mysql-backup.cnf -e "SELECT NOW();"
   ```
3. Pasar la ruta del archivo al parámetro `-DefaultsExtraFile` de cada script.

> No guardes usuario/clave dentro del repositorio.

## Script: `backup_mysql.ps1`
- Hora sugerida: todos los días 02:00 AM.
- Genera: `NOMBREDB_YYYYMMDD_HHMM.zip`, `NOMBREDB_YYYYMMDD_HHMM.zip.sha256` y `backup_YYYYMMDD.log`.
- Flags usados: `--single-transaction --routines --triggers --events --hex-blob --default-character-set=utf8mb4`.

### Uso manual
```powershell
cd ops\backups\mysql\windows
./backup_mysql.ps1 -DatabaseName "clemen_integra_erp" -BackupDirectory "D:\backups\mysql" -RetentionDays 30 -LoginPath "clemen_backup"
# o usando defaults-extra-file
./backup_mysql.ps1 -DefaultsExtraFile "C:\\secure\\mysql-backup.cnf"
```

### Comprobación del resultado
```powershell
Get-ChildItem D:\backups\mysql
Get-Content D:\backups\mysql\backup_YYYYMMDD.log
Get-Content D:\backups\mysql\clemen_integra_erp_YYYYMMDD_HHMM.zip.sha256
Get-FileHash D:\backups\mysql\clemen_integra_erp_YYYYMMDD_HHMM.zip -Algorithm SHA256
```

## Script: `verify_restore_mysql.ps1`
- Hora sugerida: domingos 03:00 AM.
- Busca el último ZIP, valida SHA256, restaura en `<db>_restorecheck`, ejecuta consultas y elimina la base temporal.
- Log: `verify_restore_YYYYMMDD.log`.

### Uso manual
```powershell
cd ops\backups\mysql\windows
./verify_restore_mysql.ps1 -DatabaseName "clemen_integra_erp" -BackupDirectory "D:\backups\mysql" -LoginPath "clemen_backup"
# o usando defaults-extra-file
./verify_restore_mysql.ps1 -DefaultsExtraFile "C:\\secure\\mysql-backup.cnf"
```

### Qué valida
- Integridad del ZIP (coincidencia SHA256).
- Restauración en base temporal `clemen_integra_erp_restorecheck`.
- Consultas de salud:
  - `SELECT COUNT(*) FROM productos;`
  - `SELECT COUNT(*) FROM lotes_productos;`
  - `SELECT COUNT(*) FROM movimientos_inventario;`

Si cualquier paso falla, el script sale con código diferente de `0` y registra `[ERROR]` en el log.

## Programar tareas en Windows (Task Scheduler)
### Importar plantillas
1. Abrir **Task Scheduler** > **Import Task...**.
2. Seleccionar la plantilla desde `ops\backups\mysql\windows\task-scheduler`:
   - `mysql_backup_daily.xml` para el backup diario 02:00 AM.
   - `mysql_verify_restore_weekly.xml` para la verificación semanal domingo 03:00 AM.
3. Ajustar:
   - Usuario que ejecuta la tarea (con permisos de MySQL y escritura de backups).
   - Ruta absoluta de `Program/Arguments` hacia `powershell.exe` y el script.
   - Ruta de `Start in` hacia la carpeta que contiene los scripts.

### Crear manualmente (alternativa)
```powershell
# Backup diario
schtasks /Create /SC DAILY /TN "MySQL Backup" /TR "powershell -ExecutionPolicy Bypass -File C:\ops\backups\mysql\windows\backup_mysql.ps1" /ST 02:00

# Verificación semanal
schtasks /Create /SC WEEKLY /D SUN /TN "MySQL Verify Restore" /TR "powershell -ExecutionPolicy Bypass -File C:\ops\backups\mysql\windows\verify_restore_mysql.ps1" /ST 03:00
```
> Ajusta las rutas y parámetros (por ejemplo `-BackupDirectory` y `-DefaultsExtraFile`).

## Restauración manual a producción
1. Validar hash del ZIP.
2. Detener la aplicación que use la BD.
3. `DROP DATABASE` y `CREATE DATABASE` de la base objetivo con `utf8mb4`.
4. Importar el `.sql` con `mysql --login-path=clemen_backup <archivo>`.
5. Arrancar la aplicación y revisar.

## Dónde revisar logs y qué hacer si falla
- Carpeta de backups: revisar los archivos `backup_YYYYMMDD.log` y `verify_restore_YYYYMMDD.log`.
- Task Scheduler: revisar History / Last Run Result (0x0 indica éxito).
- Acciones sugeridas ante fallas:
  - Validar conectividad MySQL con `mysql --login-path=clemen_backup -e "SELECT 1;"`.
  - Revisar espacio en disco y permisos de escritura en la carpeta de backups.
  - Reconfigurar credenciales con `mysql_config_editor set --login-path=clemen_backup ...`.
  - Reejecutar el script manualmente para confirmar.

## Ejemplo de ejecución local (sin credenciales reales)
```powershell
# Simular con parámetros mínimos en una máquina con MySQL local y login-path configurado
cd ops\backups\mysql\windows
./backup_mysql.ps1 -BackupDirectory "C:\\temp\\mysql-backups"
./verify_restore_mysql.ps1 -BackupDirectory "C:\\temp\\mysql-backups"
```

## Nota offsite
Recomienda copiar la carpeta de backups (ZIP + logs) a un medio externo o carpeta de red segura después de cada respaldo. Esto no está automatizado en estos scripts.
