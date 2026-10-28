# Financial Home Backend

Backend Java con Spring Boot en etapa inicial. El código contiene el punto de entrada de la aplicación y una estructura de capas para cuentas (`Accounts`), todavía sin un flujo funcional completo ni reglas de negocio implementadas.

## Tecnologías declaradas

Verificadas en `pom.xml`:

| Tecnología | Configuración o dependencia |
| --- | --- |
| Java | Versión 21 |
| Maven | Gestión del proyecto mediante `pom.xml` |
| Spring Boot | Versión 4.0.8 |
| Spring MVC | `spring-boot-starter-webmvc` |
| Spring Data JPA | `spring-boot-starter-data-jpa` |
| Validación | `spring-boot-starter-validation` |
| PostgreSQL | Controlador JDBC con alcance de ejecución |
| Flyway | Starter y soporte para PostgreSQL; desactivado en la configuración actual |

Estas dependencias no implican que todas las capacidades estén implementadas: todavía no hay entidades, operaciones de persistencia ni validaciones de datos en el código de cuentas.

## Estructura del código

```text
src/
├── main/
│   ├── java/com/financialhome/financial_home_backend/
│   │   ├── FinancialHomeBackendApplication.java
│   │   └── Accounts/
│   │       ├── controller/AccountController.java
│   │       ├── service/AccountService.java
│   │       └── repository/AccountRepository.java
│   └── resources/application.properties
└── test/java/com/financialhome/financial_home_backend/
    └── FinancialHomeBackendApplicationTests.java
```

`FinancialHomeBackendApplication` contiene el método `main` y la anotación `@SpringBootApplication`, que sirve como punto de entrada de Spring Boot.

## Capas y estado actual

La estructura agrupa el código de cuentas en `Accounts` y separa las responsabilidades HTTP, de aplicación y de acceso a datos. Las capas todavía no están conectadas mediante inyección de dependencias.

### Controller

La capa `controller` recibe solicitudes HTTP y define las respuestas de la API.

`AccountController` usa `@RestController`, declara la ruta base `/api/accounts` y un método `getAccounts()` con `@GetMapping`. El método está incompleto y presenta errores de sintaxis que impiden compilar el código en su estado actual. No existe aún una respuesta funcional para ese endpoint.

### Service

La capa `service` tiene como responsabilidad alojar la lógica de aplicación y coordinar operaciones, separándolas del manejo HTTP.

`AccountService` es una clase vacía: no tiene `@Service`, métodos ni dependencias. No implementa lógica de negocio.

### Repository

La capa `repository` tiene como responsabilidad acceder a los datos y separar la persistencia del resto de la aplicación.

`AccountRepository` es una clase vacía. No extiende una interfaz de Spring Data como `JpaRepository` ni implementa operaciones de persistencia.

### Entidades

No hay entidades JPA en el código actual. Por tanto, no hay un modelo de persistencia de cuentas definido en este repositorio.

## Configuración existente

`src/main/resources/application.properties` contiene:

- El nombre de aplicación `financial-home-backend`; la propiedad está declarada dos veces con el mismo valor.
- La configuración de conexión a PostgreSQL mediante `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`, con valores predeterminados en el archivo.
- El controlador JDBC de PostgreSQL.
- `spring.jpa.hibernate.ddl-auto=validate`, para validar los mapeos contra el esquema sin generar cambios automáticos mediante Hibernate.
- `spring.flyway.enabled=false`, que desactiva las migraciones automáticas de Flyway.

Esta configuración no demuestra que la conexión a la base de datos se haya verificado.

## Pruebas existentes

`FinancialHomeBackendApplicationTests` contiene una prueba `contextLoads()` con `@SpringBootTest`, destinada a comprobar la carga del contexto de Spring. No hay pruebas específicas de las capas de cuentas.

No se ejecutaron pruebas, compilaciones, comandos Docker ni migraciones para elaborar esta documentación.

## Componentes pendientes

- Completar el método del controlador y definir su respuesta HTTP.
- Implementar la capa de servicio.
- Definir las entidades y el acceso a datos, según las decisiones de diseño del proyecto.
- Conectar las capas y añadir pruebas para el comportamiento que se defina.

Estos pendientes describen las partes ausentes; no establecen funcionalidades, contratos de respuesta ni reglas de negocio.
