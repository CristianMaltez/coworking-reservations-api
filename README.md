# Coworking Reservation Service

API REST para la gestión de espacios de coworking y reservas, desarrollada con **Spring Boot 3.5.x**, **Java 21** y **PostgreSQL 17**.

El proyecto implementa autenticación y autorización con JWT, administración de espacios, creación y gestión de reservas con control de concurrencia, validación de pagos mediante un servicio externo simulado protegido con **Resilience4j Circuit Breaker**, notificaciones asíncronas y documentación OpenAPI/Swagger.

---

## Estado del proyecto

Actualmente se encuentran implementados y validados:

- Registro, login, logout y consulta del usuario autenticado.
- Autorización basada en roles `USER` y `ADMIN`.
- Gestión de espacios de coworking.
- Catálogos de tipos de espacio y estados de reserva.
- Creación, consulta, filtrado, paginación y cancelación de reservas.
- Prevención de reservas solapadas.
- Manejo transaccional y control de concurrencia con bloqueo pesimista.
- Patrón de comportamiento **State** para el ciclo de vida de una reserva.
- Validación externa de pagos simulada.
- **Circuit Breaker con Resilience4j** y fallback coherente.
- Notificación asíncrona al confirmar una reserva.
- Manejo centralizado de errores.
- OpenAPI / Swagger UI.
- Spring Boot Actuator.
- Pruebas unitarias e integración con PostgreSQL mediante Testcontainers.
- Dockerfile y Docker Compose para levantar API + PostgreSQL.

Por la restricción de tiempo de la prueba técnica, el **reporte de ocupación y su caché con `@Cacheable/@CacheEvict` no fueron implementados**. Se priorizó mantener un alcance estable, probado y ejecutable antes que introducir una funcionalidad adicional incompleta.

---

## Tecnologías principales

- Java 21
- Spring Boot 3.5.x
- Spring Web MVC
- Spring Data JPA / Hibernate
- Spring Security
- JWT
- Jakarta Validation
- PostgreSQL 17
- Flyway
- Resilience4j / Spring Cloud Circuit Breaker
- Spring Boot Actuator
- Springdoc OpenAPI / Swagger UI
- JUnit 5
- Mockito
- Testcontainers
- Docker / Docker Compose
- Maven Wrapper

---

## Requisitos para ejecutar

### Opción recomendada: Docker

Solo se requiere:

- Docker Desktop o Docker Engine
- Docker Compose

No es necesario instalar Java, Maven ni PostgreSQL localmente para esta opción.

### Desarrollo local

Para ejecutar Spring Boot directamente en la máquina se requiere:

- Java 21+
- Docker para PostgreSQL, o una instancia local compatible de PostgreSQL

Maven no necesita instalarse globalmente porque el proyecto incluye Maven Wrapper.

---

## Ejecución con Docker Compose

Desde la raíz del proyecto:

```bash
docker compose up --build
```

Esto levanta:

- PostgreSQL 17
- `reservation-service`
- Flyway aplica automáticamente las migraciones pendientes

Una vez iniciado el servicio:

```text
API:        http://localhost:8080
Health:     http://localhost:8080/actuator/health
Swagger:    http://localhost:8080/swagger-ui.html
OpenAPI:    http://localhost:8080/v3/api-docs
```

El `docker-compose.yml` utiliza el perfil `dev`, de modo que Swagger y el servicio simulado de pagos están disponibles para facilitar la evaluación.

Para ver los logs de la API:

```bash
docker compose logs -f reservation-service
```

Para detener los servicios:

```bash
docker compose down
```

Para detener los servicios y eliminar también el volumen de PostgreSQL:

```bash
docker compose down -v
```

> `docker compose down -v` elimina los datos almacenados en el volumen de PostgreSQL.

---

## Ejecución local para desarrollo

Puede levantarse únicamente PostgreSQL con Docker:

```bash
docker compose up postgres -d
```

Luego ejecutar Spring Boot directamente.

### Windows / PowerShell

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

