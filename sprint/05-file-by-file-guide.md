# File By File Guide

This guide explains the purpose of every important project file. Generated/cache folders like `target`, `node_modules`, `.angular/cache`, `.git`, `.m2`, and IDE metadata are not business logic.

## Root Files

### `.gitignore`

Defines files/folders Git should ignore.

### `.mvn-local-settings.xml`

Local Maven settings file. Usually used to point Maven to a local repository or custom settings.

### `docker-compose.yml`

Defines the full local container stack: PostgreSQL, RabbitMQ, Zipkin, service registry, config server, all backend services, gateway, and Angular frontend.

### `DOCKER_DEPLOYMENT.md`

Short guide for starting/stopping the Docker stack and listing service URLs.

### `SWAGGER_TESTING_GUIDE.md`

Manual API test guide with the expected end-to-end Swagger flow.

## Frontend Root

### `finflow-frontend/package.json`

Defines Angular dependencies and scripts:

- `npm start`: runs Angular dev server on port 4200.
- `npm run build`: production build.
- `npm run watch`: development build watch.

### `finflow-frontend/package-lock.json`

Locks exact npm dependency versions.

### `finflow-frontend/angular.json`

Angular CLI project configuration. It defines build/serve targets, assets, styles, and standalone component defaults.

### `finflow-frontend/tsconfig.json`

Main TypeScript compiler configuration.

### `finflow-frontend/tsconfig.app.json`

TypeScript config for Angular application build.

### `finflow-frontend/.editorconfig`

Editor formatting conventions.

### `finflow-frontend/.dockerignore`

Files excluded from Docker build context.

### `finflow-frontend/Dockerfile`

Builds Angular using Node 22 Alpine, then serves compiled files through Nginx.

### `finflow-frontend/docker/nginx.conf`

Nginx config for serving Angular SPA and redirecting unknown routes to `index.html`.

### `finflow-frontend/docker/docker-entrypoint.sh`

Writes runtime `config.js` with `FINFLOW_API_BASE_URL`.

### `finflow-frontend/public/config.js`

Default browser runtime config. Provides `apiBaseUrl`.

### `finflow-frontend/public/favicon.svg`

Browser tab icon.

### `finflow-frontend/src/index.html`

HTML shell loaded by browser. Angular mounts into this document.

### `finflow-frontend/src/main.ts`

Bootstraps `AppComponent` with `appConfig`.

### `finflow-frontend/src/styles.css`

Global CSS design system: colors, typography, buttons, forms, tables, loading states, utilities, and responsive behavior.

## Frontend App Core

### `src/app/app.component.ts`

Root layout component. Tracks current route and hides navbar on login/signup.

### `src/app/app.component.html`

Renders navbar conditionally, route outlet, and toast outlet.

### `src/app/app.component.css`

Root layout spacing and navbar offset.

### `src/app/app.config.ts`

Registers router, HTTP interceptors, global error listeners, zone change detection, view transitions, scrolling, and API base URL.

### `src/app/app.routes.ts`

Top-level route table for auth, dashboard, applications, profile, admin, and wildcard redirect.

### `core/tokens/api-base-url.token.ts`

Angular injection token used by services to inject backend base URL.

### `core/guards/auth.guard.ts`

Protects routes that require any logged-in user.

### `core/guards/admin.guard.ts`

Protects admin routes and requires `ADMIN` role.

### `core/interceptors/auth.interceptor.ts`

Adds JWT Bearer token to outgoing HTTP requests.

### `core/interceptors/error.interceptor.ts`

Centralizes HTTP error handling and toast messages.

### `core/models/auth.model.ts`

Auth-related TypeScript interfaces and user role type.

### `core/models/application.model.ts`

Loan status, loan type, interest rates, and application request/response interfaces.

### `core/models/document.model.ts`

Document type and document response interface.

### `core/models/admin-user.model.ts`

Admin user table interface.

### `core/models/toast.model.ts`

Toast message and toast type definitions.

### `core/services/auth.service.ts`

Handles login, signup, JWT storage/decoding, profile, password change, logout, and auth state signals.

### `core/services/application.service.ts`

HTTP wrapper for user application endpoints.

### `core/services/document.service.ts`

HTTP wrapper for document upload/download/list APIs.

### `core/services/admin.service.ts`

