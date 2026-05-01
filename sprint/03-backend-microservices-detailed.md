# Backend Microservices Detailed Guide

## Backend Stack

All backend services use Java 21 and Spring Boot 3.5.12. Spring Cloud version is 2025.0.1. The backend uses:

- Spring Web or WebFlux.
- Spring Cloud Gateway.
- Spring Cloud Config.
- Eureka service discovery.
- Spring Security.
- Spring Data JPA.
- PostgreSQL.
- RabbitMQ.
- ModelMapper in application/document services.
- JJWT for JWT handling.
- Springdoc OpenAPI for Swagger.
- Micrometer and Zipkin for tracing.

## API Gateway

Folder: `api-gateway`

The gateway is the frontend's main backend entry point.

### Main Class

`ApiGatewayApplication.java` starts the Spring Boot gateway app.

### Routing

Routes are configured in `api-gateway/src/main/resources/application.properties`.

Important route mappings:

- `/gateway/auth/**` and `/auth/**` -> `lb://AUTH-SERVICE`
- `/gateway/applications/**` and `/applications/**` -> `lb://APPLICATION-SERVICE`
- `/gateway/documents/**` and `/documents/**` -> `lb://DOCUMENT-SERVICE`
- `/gateway/admin/**` and `/admin/**` -> `lb://ADMIN-SERVICE`

The `lb://` prefix means routes use Eureka service discovery.

The gateway also aggregates Swagger docs:

- `/v3/api-docs/auth`
- `/v3/api-docs/application`
- `/v3/api-docs/document`
- `/v3/api-docs/admin`

### JWT Filter

`JwtFilter.java` is a global gateway filter.

It allows public paths:

- `/auth/login`
- `/auth/signup`
- `/gateway/auth/login`
- `/gateway/auth/signup`
- `/test`
- Swagger/OpenAPI paths.

For protected routes, it:

1. Reads `Authorization` header.
2. Requires `Bearer <token>`.
3. Validates JWT signature and expiration using `JwtService`.
4. Blocks admin routes if role is not `ADMIN`.
5. Adds headers:
   - `X-User-Email`
   - `X-User-Role`
6. Forwards request downstream.

### Security Config

`SecurityConfig.java` permits all exchanges because JWT protection is handled by `JwtFilter`.

### CORS Config

Allows frontend origins:

- `http://localhost:*`
- `http://127.0.0.1:*`

### JwtService

Validates tokens and extracts:

- email from subject.
- role from `role` claim.

## Auth Service

Folder: `auth-service`

Auth service owns users, passwords, roles, JWT generation, and profile data.

### Main Class

`AuthServiceApplication.java` starts the service.

### Entity

`User.java` maps to table `users`.

Fields:

- `id`
- `email`
- `password`
- `role`
- `firstName`
- `lastName`
- `dateOfBirth`
- `phoneNumber`
- `referralCode`
- `createdAt`

Email is unique.

### Repository

`UserRepository.java` extends `JpaRepository<User, Long>`.

Methods:

- `findByEmail(String email)`
- `findAllByOrderByIdAsc()`

### DTOs

- `SignupRequest`: registration input with validation.
- `LoginRequest`: login input with email/password validation.
- `ProfileUpdateRequest`: profile update input.
- `ChangePasswordRequest`: current and new password.
- `RoleUpdateRequest`: internal role update payload.
- `UserResponse`: public user data returned to frontend/admin.

### AuthController

Base path: `/auth`

Endpoints:

- `POST /auth/signup`: creates user and returns JWT.
- `POST /auth/login`: validates credentials and returns JWT.
- `GET /auth/profile`: returns user profile using `X-User-Email`.
- `PUT /auth/profile`: updates profile.
- `PUT /auth/password`: changes password.

### InternalUserController

Base path: `/internal/users`

Endpoints:

- `GET /internal/users`: returns all users.
- `PUT /internal/users/{id}/role`: updates a user's role.

These are intended for service-to-service use by admin-service.

### AuthService Business Logic

Signup:

1. Normalize email to lowercase.
2. Check duplicate email.
3. Enforce age 18+.
4. Encode password with BCrypt.
5. Default role is `USER`.
6. Set createdAt.
7. Save user.
8. Return JWT.

Login:

1. Normalize email.
2. Find user.
3. Check BCrypt password.
4. Normalize role.
5. Return JWT.

Profile update:

- Allows first name, last name, DOB, and phone.
- Enforces age 18+ if DOB changes.

Password change:

- Validates current password before saving encoded new password.

Role update:

- Valid roles are `USER` and `ADMIN`.

### Admin Bootstrap

`AdminBootstrapConfig.java` creates or upgrades a default admin on startup.

Default:

