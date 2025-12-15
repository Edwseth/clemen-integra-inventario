# Operación y despliegue

## Publicación detrás de túnel/Reverse Proxy
El backend se publica detrás de un túnel HTTPS (Cloudflared) que termina TLS públicamente y reenvía al backend por `http://localhost:8080` dentro del servidor.

- **Escucha local en PROD**: el perfil `prod` de Spring Boot queda ligado a `127.0.0.1:8080` (`server.address` + `server.port`).
- **Cloudflared interno**: el túnel debe apuntar a `http://localhost:8080` (no exponer el puerto directamente).
- **Acceso externo**: solo se atiende por `https://<DOMINIO>` publicado en Cloudflare; no se debe acceder directamente al puerto local.

- **Encabezados reenviados**: `server.forward-headers-strategy=framework` ya está activo en los perfiles principales para respetar `X-Forwarded-*`/`Forwarded` y conservar el esquema seguro (`https`).
- **CORS**: define el dominio real del frontend vía `CORS_ALLOWED_ORIGINS` o ajusta `app.cors.allowed-origins` en el perfil correspondiente. En producción el valor por defecto es `https://erp.clemenlab.com`.
- **Objetivo**: HTTPS público extremo a extremo. Cloudflared termina TLS y reenvía al backend local en 8080; el frontend consume únicamente la URL pública.

## Carpeta recomendada en Windows
```
C:\ClemenERP\
├─ backend\           (JAR y config Spring Boot)
├─ cloudflared\       (config.yml, credenciales del túnel)
├─ logs\              (stdout/stderr de servicios, logs de app)
├─ config\            (vars/env, archivos .env opcionales)
├─ ops\               (scripts operativos, p. ej. HealthWatchdog.ps1)
└─ backups\           (respaldos BD/config)
```

## Servicios Windows (NSSM)
Nombres estándar de servicios:
- **ClemenERP-Backend**: ejecuta `java -jar clemen-integra-erp.jar --spring.profiles.active=prod`
- **ClemenERP-Tunnel**: ejecuta el túnel Cloudflared apuntando al backend local.

### Backend (ClemenERP-Backend)
1. Copia el JAR a `C:\ClemenERP\backend\clemen-integra-erp.jar`.
2. Instala con NSSM (ejecutar en PowerShell/Administrador):
   ```powershell
   nssm install ClemenERP-Backend "C:\Program Files\Java\jdk-21\bin\java.exe" \
       -jar "C:\ClemenERP\backend\clemen-integra-erp.jar" --spring.profiles.active=prod
   ```
3. En **I/O** de NSSM define:
   - `stdout`: `C:\ClemenERP\logs\backend-stdout.log`
   - `stderr`: `C:\ClemenERP\logs\backend-stderr.log`
   - Habilita rotación por tamaño (p. ej. 10 MB, conservar 7 archivos).
4. En **Environment** agrega variables requeridas (`DB_URL`, `DB_USERNAME`, `DB_PASS`, `CORS_ALLOWED_ORIGINS`, `SERVER_PORT` si deseas diferente a 8080, etc.).
5. En **Shutdown** usa `Graceful` para permitir cierre limpio.
6. Habilita reinicio automático ante fallos:
   ```powershell
   sc.exe failure ClemenERP-Backend reset= 0 actions= restart/5000/restart/5000/restart/5000
   ```
7. Inicia y verifica:
   ```powershell
   nssm start ClemenERP-Backend
   ```

### Cloudflared Tunnel (ClemenERP-Tunnel)
1. Instala Cloudflared y coloca `config.yml` en `C:\ClemenERP\cloudflared\config.yml`.
2. Plantilla mínima de `config.yml` (reemplaza `<DOMINIO>` y `tunnel:` por el ID creado en Cloudflare):
   ```yaml
   tunnel: <ID_DEL_TUNEL>
   credentials-file: C:\ClemenERP\cloudflared\<ID_DEL_TUNEL>.json
   ingress:
     - hostname: api.<DOMINIO>
       service: http://localhost:8080
     - service: http_status:404
   originRequest:
     connectTimeout: 10s
   ```
3. Instala el servicio NSSM:
   ```powershell
   nssm install ClemenERP-Tunnel "C:\Program Files\cloudflared\cloudflared.exe" tunnel run
   ```
   - Directorio de inicio: `C:\ClemenERP\cloudflared`
   - `stdout`/`stderr`: `C:\ClemenERP\logs\cloudflared-stdout.log` y `cloudflared-stderr.log` (con rotación).
4. Configura reinicio automático:
   ```powershell
   sc.exe failure ClemenERP-Tunnel reset= 0 actions= restart/5000/restart/5000/restart/5000
   ```
5. Inicia y valida que el túnel publique `https://api.<DOMINIO>`.

## Healthcheck operativo (/actuator/health)
- El backend expone `/actuator/health` y `/actuator/info` (sin información sensible). Usar el perfil `prod` y puerto local 8080 salvo que se sobrescriba `SERVER_PORT`.
- Pruebas:
  - **Local**: `curl http://localhost:8080/actuator/health`
  - **Público**: `curl https://api.<DOMINIO>/actuator/health` (debe devolver `X-Forwarded-Proto: https` desde Cloudflared).

## Checklist de verificación (24/7)
1. **Servicio Backend**: `nssm status ClemenERP-Backend`; revisar `backend-stdout.log`/`stderr`.
2. **Servicio Túnel**: `nssm status ClemenERP-Tunnel`; revisar logs de cloudflared.
3. **Health local**: `curl http://localhost:8080/actuator/health` devuelve `UP`.
4. **Health público**: `curl https://api.<DOMINIO>/actuator/health` devuelve `UP` con encabezado `X-Forwarded-Proto: https`.
5. **CORS**: el frontend carga sin errores CORS. Ajustar `CORS_ALLOWED_ORIGINS` si aparece `CORS policy error`.
6. **Contenido mixto**: la consola del navegador no debe mostrar `Mixed Content`; el backend debe ser consumido siempre por `https://api.<DOMINIO>`.
7. **TLS extremo a extremo**: validar el certificado público en el navegador y que Cloudflared apunte a `http://localhost:8080` únicamente dentro del servidor.

## Watchdog opcional
`ops/HealthWatchdog.ps1` permite reiniciar el servicio si falla el health local. Se recomienda programarlo en el Programador de tareas cada 5 minutos con privilegios de Administrador. No es obligatorio para compilar.
