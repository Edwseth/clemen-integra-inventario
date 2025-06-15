# Clemen-Integra ERP – Backend

Clemen-Integra es un ERP para laboratorios de productos homeopáticos y suplementos alimenticios. El backend está desarrollado en Java y concentra los módulos de Inventario, Producción, Calidad, Fórmulas (BOM) y Seguridad con autenticación 2FA.

## Módulos Funcionales
- **Inventario:** control de productos, almacenes, lotes, movimientos y órdenes de compra.
- **Producción:** gestión de órdenes, etapas y trazabilidad de lotes.
- **Calidad:** registro de no conformidades, acciones correctivas/preventivas y liberación de lotes.
- **Fórmulas (BOM):** definición de recetas e insumos por producto.
- **Seguridad:** control de usuarios, roles y autenticación con JWT y código 2FA.

## Arquitectura Técnica
El proyecto utiliza una arquitectura **monolito modular evolutivo**. Cada dominio funcional se implementa en paquetes independientes bajo `com.willyes.clemenintegra`:

```
com.willyes.clemenintegra
├── inventario
├── produccion
├── calidad
├── bom
└── shared    (configuración común, seguridad y excepciones)
```

## Tecnologías Principales
- Spring Boot 3
- Spring Security y JWT
- Autenticación 2FA
- Jakarta Validation
- Lombok y MapStruct
- JPA (Hibernate)
- SLF4J para registros
- MySQL

## Patrones Aplicados
- Inyección de dependencias por constructor.
- Separación de interfaces para servicios y repositorios.
- Centralización de manejo de excepciones.

## Calidad de Código
El código ha sido refactorizado mediante **Codex Workspace**, mejorando la legibilidad y eliminando clases obsoletas.

## Requisitos de Compilación y Despliegue
- JDK 17
- Maven 3.8+
- Base de datos MySQL en funcionamiento

Configurar las credenciales de base de datos en `src/main/resources/application.properties` mediante las variables `DB_USERNAME` y `DB_PASS`.

## Ejecución Básica
```bash
mvn clean install
mvn spring-boot:run
```
La API REST estará disponible en `http://localhost:8080/swagger-ui.html`.