### Linux / macOS

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Perfiles de configuración

### `dev`

Pensado para desarrollo y evaluación local.

Características principales:

- PostgreSQL local / Docker Compose.
- Swagger habilitado.
- OpenAPI habilitado.
- Mock del servicio de pagos habilitado.
- Logging SQL habilitado.
- JWT con secreto local de desarrollo.

### `prod`

Pensado para despliegue productivo.

Características principales:

- Credenciales de base de datos por variables de entorno.
- Secreto JWT por variable de entorno.
- Swagger/OpenAPI deshabilitados.
- Mock de pagos deshabilitado.
- Servicio de pagos configurado externamente.
- Logging SQL deshabilitado.

Variables principales:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SIGNING_SECRET
JWT_ACCESS_TOKEN_TTL
PAYMENT_SERVICE_URL
PAYMENT_CONNECT_TIMEOUT
PAYMENT_READ_TIMEOUT
```

---

## Base de datos

El proyecto usa **PostgreSQL 17**.

El esquema se administra mediante **Flyway**. Hibernate está configurado con:

```text
ddl-auto=validate
```

por lo que Hibernate valida el esquema, pero no lo crea ni lo modifica automáticamente.

Las entidades principales incluyen:

- `users`
- `roles`
- `user_roles`
- `space_types`
- `spaces`
- `reservations`
- `revoked_tokens`

Los identificadores principales utilizan UUID. Los catálogos pequeños utilizan identificadores numéricos.

---

## Autenticación y autorización

La API utiliza JWT y funciona de forma stateless.

### Roles

- `USER`
- `ADMIN`

El registro público siempre crea usuarios con rol `USER`.

### Flujo básico

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/users/me
POST /api/auth/logout
```

El JWT incluye el identificador del usuario, roles, `jti`, fecha de emisión y expiración.

El logout revoca el `jti` del token mediante la tabla `revoked_tokens`.

Las contraseñas se almacenan usando BCrypt.

No se implementaron refresh tokens para mantener el alcance de la prueba reducido y enfocado.

---

## Crear un usuario ADMIN para evaluación local

El registro público no permite elegir el rol `ADMIN` deliberadamente.

Primero registrar un usuario normalmente mediante:

```text
POST /api/auth/register
```

Luego puede asignarse el rol `ADMIN` directamente en la base de datos de desarrollo.

Entrar a PostgreSQL:

```bash
docker exec -it reservation-postgres psql -U reservation_user -d reservation_db
```

Ejemplo:

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE LOWER(u.email) = LOWER('admin@example.com')
  AND r.code = 'ADMIN'
ON CONFLICT DO NOTHING;
```

Después debe realizarse login nuevamente porque los roles se incorporan al JWT en el momento de autenticación.

---

## Endpoints principales

### Autenticación

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/auth/register` | Público |
| POST | `/api/auth/login` | Público |
| POST | `/api/auth/logout` | Autenticado |
| GET | `/api/users/me` | Autenticado |

### Espacios

| Método | Endpoint | Acceso |
|---|---|---|
| GET | `/api/spaces` | USER / ADMIN |
| GET | `/api/spaces/{id}` | USER / ADMIN |
| POST | `/api/spaces` | ADMIN |
| PUT | `/api/spaces/{id}` | ADMIN |
| DELETE | `/api/spaces/{id}` | ADMIN |

### Catálogos

| Método | Endpoint | Acceso |
|---|---|---|
| GET | `/api/catalogs/space-types` | Autenticado |
| GET | `/api/catalogs/reservation-statuses` | Autenticado |