- email: `admin@finflow.com`
- password: `Admin@123`
- role: `ADMIN`

Controlled by properties:

- `auth.bootstrap.admin.enabled`
- `auth.bootstrap.admin.email`
- `auth.bootstrap.admin.password`

### Security

`SecurityConfig.java`:

- Stateless session.
- CSRF disabled.
- Permits `/auth/signup`, `/auth/login`, and `/internal/**`.
- Permits Swagger.
- Requires authentication for other paths.

In practice, profile endpoints are protected at gateway and receive `X-User-Email`.

## Application Service

Folder: `application-service`

Application service owns user-facing loan applications.

### Entity

`LoanApplication.java` fields:

- `id`
- `name`
- `applicantName`
- `amount`
- `loanType`
- `tenureMonths`
- `status`

`applicantName` stores the applicant email from `X-User-Email`.

### Repository

`ApplicationRepository.java` extends `JpaRepository`.

Methods:

- `findByApplicantName(String applicantName)`

### DTOs

- `ApplicationRequestDTO`: create/update request.
- `ApplicationResponseDTO`: response sent to frontend.
- `ApplicationMessageDTO`: RabbitMQ snapshot message to admin-service.
- `ApplicationStatusUpdateDTO`: RabbitMQ status update from admin-service.
- `ApplicationOwnerResponseDTO`: internal owner lookup response for document-service.

### ApplicationController

Base path: `/applications`

Endpoints:

- `POST /applications`: create draft.
- `GET /applications`: gets current user's applications.
- `GET /applications/my`: also gets current user's applications.
- `GET /applications/{id}`: get owned application.
- `PUT /applications/{id}`: update owned draft.
- `DELETE /applications/{id}`: delete owned draft.
- `POST /applications/{id}/submit`: submit owned draft.
- `GET /applications/{id}/status`: get owned application status.
- `GET /applications/test`: test endpoint.

### InternalApplicationController

Base path: `/internal/applications`

Endpoint:

- `GET /internal/applications/{id}/owner`

Used by document-service to verify document access.

### ApplicationService Business Logic

Create:

- Requires authenticated email.
- Normalizes name.
- Defaults loan type to `PERSONAL` if blank.
- Defaults tenure to `12` months if missing.
- Status is `DRAFT`.
- Saves to application database.
- Publishes snapshot to RabbitMQ `application_queue`.

Update:

- Only allowed for `DRAFT`.
- Only owner can update through user endpoint.
- Publishes updated snapshot.

Delete:

- Only allowed for `DRAFT`.
- Only owner can delete through user endpoint.
- Publishes delete message to admin-service.

Submit:

- Only owner can submit.
- Only `DRAFT` can be submitted.
- Sets status to `SUBMITTED`.
- Publishes snapshot.

Status sync:

- `ApplicationStatusUpdateListener` listens to `application_status_update_queue`.
- It updates the original `LoanApplication` status after admin decisions.

### RabbitConfig

Defines:

- `application_queue`
- `application_status_update_queue`
- Jackson JSON message converter.

Note: In application-service, only `statusUpdateQueue()` bean is declared, but it still sends to `application_queue` by name.

## Document Service

Folder: `document-service`

Document service owns uploaded file content and document metadata.

### Entity

`Document.java` fields:

- `id`
- `fileName`
- `fileType`
- `documentType`
- `data` as `@Lob byte[]`
- `applicationId`
- `uploadedByEmail`

### Repository

`DocumentRepository.java` extends `JpaRepository`.

Methods:

- `existsByApplicationId(Long applicationId)`
- `findMetadataByApplicationId(Long applicationId)` using JPQL constructor query.

The metadata query returns no binary data, only details needed for lists.

### DTOs

- `DocumentResponseDTO`: document metadata.
- `ApplicationOwnerResponseDTO`: owner response from application-service.

### DocumentController

Base path: `/documents`

Endpoints:

- `POST /documents/upload`: multipart upload.
- `GET /documents/{id}`: download file.
- `GET /documents/applications/{applicationId}`: list metadata for one application.
- `GET /documents/applications/{applicationId}/exists`: admin-only existence check.
- `GET /documents/internal/applications/{applicationId}/exists`: internal existence check.
- `GET /documents/test`: test endpoint.

### DocumentService Business Logic

Before saving, downloading, or listing documents, it calls `validateApplicationAccess`.

Access rule:

- If role is `ADMIN`, allow.
- Otherwise, require email.
- Ask application-service `/internal/applications/{id}/owner`.
- Compare owner applicant email with request email.
- If different, throw `Unauthorized`.

Upload:

- Validates access.
- Saves original file name.
- Saves content type.
- Normalizes document type.
- Saves file bytes.
- Stores application id and uploader email.

Allowed document types:

