# Clemen-Integra ERP - Módulo de Inventarios

Este proyecto representa el backend oficial del **módulo de inventarios** del sistema **Clemen-Integra ERP**, desarrollado por **Will Yes Solutions** para la gestión de operaciones de inventario en laboratorios de productos homeopáticos y suplementos alimenticios.

## 🛠 Tecnologías y dependencias principales

- Java 17
- Spring Boot 3.2.x
- Spring Data JPA
- Spring Security + JWT
- Swagger / OpenAPI 3
- Lombok
- MySQL

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
Una vez levantado el proyecto, accede a la documentación Swagger en:

```bash
http://localhost:8080/swagger-ui.html
```
## 🚀 Comandos útiles
```bash
# Compilar proyecto
./mvnw clean install

# Ejecutar aplicación
./mvnw spring-boot:run

# Ejecutar pruebas
./mvnw test
```
## 📄 Licencia
Este proyecto está desarrollado y mantenido por Will Yes Solutions. Todos los derechos reservados.

