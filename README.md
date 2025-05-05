# Clemen-Integra ERP - Módulo de Inventarios

Este proyecto representa el backend oficial del **módulo de inventarios** del sistema **Clemen-Integra ERP**, desarrollado por **Will Yes Solutions** para la gestión de operaciones de inventario en laboratorios de productos homeopáticos y suplementos alimenticios.

## ✅ Funcionalidades implementadas
- Gestión de almacenes con categoría y tipo definidos mediante enums.
- Registro y consulta de proveedores.
- Registro y seguimiento de órdenes de compra con estado controlado.
- Manejo de motivos de movimiento asociados a tipos de movimiento definidos por enums.

- Registro de productos con sus respectivas unidades, categorías y requisitos de calidad.
- Gestión de lotes de productos (stock, fechas de vencimiento, estado).
- Registro de movimientos de inventario (entradas, salidas, ajustes, transferencias).
- Consulta de movimientos con **filtros dinámicos** por producto, bodega, tipo de movimiento y fechas.
- Paginación integrada en las consultas.
- Documentación de API mediante Swagger.

## 🛠 Tecnologías y dependencias principales

- **Java 17**
- **Spring Boot 3.2.x**
- **Spring Data JPA**
- **Spring Security + JWT**
- **Swagger / OpenAPI 3**
- **Lombok**
- **MySQL**

## 📦 Estructura del proyecto

```bash
src/
├── main/
│   ├── java/com/willyes/clemenintegra/inventario/
│   │   ├── controller/        # Controladores REST
│   │   ├── service/           # Lógica de negocio
│   │   ├── domain/model/      # Entidades JPA
│   │   ├── domain/repository/ # Interfaces JPA
│   │   ├── dto/               # Clases DTO
│   │   ├── config/            # Configuración general (Swagger, CORS)
│   │   └── security/          # Configuración JWT
│   └── resources/
│       ├── application.properties
│       └── ...

```

## ⚙️ Configuración de base de datos
En src/main/resources/application.properties:
```bash
spring.datasource.url=jdbc:mysql://localhost:3306/clemen_integra_db
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contraseña
spring.jpa.hibernate.ddl-auto=none
```
## 🔐 Seguridad
Este proyecto implementa seguridad basada en JWT (JSON Web Tokens).

- Los endpoints protegidos deben incluir el token JWT en el header:

```bash
Authorization: Bearer <token>
```

## 🔍 Documentación API
El backend de **Clemen-Integra ERP** para el módulo de **inventarios** está documentado usando **Swagger**. Puedes ver y probar la API en tiempo real a través de la siguiente URL:

- **Accede a la documentación interactiva en Swagger UI**:

```bash
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
```
### Autenticación en Swagger

Por razones de seguridad, Swagger requiere autenticación. Usa el siguiente usuario y contraseña para acceder:

- **Usuario:** `admin`
- **Contraseña:** `admin123`

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
## 📄 Licencia
Este proyecto está desarrollado y mantenido por Will Yes Solutions. Todos los derechos reservados.