- `SALARY_SLIP`
- `BANK_STATEMENT`
- `ID_PROOF`
- `ADDRESS_PROOF`
- `OTHER`

## Admin Service

Folder: `admin-service`

Admin service owns admin review workflow and keeps a synced copy of applications.

### Main Class

`AdminServiceApplication.java` excludes default `UserDetailsServiceAutoConfiguration` and enables Rabbit.

### Entity

`Application.java` is the admin-side synced application.

Fields:

- `id`
- `name`
- `applicantName`
- `amount`
- `loanType`
- `tenureMonths`
- `adminNotes`
- `status`

It implements `Persistable<Long>` because IDs come from application-service, not from admin-service auto-generation.

### Repository

`ApplicationRepository.java`:

- Extends `JpaRepository<Application, Long>`.
- Defines native PostgreSQL upsert query.
- Defines `countByStatus(String status)`.

The upsert keeps admin_db synchronized when application-service publishes snapshots.

### Rabbit Listener

`ApplicationListener.java` listens to `application_queue`.

- If action is `DELETE`, deletes local admin copy.
- Otherwise, calls `upsertApplication`.

### AdminController

Base path: `/admin`

Endpoints:

- `GET /admin/applications`
- `GET /admin/applications/{id}`
- `POST /admin/applications/{id}/decision`
- `POST /admin/applications/bulk-decision`
- `PUT /admin/applications/{id}/notes`
- `PUT /admin/documents/{id}/verify`
- `GET /admin/reports`
- `GET /admin/users`
- `PUT /admin/users/{id}`

Important note:

`PUT /admin/documents/{id}/verify` uses application id, even though path says documents.

### AdminService Business Logic

Decision:

- Normalizes status.
- Valid statuses include `APPROVED`, `REJECTED`, `SUBMITTED`, `DOCS_VERIFIED`, `DRAFT`.
- Prevents changing final applications.
- Allows approval only from `DOCS_VERIFIED`.
- Allows rejection only from `SUBMITTED` or `DOCS_VERIFIED`.
- Blocks direct `DOCS_VERIFIED` via decision endpoint.
- Saves admin copy.
- Publishes status update to application-service.

Verify document:

- Requires application status `SUBMITTED`.
- Calls document-service internal exists endpoint.
- If no document exists, throws error.
- Sets status to `DOCS_VERIFIED`.
- Publishes status update.

Reports:

- Counts total, submitted, approved, rejected, docs verified.
- Returns a summary string.

Users:

- Calls auth-service `/internal/users`.
- Updates auth-service `/internal/users/{id}/role`.

### Admin Security

Admin-service has its own JWT authorization filter in addition to gateway checks.

`JwtAuthorizationFilter.java`:

- Allows Swagger and OPTIONS.
- Requires Bearer token.
- Validates token.
- Requires role `ADMIN`.
- Sets Spring Security authentication with `ROLE_ADMIN`.

This means direct calls to admin-service also require admin JWT.

## Config Server

Folder: `config-server`

`ConfigServerApplication.java` uses `@EnableConfigServer`.

It serves config from local native folder:

- `../config-repo`
- `./config-repo`

It also has git config properties for a remote config repository, but active profile defaults to native.

## Service Registry

Folder: `service-registry`

`ServiceRegistryApplication.java` uses `@EnableEurekaServer`.

It runs at port `8761` locally.

Services register themselves here so gateway and load-balanced RestTemplate calls can use logical names like:

- `AUTH-SERVICE`
- `APPLICATION-SERVICE`
- `DOCUMENT-SERVICE`
- `ADMIN-SERVICE`

## Config Repo

Folder: `config-repo`

Contains service configuration by `spring.application.name`:

- `AUTH-SERVICE.properties`
- `APPLICATION-SERVICE.properties`
- `DOCUMENT-SERVICE.properties`
- `ADMIN-SERVICE.properties`
- `api-gateway.properties`
- `service-registry.properties`

The file names must match each service's `spring.application.name`.

## Docker Compose

`docker-compose.yml` starts:

- PostgreSQL
- RabbitMQ
- Zipkin
- service-registry
- config-server
- auth-service
- application-service
- document-service
- admin-service
- api-gateway
- finflow-frontend

PostgreSQL init script creates:

- `auth_db`
- `application_db`
- `document_db`
- `admin_db`

## Testing

Tests are mostly service-layer unit tests with Mockito plus context-load tests.

Important covered behavior:

- Auth signup/login validation.
- Auth duplicate user and invalid password cases.
- JWT validation/extraction in gateway.
- Application create/update/delete/status/ownership cases.
- Document upload/access ownership cases.
- Admin decision rules, report counts, document verification, user management calls.

Testing configs disable config server, Eureka, tracing, and database auto-config where needed.