HTTP wrapper for admin application, document verification, reports, and user management endpoints.

### `core/services/toast.service.ts`

Global toast state and helper methods.

### `core/services/notification.service.ts`

Frontend-only localStorage notification store.

## Frontend Shared

### `shared/components/navbar/*`

Responsive navbar, role-based links, notifications menu, profile dropdown, and logout.

### `shared/components/toast/*`

Visual rendering for toast messages.

### `shared/components/loader/*`

Skeleton loader component.

### `shared/components/status-badge/*`

Reusable status badge for loan application status.

### `shared/components/confirm-dialog/*`

Reusable modal confirmation dialog.

### `shared/pipes/currency-inr.pipe.ts`

Formats numeric values as INR currency.

### `shared/pipes/inr-currency.pipe.ts`

Alternative INR formatter that also accepts strings.

## Frontend Auth Feature

### `features/auth/login/login.component.ts`

Login form logic, validation, submit state, password visibility, role-based navigation.

### `features/auth/login/login.component.html`

Login page template with hero section and form.

### `features/auth/login/login.component.css`

Login page styles.

### `features/auth/signup/signup.component.ts`

Signup form logic, validations, password strength, account creation, routing.

### `features/auth/signup/signup.component.html`

Signup page template with fields and password checklist.

### `features/auth/signup/signup.component.css`

Signup page styles and responsive layout.

## Frontend User Feature

### `features/dashboard/dashboard.component.ts`

User dashboard data loading and computed statistics, EMI calculator, eligibility calculator.

### `features/dashboard/dashboard.component.html`

Dashboard template with stats, calculators, recent applications, quick actions.

### `features/dashboard/dashboard.component.css`

Dashboard layout and visual styles.

### `features/applications/applications.routes.ts`

Child routes for application list, create, and detail pages.

### `features/applications/list/applications-list.component.ts`

Loads user applications, supports search/filter/pagination, submit draft, delete.

### `features/applications/list/applications-list.component.html`

Application list table and filter UI.

### `features/applications/list/applications-list.component.css`

List page styles.

### `features/applications/create/application-create.component.ts`

New loan draft form, EMI preview, localStorage autosave, create API call.

### `features/applications/create/application-create.component.html`

Loan creation form and summary panel.

### `features/applications/create/application-create.component.css`

Create page layout and controls.

### `features/applications/detail/application-detail.component.ts`

Loads a single application, edits draft fields, loads documents, submits/deletes draft.

### `features/applications/detail/application-detail.component.html`

Application detail, edit form, document upload, status actions.

### `features/applications/detail/application-detail.component.css`

Detail page layout and styling.

### `features/documents/upload/upload.component.ts`

Reusable document uploader with drag/drop, validation, document type, upload API call.

### `features/documents/upload/upload.component.html`

Upload widget template.

### `features/documents/upload/upload.component.css`

Upload card/dropzone styles.

### `features/profile/profile.component.ts`

Profile loading, editing, saving, and password change.

### `features/profile/profile.component.html`

Profile and password forms.

### `features/profile/profile.component.css`

Profile page styles.

## Frontend Admin Feature

### `features/admin/admin.routes.ts`

Nested admin routes.

### `features/admin/admin-shell.component.ts`

Admin layout shell and report fetch side effect.

### `features/admin/admin-shell.component.html`

Admin sidebar and nested router outlet.

### `features/admin/admin-shell.component.css`

Admin shell/sidebar styles.

### `features/admin/applications/admin-applications.component.ts`

Admin application list logic, filtering, decisions, bulk decisions, CSV export, notifications.

### `features/admin/applications/admin-applications.component.html`

Admin applications table and actions.

### `features/admin/applications/admin-applications.component.css`

Admin applications styles.

### `features/admin/applications/admin-application-detail.component.ts`

Admin detail logic for one application, documents, notes, document verification, decisions.

### `features/admin/applications/admin-application-detail.component.html`

Admin detail UI.

### `features/admin/applications/admin-application-detail.component.css`

Admin detail styles.

### `features/admin/users/admin-users.component.ts`

Admin user loading, searching, role drafts, and role save.

### `features/admin/users/admin-users.component.html`

User management table.

### `features/admin/users/admin-users.component.css`

User management styles.

### `features/admin/reports/admin-reports.component.ts`