### Reservas

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/reservations` | USER / ADMIN |
| GET | `/api/reservations` | USER / ADMIN |
| GET | `/api/reservations/{id}` | Propietario / ADMIN |
| PATCH | `/api/reservations/{id}` | Propietario / ADMIN según transición |

Filtros disponibles para `GET /api/reservations`:

```text
userId
spaceId
statusCode
from
to
page
size
```

La paginación es **zero-based**:

```text
page=0 -> primera página
page=1 -> segunda página
```

El tamaño máximo permitido es 100.

---

## Ciclo de vida de una reserva

Estados expuestos por la API:

| Código | Estado |
|---:|---|
| 1 | Pendiente de pago |
| 2 | Confirmada |
| 3 | Cancelada |

Las transiciones válidas se implementan mediante el patrón **State**.

Ejemplos:

```text
PENDING_PAYMENT -> CONFIRMED
PENDING_PAYMENT -> CANCELLED
CONFIRMED       -> CANCELLED
CANCELLED       -> sin transiciones posteriores
```

Los códigos son explícitos y no dependen del ordinal del enum de Java.

---

## Patrón de diseño State

Se eligió el patrón de comportamiento **State** para representar el ciclo de vida de una reserva.

Cada estado conoce qué transiciones son válidas desde su estado actual. Esto evita concentrar toda la lógica en bloques crecientes de `if/else` o `switch` dentro del servicio.

Ventajas principales:

- Reglas de transición explícitas.
- Responsabilidades separadas.
- Menor crecimiento de condicionales en el servicio.
- Mayor facilidad para agregar nuevos estados o reglas en el futuro.
- Facilita las pruebas unitarias de la máquina de estados.

La autorización y las reglas de estado se mantienen separadas: por ejemplo, una transición puede ser válida desde el punto de vista del dominio y aun así estar prohibida para un `USER`.

---

## Prevención de reservas solapadas

Una reserva se considera solapada cuando:

```text
existing.start < requested.end
AND
existing.end > requested.start
```

Esto permite reservas consecutivas, por ejemplo:

```text
10:00 - 11:00
11:00 - 12:00
```

Los estados que bloquean disponibilidad son:

- Pendiente de pago
- Confirmada

Una reserva cancelada no bloquea el espacio.

---

## Concurrencia y transacciones

La creación de reservas utiliza una transacción y bloqueo pesimista sobre el espacio correspondiente.

Flujo simplificado:

```text
1. Bloquear el espacio con PESSIMISTIC_WRITE.
2. Consultar si existe una reserva solapada.
3. Calcular el precio.
4. Crear la reserva como PENDING_PAYMENT.
5. Commit.
```

Esto serializa intentos concurrentes sobre el mismo espacio y evita que dos peticiones simultáneas creen reservas solapadas.

Reservas de espacios diferentes pueden procesarse independientemente.

---

## Cálculo de precio

El precio de una reserva se calcula a partir de:

```text
tarifa por hora × duración real
```

Se utiliza `BigDecimal` para valores monetarios.

La duración admite fracciones de hora y el resultado se redondea a dos decimales.

El precio total queda almacenado como snapshot en la reserva para evitar que cambios futuros en la tarifa alteren reservas existentes.

---

## Validación de pagos

La confirmación de una reserva requiere validación de pago.

Ejemplo:

```http
PATCH /api/reservations/{id}
```

```json
{
  "statusCode": 2,
  "paymentMethod": "test-approved"
}
```

`paymentMethod` se utiliza únicamente como escenario determinístico para el proveedor de pagos simulado.

### Escenarios disponibles en desarrollo

| `paymentMethod` | Comportamiento |
|---|---|
| `test-approved` | Pago aprobado |
| `test-rejected` | Pago rechazado |
| `test-error` | Simula error HTTP del proveedor |
| `test-slow` | Simula una respuesta superior al timeout |

En una integración real, este campo sería reemplazado por una referencia o token entregado por el proveedor de pagos.

---

## Circuit Breaker con Resilience4j

La llamada al servicio externo de pagos está protegida por un Circuit Breaker llamado:

```text
paymentService
```

El Circuit Breaker evita realizar continuamente llamadas a un proveedor que está fallando o respondiendo demasiado lento.

Estados principales:

```text
CLOSED    -> las llamadas pasan normalmente
OPEN      -> las llamadas se bloquean y se ejecuta fallback
HALF_OPEN -> se permiten llamadas limitadas para comprobar recuperación
```

La configuración de umbrales se encuentra en `application.yml` e incluye, entre otros:

- ventana de llamadas
- número mínimo de llamadas
- tasa de fallos
- tiempo en estado abierto
- llamadas permitidas en half-open

### Fallback

Si el proveedor falla, excede el timeout o el circuito se encuentra abierto:

```text
la reserva permanece PENDING_PAYMENT
```

La indisponibilidad del proveedor no convierte la reserva en cancelada ni genera un estado artificial de fallo.

El estado del Circuit Breaker puede consultarse mediante:

```text
GET /actuator/circuitbreakers
```

Este endpoint requiere autenticación según la configuración actual de seguridad.

---

## Límite transaccional de la validación de pago

La llamada HTTP al proveedor de pagos **no se realiza mientras existe un bloqueo pesimista ni dentro de una transacción larga**.

Flujo simplificado:

```text
1. Validación corta de la reserva.
2. Finaliza la transacción de lectura.
3. Se llama al proveedor externo.
4. Si el pago se aprueba, se abre una transacción corta de escritura.
5. La reserva pasa a CONFIRMED.
6. Commit.
```

Esto evita mantener conexiones, transacciones y locks de base de datos mientras se espera una operación de red potencialmente lenta o inestable.

---

## Notificación asíncrona

Cuando una reserva se confirma correctamente se publica:

```text
ReservationConfirmedEvent
```

La notificación simulada se procesa con:

```text
@Async
@TransactionalEventListener(phase = AFTER_COMMIT)
```

Por lo tanto:

- El correo simulado solo se ejecuta después de un commit exitoso.
- La respuesta HTTP no espera a que la notificación termine.
- La lógica de reserva queda desacoplada de la notificación.

Actualmente el envío se simula mediante logging:

```text
Correo de confirmación simulado enviado a {email} para la reserva {id}
```

---

## Documentación OpenAPI / Swagger

La documentación OpenAPI se genera automáticamente mediante Springdoc.

En perfil `dev`:

```text
Swagger UI:
http://localhost:8080/swagger-ui.html

