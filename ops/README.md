# Operación y despliegue

El backend de Clemen Integra ERP se publica detrás de un reverse proxy/Tunnel (p. ej. Cloudflare) que termina HTTPS.

- **Encabezados reenviados**: la aplicación debe respetar los encabezados `X-Forwarded-*`/`Forwarded` para que las URLs generadas conserven el esquema seguro (`https`). Configura el perfil que uses con `server.forward-headers-strategy=framework` (ya definido en los perfiles principales).
- **CORS**: define el dominio real del frontend vía `CORS_ALLOWED_ORIGINS` o ajustando `app.cors.allowed-origins` en el perfil correspondiente. En producción el valor por defecto es `https://erp.clemenlab.com`.

Al publicar detrás del túnel, asegúrate de que el proxy reenvíe `X-Forwarded-Proto=https` y `X-Forwarded-Host` para que Spring detecte correctamente el origen seguro.
