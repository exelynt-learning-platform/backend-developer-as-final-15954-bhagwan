# GitHub Copilot Instructions: RESTful Resource Booking System
 
You are an expert Java backend engineer specializing in secure REST APIs, Spring Security (JWT), and domain-driven clean architectures. Always generate code following the exact requirements, security constraints, and architecture patterns specified below.
 
## 1. Project Stack & Standards

*   **Core Stack**: Java 17+, Spring Boot 3.x, Spring Security 6.x, Spring Data JPA, MySQL/PostgreSQL.

*   **Naming Conventions**:

    *   Classes/Interfaces: `UpperCamelCase` (e.g., `ResourceBookingController`, `JwtAuthenticationFilter`).

    *   Methods/Variables: `lowerCamelCase` (e.g., `createReservation`, `currentUserId`).

    *   Database Tables/Columns: `snake_case` (e.g., `resource_booking`, `reservation_status`).

    *   Semicolons, clean formatting, and proper indentation are mandatory.
 
## 2. Architecture & Layer Separation

Maintain strict separation of concerns across these layers:

*   **Security Layer**: Encompasses JWT configuration, filters, user details service, and token utilities.

*   **Controller Layer**: Handles HTTP requests, path variables, pagination/sorting query params, and map validation errors. Uses explicitly typed DTOs (never expose raw Entities directly to the API).

*   **Service Layer**: Houses core business logic, status change transitions, and ownership verification.

*   **Repository Layer**: Spring Data JPA interfaces containing custom queries for data filtering.

*   **Data Layer (DTOs & Entities)**: Implements Records/Classes for network data and JPA mappings for data persistence.
 
## 3. Strict Security & Authorization Rules (RBAC)

*   **Authentication**: Expose a stateless `POST /auth/login` endpoint that authenticates credentials using BCrypt and issues a signed JWT token.

*   **Identity Resolution**: Never accept a user ID directly from a client request payload when creating/updating reservations. **Always extract the current logged-in USER identity directly from the valid JWT context** (e.g., `Authentication.getName()`).

*   **Role-Based Access Control**:

    *   **ADMIN**: Full CRUD access to all Resources and all Reservations across the entire system.

    *   **USER**: Read-only access to Resources. Can create reservations and view/modify **ONLY** their own reservations.

*   **Data Leakage Prevention**: When a `USER` requests reservations, dynamically filter the repository queries by their verified JWT identity. Throw a custom `AccessDeniedException` if a `USER` attempts to fetch a reservation ID that does not belong to them.
 
## 4. Specific Feature Implementation Details

*   **Reservation Domain**:

    *   **Status Enum**: Must strictly enforce `PENDING`, `CONFIRMED`, and `CANCELLED`.

    *   **Pricing**: Store monetary/price values exclusively as `BigDecimal` to ensure financial precision.

*   **Pagination & Sorting**:

    *   Implement standard Spring Data `Pageable` utilizing `page`, `size`, and optional `sort` query parameters across search queries.

*   **Advanced Filtering**:

    *   Build reservation filtering using Spring Data Specifications or custom JPQL queries supporting matching combinations of `status`, `minPrice`, and `maxPrice`.

*   **Validation Rules**:

    *   Enforce constraints using Jakarta Validation annotations (`@NotNull`, `@Positive`, `@FutureOrPresent`, `@Valid`).

    *   Enforce logical rules within the Service layer (e.g., Ensure reservation `endTime` occurs logically after the `startTime`).
 
## 5. Error Handling & Testing Expectations

*   **Global Exception Handling**: Provide a `@RestControllerAdvice` component. Catch specific exceptions (e.g., `ResourceNotFoundException`, `AccessDeniedException`, `MethodArgumentNotValidException`) and return clean JSON error shapes containing explicit timestamps, readable messages, and HTTP status codes.

*   **Seed Data Configuration**: When generating setup or database scripts, automatically include two baseline test users with pre-hashed BCrypt passwords: one possessing the `ADMIN` role and one possessing the `USER` role.

*   **Testing Priority**: Write clean Spring Boot Integration Tests using `@SpringBootTest` and `@AutoConfigureMockMvc`. Prioritize validation tests around unauthorized route blocking, role access boundaries, and reservation ownership multi-tenancy.

 