OpenAPI JSON:
http://localhost:8080/v3/api-docs
```

Swagger incluye configuración `Bearer JWT`, permitiendo autenticarse mediante el botón **Authorize**.

En perfil `prod` Swagger/OpenAPI está deshabilitado deliberadamente.

---

## Spring Boot Actuator

Endpoints principales expuestos:

```text
/actuator/health
/actuator/info
/actuator/metrics
/actuator/circuitbreakers
```

`/actuator/health` es público.

Los demás endpoints mantienen protección de seguridad.

---

## Manejo de errores

La API utiliza un `@ControllerAdvice` centralizado y mantiene una semántica consistente:

| HTTP | Uso |
|---:|---|
| 400 | JSON inválido, request ilegible o errores de binding |
| 401 | Token ausente, inválido, expirado o revocado; credenciales inválidas |
| 403 | Usuario autenticado sin permisos suficientes |
| 404 | Recurso solicitado directamente no encontrado |
| 409 | Conflictos de estado, email duplicado, solapamiento o transición inválida |
| 422 | Bean Validation o validaciones semánticas de campos |

Los mensajes visibles para el consumidor de la API se encuentran en español.

---

## Pruebas automatizadas

Ejecutar:

### Windows

```powershell
.\mvnw.cmd test
```

### Linux / macOS

```bash
./mvnw test
```

Actualmente la suite contiene **18 pruebas** y ha sido validada con:

```text
Tests run: 18
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Se incluyen pruebas de:

- cálculo de precios
- State Pattern
- ReservationService con Mockito
- creación de reservas
- concurrencia real
- validación de pagos
- fallback de pagos

Las pruebas de integración utilizan Testcontainers con:

```text
postgres:17-alpine
```

De esta manera las pruebas de persistencia y concurrencia se ejecutan contra PostgreSQL real y no contra una base con comportamiento diferente.

---

## Build

