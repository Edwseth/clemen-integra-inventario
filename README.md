# Clemen-Integra ERP – Backend Modular

Este repositorio contiene el backend completo del sistema **Clemen-Integra ERP**, desarrollado por **Will Yes Solutions**, diseñado para gestionar integralmente los procesos de inventario, producción, calidad y formulación (BOM) en laboratorios de productos homeopáticos y suplementos alimenticios.

## 🧱 Arquitectura

El proyecto sigue una arquitectura **Monolito Modular Evolutivo**, organizada por paquetes independientes según el dominio funcional:

```bash
src/main/java/com/willyes/clemenintegra/
├── inventario/ # Gestión de productos, almacenes, lotes, movimientos
├── calidad/ # No conformidades, CAPA, liberación de lotes
├── produccion/ # Órdenes, etapas, trazabilidad
├── bom/ # Fórmulas, insumos, simulaciones
└── shared/ # Configuración, seguridad, excepciones

```

Cada módulo contiene su propio conjunto de:
- `controller`
- `service`
- `repository`
- `model`
- `dto`
- `mapper` (cuando aplica)

## ✅ Funcionalidades actuales por módulo

### 📦 Inventario
- Gestión de productos con unidades, categorías y control de calidad
- Manejo de almacenes con tipo y categoría
- Registro de lotes con trazabilidad y vencimientos
- Movimientos de entrada, salida, ajustes y transferencias
- Órdenes de compra y proveedores

### 🧪 Calidad
- Registro y control de no conformidades
- Gestión de acciones correctivas y preventivas (CAPA)
- Retención y liberación de lotes
- Checklists y validación de condiciones

### ⚙ Producción
- Creación y seguimiento de órdenes de producción
- Etapas: dispensado, mezcla, envasado, acondicionamiento, cuarentena
- Registro de controles de calidad en proceso

### 🧬 BOM (Fórmulas)
- Registro de fórmulas por producto con control de versiones y estados (BORRADOR, APROBADA, etc.)
- Definición de insumos, cantidades y unidades por fórmula (detalle_formula)
- Asociación de documentos técnicos: MSDS, instructivos y procedimientos
- Preparación de estructura para simulación de disponibilidad y costos de producción

## 🛠 Tecnologías utilizadas

- Java 17
- Spring Boot 3.2.x
- Spring Security + JWT
- Spring Data JPA (Hibernate)
- Swagger / OpenAPI 3
- Lombok
- MySQL

## 🔐 Seguridad y Autenticación

El sistema implementa seguridad con JWT. Los roles se definen por módulo (`ROL_ADMIN`, `ROL_CALIDAD`, `ROL_PRODUCCION`, etc.).

Swagger requiere autenticación:

```bash
Usuario: admin
Contraseña: admin123

```

## ⚙️ Configuración de base de datos
En src/main/resources/application.properties:
```bash
spring.datasource.url=jdbc:mysql://localhost:3306/clemen_integra_db
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contraseña
spring.jpa.hibernate.ddl-auto=none
```

## 🔍 Documentación API (Swagger)

- **Disponible en:**:

```bash
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
```
Incluye documentación de todos los endpoints REST de todos los módulos integrados.

---
## 🚀 Comandos útiles
```bash
# Compilar proyecto
./mvnw clean install

# Ejecutar aplicación
./mvnw spring-boot:run

# Generar la documentación Swagger
# Swagger se genera automáticamente y es accesible en el navegado

# Ejecutar pruebas
./mvnw test
```
## ‍💻 Desarrollado por
Will Yes Solutions
Repositorio oficial: github.com/Edwseth/clemen-integra-inventario

## 📄 Licencia
Este proyecto está desarrollado y mantenido por Will Yes Solutions. Todos los derechos reservados.