Loads admin applications and computes report data.

### `features/admin/reports/admin-reports.component.html`

Reports display with bar and status list.

### `features/admin/reports/admin-reports.component.css`

Reports page styling.

## Backend Common Service Files

Every Spring Boot service has:

- `pom.xml`: Maven dependencies and Java version.
- `Dockerfile`: container build.
- `mvnw` and `mvnw.cmd`: Maven wrapper.
- `.mvn/wrapper/maven-wrapper.properties`: Maven wrapper config.
- `src/main/resources/application.properties`: local/default service config.
- `src/test/resources/application.properties`: test-specific config.
- `*Application.java`: service entry point.
- `GlobalExceptionHandler.java`: consistent error response.

## Auth Service Files

### `auth-service/pom.xml`

Dependencies for web, security, JPA, validation, JWT, Eureka, Config, PostgreSQL, Swagger, tests, tracing.

### `AuthServiceApplication.java`

Starts auth-service.

### `CONFIG/AdminBootstrapConfig.java`

Creates default admin user on startup.

### `CONTROLLER/AuthController.java`

Public login/signup and protected profile/password endpoints.

### `CONTROLLER/InternalUserController.java`

Internal user list and role update endpoints for admin-service.

### `DTOs/*.java`

Request and response classes with validation annotations.

### `Entity/User.java`

JPA entity for users.

### `REPOSITORY/UserRepository.java`

JPA repository for user lookup and listing.

### `SECURITY_CONFIG/CorsConfig.java`

Auth-service CORS rules.

### `SECURITY_CONFIG/SecurityConfig.java`

Spring Security rules and BCrypt password encoder.

### `SECURITY_CONFIG/SwaggerConfig.java`

OpenAPI server config.

### `SERVICE_LAYER/AuthService.java`

Core auth business logic.

### `UTIL/JwtUtil.java`

JWT create, validate, email extract, and role extract.

### Auth Tests

- `AuthServiceApplicationTests.java`: context load with mocks.
- `AuthControllerValidationTest.java`: invalid request validation.
- `AuthServiceTest.java`: signup/login service behavior.

## Application Service Files

### `application-service/pom.xml`

Dependencies for web, JPA, validation, RabbitMQ, ModelMapper, Eureka, Config, Swagger, tracing.

### `ApplicationServiceApplication.java`

Starts application-service.

### `config/CorsConfig.java`

CORS rules.

### `config/ModelMapperConfig.java`

Provides ModelMapper bean.

### `config/RabbitConfig.java`

Queue constants and JSON message converter.

### `config/SwaggerConfig.java`

OpenAPI and bearer auth scheme.

### `controller/ApplicationController.java`

User-facing application CRUD/status/submit endpoints.

### `controller/InternalApplicationController.java`

Internal owner lookup for document-service.

### `dto/*.java`

Request, response, owner, RabbitMQ message, and status update DTOs.

### `entity/LoanApplication.java`

JPA entity for user loan applications.

### `repository/ApplicationRepository.java`

JPA repository with `findByApplicantName`.

### `listener/ApplicationStatusUpdateListener.java`

Consumes admin status updates from RabbitMQ.

### `service/ApplicationService.java`

Core application lifecycle logic and RabbitMQ publishing.

### Application Tests

- `ApplicationServiceApplicationTests.java`: context load.
- `ApplicationServiceTest.java`: create/update/delete/ownership/status behavior.

## Document Service Files

### `document-service/pom.xml`

Dependencies for web, JPA, ModelMapper, Eureka, Config, PostgreSQL, Swagger, tracing.

### `DocumentServiceApplication.java`

Starts document-service.

### `config/CorsConfig.java`

CORS rules.

### `config/ModelMapperConfig.java`

Provides ModelMapper bean.

### `config/RestTemplateConfig.java`

Provides load-balanced RestTemplate for service-to-service calls.

### `config/SwaggerConfig.java`

OpenAPI bearer auth scheme.

### `controller/DocumentController.java`

Upload/download/list/exists document APIs.

### `dto/ApplicationOwnerResponseDTO.java`

Response shape from application-service owner lookup.

### `dto/DocumentResponseDTO.java`

Document metadata response.

### `entity/Document.java`

JPA entity for uploaded document metadata and binary content.

### `repository/DocumentRepository.java`