### Windows

```powershell
.\mvnw.cmd clean package
```

### Linux / macOS

```bash
./mvnw clean package
```

---

## Docker

Construir la imagen directamente:

```bash
docker build -t reservation-service .
```

La opción recomendada para evaluación es:

```bash
docker compose up --build
```

porque levanta tanto PostgreSQL como la aplicación y espera a que la base de datos esté saludable antes de iniciar el servicio.

---

## Decisiones de diseño

### UUID para entidades principales

Usuarios, espacios y reservas utilizan UUID para evitar depender de secuencias globales y reducir exposición de IDs incrementales.

### SMALLINT para catálogos

Roles, tipos de espacio y estados utilizan identificadores compactos y estables.

### `OffsetDateTime` + `TIMESTAMPTZ`

Las fechas de reserva conservan información de zona/offset y se almacenan usando tipos temporales adecuados de PostgreSQL.

### `BigDecimal` para dinero

Los valores monetarios no utilizan `double` o `float` para evitar errores de precisión.

### DTOs en lugar de entidades JPA

Las entidades de persistencia no se exponen directamente desde los controladores. Esto evita acoplar el contrato HTTP al modelo interno de JPA.

### Relaciones LAZY

Las relaciones JPA se mantienen lazy y `open-in-view` está deshabilitado.

Para los casos que requieren asociaciones se utilizan consultas/EntityGraph de forma explícita para evitar problemas N+1.

### State Pattern

Se utilizó State para modelar transiciones de reservas en lugar de concentrar la lógica en condicionales crecientes.

### Locks cortos

Los locks de base de datos solo se mantienen durante operaciones críticas de persistencia. Las llamadas de red quedan fuera de ese límite.

### Mock de pagos determinístico

Los escenarios del proveedor externo no son aleatorios. Esto facilita reproducir errores, timeouts y recuperaciones durante evaluación y pruebas.

---

## Trade-offs y funcionalidades fuera de alcance

Debido al límite de tiempo se priorizó una implementación sólida de seguridad, reservas, concurrencia, resiliencia, pruebas y ejecución reproducible.

### No implementado

#### Reporte de ocupación y caché

Quedó pendiente el endpoint de porcentaje de ocupación por espacio/rango de fechas junto con `@Cacheable/@CacheEvict`.

Con más tiempo se implementaría:

- query agregada para calcular ocupación sobre un rango
- DTO específico de reporte
- caché del resultado por espacio y rango de fechas
- invalidación al crear, confirmar o cancelar reservas
- pruebas de integración del cálculo e invalidación

#### Proveedor real de pagos

El proyecto utiliza un mock determinístico únicamente en `dev`.

En un entorno real se configuraría un proveedor externo mediante `PAYMENT_SERVICE_URL` y credenciales gestionadas mediante secretos del entorno.

#### Proveedor real de correo

La notificación está implementada de forma asíncrona, pero el envío se simula mediante logs.

Con más tiempo se conectaría a un proveedor de correo manteniendo el mismo listener/evento.

#### Refresh tokens

No se implementaron refresh tokens. Para el alcance de la prueba se utilizó un JWT de acceso con expiración corta y revocación mediante `jti`.

#### Bootstrap automático de ADMIN

No se crea automáticamente un administrador para evitar credenciales privilegiadas por defecto. En desarrollo puede asignarse manualmente el rol como se documenta anteriormente.

#### Reprogramación de reservas

No se agregó modificación de fechas porque implica volver a ejecutar validación de solapamiento, locking, recálculo de precio y potencial interacción con el estado de pago.

---

## Consideraciones de seguridad

- API stateless.
- CSRF deshabilitado para el API REST basado en Bearer JWT.
- Contraseñas almacenadas con BCrypt.
- Autorización basada en roles.
- Tokens revocados mediante `jti`.
- Swagger deshabilitado en producción.
- Credenciales y secretos productivos obtenidos mediante variables de entorno.
- El mock del proveedor de pagos solo existe en perfil `dev`.

