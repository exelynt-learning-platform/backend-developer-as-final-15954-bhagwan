# Resource Booking API

Secure REST API for booking rooms, vehicles, equipment, and other resources. It uses Spring Boot 3, Java 17+, Spring Security 6, Auth0's `java-jwt` library, Spring Data JPA, and PostgreSQL by default. MySQL is also supported through environment variables.

## Run locally

Prerequisites: Java 17 or newer, Maven 3.9+, and a running PostgreSQL or MySQL database.

```bash
mvn spring-boot:run
```

The application listens on `http://localhost:8080`. The default PostgreSQL database is `booking_db`.

### Environment variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/booking_db` | JDBC connection URL |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `DB_DRIVER` | `org.postgresql.Driver` | JDBC driver (`com.mysql.cj.jdbc.Driver` for MySQL) |
| `DDL_AUTO` | `update` | Hibernate schema mode; use `validate` in production |
| `JWT_SECRET` | development-only secret | HMAC signing key, at least 32 characters |
| `JWT_EXPIRATION_MS` | `86400000` | Token lifetime |

For MySQL, use for example `DB_URL=jdbc:mysql://localhost:3306/booking_db` and `DB_DRIVER=com.mysql.cj.jdbc.Driver`.

## Authentication and roles

Seed users are created on an empty database:

| Username | Password | Role |
| --- | --- | --- |
| `admin` | `Admin@123` | `ADMIN` |
| `user` | `User@123` | `USER` |

Authenticate:

```http
POST /auth/login
Content-Type: application/json

{"username":"user","password":"User@123"}
```

Send the returned token as `Authorization: Bearer <token>`.

## API

Swagger UI: `http://localhost:8080/swagger-ui.html`  
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Resources

| Method | Endpoint | Access |
| --- | --- | --- |
| GET | `/api/resources`, `/api/resources/{id}` | Authenticated users |
| POST | `/api/resources` | ADMIN |
| PUT | `/api/resources/{id}` | ADMIN |
| DELETE | `/api/resources/{id}` | ADMIN |

Resource bodies use `name`, `description`, `price`, and `available`. Prices are `BigDecimal` values.

### Reservations

| Method | Endpoint | Access |
| --- | --- | --- |
| GET | `/api/reservations` | ADMIN sees all; USER sees own |
| GET | `/api/reservations/{id}` | ADMIN or owning USER |
| POST | `/api/reservations` | Authenticated users |
| PUT | `/api/reservations/{id}` | ADMIN or owning USER |
| DELETE | `/api/reservations/{id}` | ADMIN or owning USER |

Reservation creation requires `resourceId`, `startTime`, `endTime`, and `price`; the username is always taken from the JWT. Statuses are `PENDING`, `CONFIRMED`, and `CANCELLED`. List queries support `page`, `size`, `sort`, `status`, `minPrice`, and `maxPrice`, for example:

```text
GET /api/reservations?page=0&size=10&sort=startTime,desc&status=PENDING&minPrice=10&maxPrice=100
```

Validation errors, missing entities, ownership violations, and malformed requests return JSON containing `timestamp`, `status`, `error`, and `message`.

## Test

```bash
mvn test
```

JaCoCo generates `target/site/jacoco/index.html`. The Maven `verify` phase fails when instruction coverage is below 80%:

```bash
mvn verify
```