Document repository and metadata query.

### `service/DocumentService.java`

Document save/get/list logic with application ownership checks.

### Document Tests

- `DocumentServiceApplicationTests.java`: context and OpenAPI check.
- `DocumentServiceTest.java`: save/get/access control/list metadata behavior.

## Admin Service Files

### `admin-service/pom.xml`

Dependencies for web, security, JPA, validation, RabbitMQ, RestTemplate, JWT, Eureka, Config, Swagger, tracing.

### `AdminServiceApplication.java`

Starts admin-service and enables Rabbit.

### `config/CorsConfig.java`

CORS rules.

### `config/JwtAuthorizationFilter.java`

Requires valid admin JWT for direct admin-service calls.

### `config/JwtService.java`

JWT validation and role extraction.

### `config/RabbitConfig.java`

Queues and JSON converter.

### `config/RestTemplateConfig.java`

Load-balanced RestTemplate bean.

### `config/SecurityConfig.java`

Registers JWT filter and stateless security.

### `config/SwaggerConfig.java`

OpenAPI bearer auth scheme.

### `controller/AdminController.java`

Admin application, decision, document verification, reports, and user endpoints.

### `dto/*.java`

Payloads for notes, decisions, bulk decisions, user update, user response, and Rabbit status messages.

### `entity/Application.java`

Admin-side synced application entity.

### `listener/ApplicationListener.java`

Consumes application snapshots from application-service.

### `repository/ApplicationRepository.java`

Admin JPA repository, native upsert, status counts.

### `service/AdminService.java`

Admin business logic: decisions, document verification, reports, user proxy calls.

### Admin Tests

- `AdminServiceApplicationTests.java`: context load.
- `AdminServiceTest.java`: decision rules, document verify, reports, user calls.

## API Gateway Files

### `api-gateway/pom.xml`

Dependencies for WebFlux gateway, security, JWT, Eureka, Config, Swagger UI, tracing.

### `ApiGatewayApplication.java`

Starts gateway.

### `config/CorsConfig.java`

Reactive CORS filter.

### `config/JwtFilter.java`

Global JWT validation and user header injection.

### `config/SecurityConfig.java`

Permits all exchanges because custom filter handles auth.

### `exception/GlobalExceptionHandler.java`

Generic gateway error response.

### `service/JwtService.java`

JWT validation/email/role extraction.

### Gateway Tests

- `ApiGatewayApplicationTests.java`: context load.
- `JwtServiceTest.java`: token validate/extract tests.

## Config Server Files

### `config-server/pom.xml`

Dependencies for Spring Cloud Config Server, Eureka client, actuator, tracing.

### `ConfigServerApplication.java`

Enables config server.

### `exception/GlobalExceptionHandler.java`

Generic error handler.

### `src/main/resources/application.properties`

Config server port, Eureka, native config repo locations, optional git config.

### Config Server Tests

- `ConfigServerApplicationTests.java`
- test properties disable external config/eureka/tracing.

## Service Registry Files

### `service-registry/pom.xml`

Dependencies for Eureka server, tests, actuator, tracing.

### `ServiceRegistryApplication.java`

Enables Eureka server.

### `exception/GlobalExceptionHandler.java`

Generic error handler.

### `src/main/resources/application.properties`

Runs registry on port 8761 and disables self-registration.

### Service Registry Tests

- `ServiceRegistryApplicationTests.java`

## Config Repo Files

### `config-repo/README.md`

Explains config file naming.

### `config-repo/AUTH-SERVICE.properties`

Auth service port, database, JWT, default admin, tracing.

### `config-repo/APPLICATION-SERVICE.properties`

Application service database, RabbitMQ, Eureka, tracing.

### `config-repo/DOCUMENT-SERVICE.properties`

Document service database, application-service URL, tracing.

### `config-repo/ADMIN-SERVICE.properties`

Admin service database, RabbitMQ, JWT, document/auth service URLs.

### `config-repo/api-gateway.properties`

Gateway routes, Swagger aggregation, JWT secret.

### `config-repo/service-registry.properties`

Eureka server config.

## Docker Support

### `docker/postgres/init-multiple-dbs.sh`

Creates all four PostgreSQL databases on first container startup.

### Service Dockerfiles

Each backend service Dockerfile builds and runs the service jar for container deployment.

