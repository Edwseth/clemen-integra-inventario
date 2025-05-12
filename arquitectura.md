# Documento de Arquitectura – Clemen-Integra ERP

## 1. Introducción
Este documento describe la arquitectura técnica seleccionada para el desarrollo del sistema ERP **Clemen-Integra**, implementado para el laboratorio CLEMEN. La solución busca ser escalable, modular y mantenible, permitiendo su evolución conforme crecen las necesidades del negocio.

## 2. Arquitectura Seleccionada: Monolito Modular Evolutivo

### ¿Qué es?
Una arquitectura monolítica organizada internamente por dominios funcionales (Inventario, Producción, Calidad, BOM). Todos los módulos comparten el mismo proyecto backend y base de datos, pero con separación lógica en paquetes y responsabilidades.

### Justificación
- Etapa inicial del sistema.
- Equipo de desarrollo pequeño.
- Módulos fuertemente integrados (comparten `producto_id`, `lote_id`, etc.).
- Se busca agilidad, simplicidad y bajo costo operativo.

Esta arquitectura permite entregar valor rápidamente sin perder la posibilidad de migrar a microservicios en el futuro si el crecimiento del sistema lo requiere.

## 3. Componentes Principales

- **Backend:** Java con Spring Boot
- **Estructura modular por paquetes:**
  ```
  clemen-integra-backend
  ├── inventario
  ├── calidad
  ├── produccion
  ├── bom
  ├── shared (entidades comunes, utilidades)
  ```
- **Base de datos:** MySQL con modelo físico unificado.
- **Seguridad:** Spring Security + JWT (con roles como ADMIN, CALIDAD, PRODUCCION, etc.)
- **Documentación API:** Swagger UI
- **Gestión de dependencias:** Maven
- **Control de versiones:** Git + GitHub

## 4. Consideraciones de Escalabilidad

- Las entidades están organizadas por contexto funcional, permitiendo futura extracción de microservicios.
- Los controladores están desacoplados mediante uso de DTOs y servicios intermedios.
- Las relaciones entre módulos se realizan mediante repositorios compartidos.
- Uso de estándares como REST, capas de servicio e interfaces.

## 5. Despliegue

- Aplicación empaquetada en un único JAR.
- Ejecutable en servidores Linux o servicios en la nube (Ej: EC2, Railway, Render).
- Base de datos desplegable en RDS u otro motor compatible.

## 6. Recomendaciones Futuras

- Cuando el sistema crezca, considerar migrar módulos con alta carga (como Producción) a microservicios.
- Implementar Docker para facilitar la portabilidad.
- Separar la base de datos por esquemas si la trazabilidad o auditoría lo requiere.
- Implementar un sistema de colas (ej. RabbitMQ) si hay tareas asíncronas como validación de calidad o notificaciones.

## 7. Diagrama General (propuesta visual)
```
[Usuario] → [Spring Boot Monolito] → [Módulo Inventario]
                                → [Módulo Calidad]
                                → [Módulo Producción]
                                → [Módulo BOM]
                                ↘ [MySQL DB]
```

## 8. Cierre
La arquitectura monolítica modular proporciona una solución robusta, simple y alineada con las necesidades actuales del proyecto. Permite entregar funcionalidad rápida, mantener la coherencia de datos, y dejar las bases preparadas para una futura transición a microservicios si la escala del negocio así lo exige.
