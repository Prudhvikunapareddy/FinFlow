# FinFlow End-to-End Interview Master Guide

This file is the single place to revise the FinFlow project from basics to full end-to-end flow. Read it slowly in this order:

1. What the project is
2. Why this architecture is used
3. What each service does
4. How one request flows through the system
5. How code is organized
6. What Java, Spring Boot, database, security, messaging, Docker, and CI/CD concepts are used
7. How to answer interview questions

---

## 1. Project Introduction

FinFlow is a loan management system built using a microservices architecture.

In simple words:

> FinFlow allows users to register, login, create loan applications, upload supporting documents, and track application status. Admin users can view applications, verify documents, add notes, approve or reject applications, view reports, and manage users.

Main technology stack:

| Layer | Technology |
|---|---|
| Frontend | Angular |
| Backend | Java, Spring Boot |
| Architecture | Microservices |
| API Routing | Spring Cloud Gateway |
| Service Discovery | Eureka |
| Central Config | Spring Cloud Config Server |
| Database | PostgreSQL |
| Messaging | RabbitMQ |
| Authentication | JWT, Spring Security |
| ORM | Spring Data JPA, Hibernate |
| API Docs | Swagger/OpenAPI |
| Monitoring/Tracing | Actuator, Zipkin |
| Code Quality | SonarQube |
| Containerization | Docker, Docker Compose |
| CI/CD | GitHub Actions |

---

## 2. One-Line Interview Answer

If the interviewer asks, "Tell me about your project", answer like this:

> FinFlow is a microservices-based loan management application. Users can sign up, login, create loan applications, upload documents, and track their loan status. Admins can verify documents, approve or reject applications, manage users, and view reports. The frontend is built in Angular, backend services are built in Spring Boot, PostgreSQL stores data, RabbitMQ is used for asynchronous communication, Eureka is used for service discovery, API Gateway routes requests, JWT secures APIs, Docker Compose runs the whole system, and GitHub Actions is used for CI/CD.

---

## 3. High-Level Architecture

Basic system diagram:

```text
Angular Frontend
      |
      v
API Gateway
      |
      +------------------ Auth Service
      |
      +------------------ Application Service
      |
      +------------------ Document Service
      |
      +------------------ Admin Service

Supporting Services:

Service Registry / Eureka
Config Server
PostgreSQL
RabbitMQ
Zipkin
SonarQube
Docker
GitHub Actions
```

Runtime flow:

```text
Browser
  -> Angular frontend
  -> API Gateway
  -> Required backend service
  -> Database / RabbitMQ / another service
  -> Response returns back to frontend
```

---

## 4. Why Microservices?

Instead of keeping everything in one big backend application, FinFlow splits the system into separate services.

Benefits:

- Each service has one clear responsibility.
- Auth logic is separate from loan application logic.
- Document upload logic is separate from admin decision logic.
- Services can be developed, tested, deployed, and scaled independently.
- Failure in one service does not always mean the whole system is down.
- It is easier to maintain large applications.

Interview answer:

> We used microservices because FinFlow has independent business areas like authentication, loan applications, document management, and admin operations. Splitting them into services improves maintainability, scalability, and separation of concerns.

---

## 5. Project Folder Structure

Root-level folders:

```text
FinFlow
  admin-service
  api-gateway
  application-service
  auth-service
  config-repo
  config-server
  docker
  document-service
  finflow-frontend
  service-registry
  sprint
  .github/workflows
  docker-compose.yml
```

Meaning:

| Folder | Purpose |
|---|---|
| `auth-service` | User signup, login, profile, password, JWT |
| `application-service` | Loan application create, update, submit, status |
| `document-service` | Upload and fetch loan documents |
| `admin-service` | Admin approval, rejection, notes, reports, users |
| `api-gateway` | Common entry point and routing |
| `service-registry` | Eureka server for service discovery |
| `config-server` | Central configuration server |
| `config-repo` | Configuration files served by config server |
| `finflow-frontend` | Angular frontend |
| `docker` | Docker helper files |
| `sprint` | Documentation and interview notes |
| `.github/workflows` | CI/CD pipeline |
| `docker-compose.yml` | Runs all services together |

---

## 6. Service-by-Service Explanation

### 6.1 Auth Service

Path:

```text
auth-service
```

Main responsibility:

- User signup
- User login
- JWT token generation
- Profile view/update
- Password change
- User role updates through internal admin endpoint
- Admin user bootstrap

Important packages:

```text
CONTROLLER
DTOs
Entity
REPOSITORY
SERVICE_LAYER
SECURITY_CONFIG
UTIL
EXCEPTION
```

Important files:

| File | Purpose |
|---|---|
| `AuthController.java` | Public auth APIs |
| `InternalUserController.java` | Internal APIs used by admin service |
| `AuthService.java` | Business logic for signup, login, profile |
| `User.java` | JPA entity for users table |
| `UserRepository.java` | Database access for users |
| `JwtUtil.java` | Generates and validates JWT |
| `SecurityConfig.java` | Spring Security configuration |
| `JwtAuthenticationFilter.java` | Reads JWT and authenticates request |
| `AdminBootstrapConfig.java` | Creates default admin if needed |

Main APIs:

```text
POST /auth/signup
POST /auth/login
GET  /auth/profile
PUT  /auth/profile
PUT  /auth/password

GET  /internal/users
PUT  /internal/users/{id}/role
```

Signup flow:

```text
Frontend signup form
  -> POST /auth/signup
  -> AuthController.signup()
  -> AuthService.signup()
  -> Validate email and age
  -> Encode password using BCrypt
  -> Save User in PostgreSQL
  -> Generate JWT token
  -> Return token to frontend
  -> Frontend stores token in localStorage
```

Login flow:

```text
Frontend login form
  -> POST /auth/login
  -> AuthController.login()
  -> AuthService.login()
  -> Find user by email
  -> Compare raw password with encoded password
  -> Generate JWT
  -> Return token
```

Interview answer:

> Auth service handles user identity. It validates signup/login requests, stores users in PostgreSQL, encrypts passwords using BCrypt, and generates JWT tokens. Other services use those JWT tokens to identify the logged-in user.

---

### 6.2 Application Service

Path:

```text
application-service
```

Main responsibility:

- Create loan application
- Update loan application
- Delete draft application
- Submit application
- Get application status
- Return owner information to document service
- Sync status updates received from admin service
- Publish application changes to RabbitMQ

Important files:

| File | Purpose |
|---|---|
| `ApplicationController.java` | User-facing application APIs |
| `InternalApplicationController.java` | Internal APIs for other services |
| `ApplicationService.java` | Business logic |
| `LoanApplication.java` | JPA entity |
| `ApplicationRepository.java` | Database access |
| `ApplicationMessageDTO.java` | Message sent to admin service |
| `ApplicationStatusUpdateDTO.java` | Status update message |
| `ApplicationStatusUpdateListener.java` | Listens for admin status updates |
| `RabbitConfig.java` | RabbitMQ queues |
| `JwtAuthenticationFilter.java` | Security filter |

Main APIs:

```text
POST   /applications
GET    /applications
GET    /applications/{id}
PUT    /applications/{id}
DELETE /applications/{id}
POST   /applications/{id}/submit
GET    /applications/my
GET    /applications/{id}/status
GET    /internal/applications/{id}/owner
```

Loan statuses:

```text
DRAFT
SUBMITTED
DOCS_VERIFIED
APPROVED
REJECTED
```

Create application flow:

```text
Frontend create application form
  -> POST /applications
  -> API Gateway routes to application-service
  -> JwtAuthenticationFilter validates token
  -> ApplicationController.create()
  -> ApplicationService.create()
  -> Normalize input values
  -> Set applicant email from JWT
  -> Set status as DRAFT
  -> Save in PostgreSQL
  -> Publish application snapshot to RabbitMQ
  -> Return ApplicationResponseDTO
```

Submit application flow:

```text
User clicks submit
  -> POST /applications/{id}/submit
  -> ApplicationService.submit()
  -> Check application belongs to logged-in user
  -> Check current status is DRAFT
  -> Change status to SUBMITTED
  -> Save application
  -> Publish update to RabbitMQ
  -> Return updated application
```

Interview answer:

> Application service manages the loan application lifecycle. It stores loan applications, ensures only the owner can update or submit them, validates loan type and tenure, and publishes application changes to RabbitMQ so the admin service has a synchronized copy.

---

### 6.3 Document Service

Path:

```text
document-service
```

Main responsibility:

- Upload documents for an application
- Store document metadata and binary file data
- Check whether an application has documents
- Return document metadata
- Validate that a user can access documents for an application

Important files:

| File | Purpose |
|---|---|
| `DocumentController.java` | Document APIs |
| `DocumentService.java` | Business logic |
| `Document.java` | JPA entity |
| `DocumentRepository.java` | Database access |
| `DocumentResponseDTO.java` | Response without raw binary data |
| `RestTemplateConfig.java` | Used to call application service |
| `JwtAuthenticationFilter.java` | Security filter |

Main APIs:

```text
POST /documents/upload
GET  /documents/{id}
GET  /documents/applications/{applicationId}/exists
GET  /documents/internal/applications/{applicationId}/exists
GET  /documents/applications/{applicationId}
```

Upload document flow:

```text
Frontend upload form
  -> POST /documents/upload with multipart file
  -> API Gateway routes to document-service
  -> JwtAuthenticationFilter validates JWT
  -> DocumentController.upload()
  -> DocumentService.save()
  -> Validate application access
       if ADMIN -> allow
       if USER -> call application-service owner endpoint
  -> Save file name, type, document type, application id, uploaded email, binary data
  -> Return metadata response
```

Why document service calls application service:

It needs to confirm that the logged-in user owns the loan application before allowing upload or access.

Interview answer:

> Document service handles file uploads separately from application logic. It validates access by checking the application owner from application-service, stores the file data in PostgreSQL as a LOB, and returns metadata to the frontend.

---

### 6.4 Admin Service

Path:

```text
admin-service
```

Main responsibility:

- Maintain admin-side copy of loan applications
- View all applications
- Approve or reject applications
- Add admin notes
- Verify documents
- Generate basic reports
- View and update users through auth service
- Publish status updates back to application-service

Important files:

| File | Purpose |
|---|---|
| `AdminController.java` | Admin APIs |
| `AdminService.java` | Admin business logic |
| `Application.java` | Admin-side application entity |
| `ApplicationRepository.java` | Database access and report queries |
| `ApplicationListener.java` | Listens to application changes from RabbitMQ |
| `ApplicationStatusUpdateDTO.java` | Status update message |
| `BulkDecisionRequestDTO.java` | Bulk approve/reject request |
| `RabbitConfig.java` | RabbitMQ queues |
| `JwtAuthorizationFilter.java` | Checks admin access |

Main APIs:

```text
GET  /admin/applications
GET  /admin/applications/{id}
POST /admin/applications/{id}/decision
POST /admin/applications/bulk-decision
PUT  /admin/applications/{id}/notes
PUT  /admin/documents/{id}/verify
GET  /admin/reports
GET  /admin/users
PUT  /admin/users/{id}
```

Admin application sync flow:

```text
Application service creates/updates/deletes application
  -> Publishes message to application_queue
  -> Admin service ApplicationListener receives message
  -> Admin service inserts/updates/deletes local Application entity
  -> Admin dashboard can view latest application data
```

Admin decision flow:

```text
Admin clicks approve/reject
  -> POST /admin/applications/{id}/decision
  -> AdminService.decision()
  -> Validate status transition
  -> Save admin-side application status
  -> Publish status update to application_status_update_queue
  -> Application service listener receives message
  -> Application service updates original loan application status
```

Document verification flow:

```text
Admin clicks verify document
  -> PUT /admin/documents/{id}/verify
  -> AdminService.verifyDocument()
  -> Check application status is SUBMITTED
  -> Call document-service to check if documents exist
  -> If documents exist, set status DOCS_VERIFIED
  -> Publish status update to application-service
```

Interview answer:

> Admin service is responsible for back-office loan operations. It receives application snapshots through RabbitMQ, stores an admin-side copy, allows admins to verify documents and make decisions, then sends final status updates back to application-service using RabbitMQ.

---

### 6.5 API Gateway

Path:

```text
api-gateway
```

Main responsibility:

- Single entry point for frontend
- Route requests to correct microservice
- Validate JWT for protected APIs
- Add user email and role headers
- Block non-admin users from admin APIs
- Aggregate Swagger docs routes

Important files:

| File | Purpose |
|---|---|
| `ApiGatewayApplication.java` | Main Spring Boot app |
| `JwtFilter.java` | Global gateway filter |
| `JwtService.java` | JWT validation helper |
| `application.properties` | Gateway route definitions |
| `CorsConfig.java` | CORS config |

Gateway routes:

```text
/auth/** or /gateway/auth/**              -> auth-service
/applications/** or /gateway/applications/** -> application-service
/documents/** or /gateway/documents/**    -> document-service
/admin/** or /gateway/admin/**            -> admin-service
```

JWT gateway flow:

```text
Frontend sends request with Authorization: Bearer <token>
  -> API Gateway JwtFilter runs
  -> If public endpoint, allow
  -> Else validate token
  -> Extract email and role
  -> If admin path and role is not ADMIN, return 403
  -> Add X-User-Email and X-User-Role headers
  -> Route request to backend service
```

Interview answer:

> API Gateway acts as the single entry point. It hides internal service URLs from the frontend, validates JWT tokens, routes requests using Eureka service names, and forwards user details through headers.

---

### 6.6 Service Registry

Path:

```text
service-registry
```

Main responsibility:

- Eureka server
- Allows services to register themselves
- Allows gateway to find services dynamically

Example:

Instead of hardcoding:

```text
http://localhost:8081
```

Gateway can use:

```text
lb://AUTH-SERVICE
```

Interview answer:

> Eureka is used for service discovery. Each service registers with Eureka, and the gateway can route using service names instead of fixed IP addresses or ports.

---

### 6.7 Config Server and Config Repo

Paths:

```text
config-server
config-repo
```

Main responsibility:

- Centralized configuration
- Services load config from config server
- Easier to manage environment-specific properties

Interview answer:

> Config server centralizes configuration so each microservice does not need to duplicate environment properties. It helps manage configuration consistently across services.

---

### 6.8 Frontend

Path:

```text
finflow-frontend
```

Technology:

```text
Angular
TypeScript
HTML
CSS
RxJS
```

Important folders:

| Folder | Purpose |
|---|---|
| `core/services` | API calls |
| `core/guards` | Route protection |
| `core/interceptors` | Add JWT and handle errors |
| `core/models` | TypeScript interfaces |
| `features/auth` | Login and signup pages |
| `features/applications` | Loan application UI |
| `features/documents` | Upload documents |
| `features/admin` | Admin dashboard |
| `features/profile` | Profile page |
| `shared/components` | Reusable UI components |

Important frontend files:

| File | Purpose |
|---|---|
| `app.routes.ts` | Defines frontend routes |
| `auth.service.ts` | Login, signup, token handling |
| `application.service.ts` | Loan application API calls |
| `document.service.ts` | Document API calls |
| `admin.service.ts` | Admin API calls |
| `auth.interceptor.ts` | Adds JWT token to requests |
| `auth.guard.ts` | Blocks unauthenticated users |
| `admin.guard.ts` | Blocks non-admin users |

Frontend route flow:

```text
/login
/signup
/dashboard
/applications
/applications/create
/applications/:id
/profile
/admin
```

Frontend auth flow:

```text
User logs in
  -> auth.service.ts calls /auth/login
  -> Backend returns JWT
  -> Token stored in localStorage
  -> Auth interceptor adds token to future requests
  -> Guards allow protected routes
```

Interview answer:

> The Angular frontend is responsible for user interaction. It uses services to call backend APIs, interceptors to attach JWT tokens, guards to protect routes, and components to display login, applications, documents, profile, and admin screens.

---

## 7. Complete End-to-End Business Flow

This is the most important part for interviews.

### 7.1 User Registration

```text
User opens signup page
  -> Enters name, email, date of birth, phone, password
  -> Angular calls POST /auth/signup
  -> Gateway routes to auth-service
  -> AuthService validates:
       email not already registered
       age must be at least 18
       password is encoded
  -> User saved in PostgreSQL
  -> JWT generated
  -> JWT returned to frontend
  -> Frontend stores token
```

### 7.2 User Login

```text
User enters email and password
  -> Angular calls POST /auth/login
  -> Auth service finds user by email
  -> PasswordEncoder verifies password
  -> JWT generated with email and role
  -> Frontend stores token
```

JWT contains:

```text
subject/email
role
issued time
expiry time
signature
```

### 7.3 Create Loan Application

```text
User opens create application page
  -> Enters loan name, amount, loan type, tenure
  -> Angular sends POST /applications with JWT
  -> Gateway validates JWT
  -> Gateway forwards X-User-Email
  -> Application service creates LoanApplication
  -> Status set to DRAFT
  -> Data saved in PostgreSQL
  -> Message sent to RabbitMQ application_queue
  -> Admin service receives message and stores admin-side copy
```

### 7.4 Submit Loan Application

```text
User clicks Submit
  -> POST /applications/{id}/submit
  -> Application service checks owner
  -> Application must be DRAFT
  -> Status changes to SUBMITTED
  -> Data saved
  -> RabbitMQ message sent
  -> Admin service syncs updated status
```

### 7.5 Upload Documents

```text
User uploads salary slip, bank statement, ID proof, etc.
  -> POST /documents/upload
  -> Gateway validates JWT
  -> Document service validates application access
  -> If user, document service calls application-service internal owner API
  -> If owner matches, document is saved
  -> File stored as binary data in PostgreSQL
```

### 7.6 Admin Views Applications

```text
Admin logs in
  -> JWT role is ADMIN
  -> Angular admin guard allows /admin route
  -> Admin dashboard calls /admin/applications
  -> Gateway checks role ADMIN
  -> Admin service returns all applications
```

### 7.7 Admin Verifies Documents

```text
Admin clicks verify documents
  -> PUT /admin/documents/{applicationId}/verify
  -> Admin service checks application is SUBMITTED
  -> Admin service calls document-service internal exists endpoint
  -> If documents exist, status becomes DOCS_VERIFIED
  -> Admin service publishes status update
  -> Application service listener updates original application
```

### 7.8 Admin Approves or Rejects

Approve:

```text
Admin clicks approve
  -> Admin service checks status is DOCS_VERIFIED
  -> Status becomes APPROVED
  -> Status update sent to RabbitMQ
  -> Application service updates user-facing status
```

Reject:

```text
Admin clicks reject
  -> Admin service checks status is SUBMITTED or DOCS_VERIFIED
  -> Status becomes REJECTED
  -> Status update sent to RabbitMQ
  -> Application service updates user-facing status
```

### 7.9 User Checks Status

```text
User opens application details
  -> Angular calls GET /applications/{id}/status
  -> Application service checks ownership
  -> Returns latest status
```

---

## 8. RabbitMQ Messaging Flow

RabbitMQ is used for asynchronous communication between application-service and admin-service.

Queues:

```text
application_queue
application_status_update_queue
```

Flow 1: Application service to Admin service

```text
application-service
  -> publishes application snapshot to application_queue
  -> admin-service listens
  -> admin-service saves/updates local copy
```

Flow 2: Admin service to Application service

```text
admin-service
  -> publishes status update to application_status_update_queue
  -> application-service listens
  -> application-service updates original loan application
```

Why RabbitMQ?

- Decouples services
- Avoids direct dependency for every update
- Helps async processing
- Improves scalability

Interview answer:

> RabbitMQ is used to keep application-service and admin-service synchronized asynchronously. When a user creates or updates an application, application-service publishes a message. Admin-service consumes it. When admin changes the status, admin-service publishes a status update and application-service consumes it.

---

## 9. Database Design

Each service owns its own data model.

Main entities:

| Entity | Service | Meaning |
|---|---|---|
| `User` | auth-service | Registered user/admin |
| `LoanApplication` | application-service | Original user loan application |
| `Document` | document-service | Uploaded document |
| `Application` | admin-service | Admin-side synced application copy |

Important entity fields:

### User

```text
id
email
password
role
firstName
lastName
dateOfBirth
phoneNumber
referralCode
createdAt
```

### LoanApplication

```text
id
name
applicantName
amount
loanType
tenureMonths
status
adminNotes
```

### Document

```text
id
fileName
fileType
documentType
data
applicationId
uploadedByEmail
```

### Admin Application

```text
id
name
applicantName
amount
loanType
tenureMonths
adminNotes
status
```

Interview answer:

> We used JPA entities to map Java classes to database tables. Each service has its own entity and repository. This follows microservice ownership, where each service owns its own data.

---

## 10. Security Flow

Security is based on JWT.

JWT full form:

```text
JSON Web Token
```

Why JWT?

- Stateless authentication
- No server-side session needed
- Works well with microservices
- Can carry user identity and role

Security flow:

```text
User logs in
  -> Auth service returns JWT
  -> Frontend stores JWT
  -> Frontend sends JWT in Authorization header
  -> Gateway validates JWT
  -> Gateway forwards email and role headers
  -> Services use those headers for ownership and authorization
```

Header example:

```text
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Gateway adds:

```text
X-User-Email: user@example.com
X-User-Role: USER
```

Admin protection:

```text
If path starts with /admin and role is not ADMIN
  -> return 403 Forbidden
```

Interview answer:

> Authentication is done using JWT. After login, auth-service generates a token with the user's email and role. The frontend sends the token in every request. API Gateway validates the token and forwards user identity to downstream services through headers.

---

## 11. Spring Boot Concepts Used

### `@SpringBootApplication`

Used in main classes to start each service.

Meaning:

- Enables auto-configuration
- Enables component scanning
- Marks the app as a Spring Boot application

### `@RestController`

Used for REST APIs.

Example:

```java
@RestController
@RequestMapping("/applications")
public class ApplicationController
```

### `@Service`

Used for business logic classes.

Example:

```java
@Service
public class ApplicationService
```

### `@Repository`

Spring Data repositories are interfaces extending `JpaRepository`.

Example:

```java
public interface UserRepository extends JpaRepository<User, Long>
```

### `@Entity`

Maps Java class to database table.

Example:

```java
@Entity
public class LoanApplication
```

### `@Autowired`

Used for dependency injection.

Example:

```java
@Autowired
private UserRepository userRepository;
```

### `@Bean`

Creates Spring-managed objects.

Example:

```java
@Bean
public BCryptPasswordEncoder passwordEncoder()
```

### `@RabbitListener`

Used to consume RabbitMQ messages.

Example:

```java
@RabbitListener(queues = RabbitConfig.STATUS_UPDATE_QUEUE)
```

---

## 12. Java OOP Concepts Used

### Class and Object

Classes:

```text
User
LoanApplication
Document
AuthService
ApplicationService
AdminService
```

Object creation example:

```java
User user = new User();
```

### Encapsulation

Private fields with public getters and setters.

Example:

```java
private String email;

public String getEmail() {
    return email;
}

public void setEmail(String email) {
    this.email = email;
}
```

### Inheritance

Filters extend Spring classes.

Example:

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter
```

### Interface

Repositories are interfaces.

Example:

```java
public interface UserRepository extends JpaRepository<User, Long>
```

### Polymorphism

Overriding methods.

Example:

```java
@Override
protected void doFilterInternal(...)
```

### Abstraction

Controller does not know database details. It only calls service.

Example:

```java
return service.create(dto, email);
```

The internal save, validation, RabbitMQ publish, and DTO mapping are hidden in service layer.

---

## 13. Collections Used

### List

Used for multiple records.

Examples:

```java
List<ApplicationResponseDTO>
List<UserResponse>
List<Long> ids
```

Where used:

- Returning all applications
- Returning users
- Bulk decision IDs
- Allowed status/type validation with `List.of()`

### Map

Used for key-value data.

Examples:

```java
Map<String, Object>
Map<String, String>
```

Where used:

- Exception response body
- JWT claims

### HashMap

Used in JWT claims.

```java
Map<String, Object> claims = new HashMap<>();
```

### LinkedHashMap

Used in exception handlers to preserve insertion order.

```java
Map<String, Object> body = new LinkedHashMap<>();
```

### Optional

Used by repository methods.

```java
Optional<User> findByEmail(String email)
```

### Stream API

Used for mapping lists.

```java
repository.findAll()
    .stream()
    .map(app -> modelMapper.map(app, ApplicationResponseDTO.class))
    .toList();
```

Interview answer:

> Collections are used to handle groups of data. We use List for multiple users or applications, Map for structured response bodies and JWT claims, Optional for safe repository results, and Stream API for converting entities to DTOs.

---

## 14. DTO Explanation

DTO full form:

```text
Data Transfer Object
```

Why DTO is used:

- Avoid exposing entity directly
- Control request and response structure
- Add validation
- Separate API model from database model

Examples:

```text
SignupRequest
LoginRequest
ApplicationRequestDTO
ApplicationResponseDTO
DocumentResponseDTO
UserResponse
BulkDecisionRequestDTO
```

Interview answer:

> DTOs are used to transfer data between frontend and backend without exposing internal entity classes directly. They also help apply validation and keep API contracts clean.

---

## 15. Repository and JPA

Repository example:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```

What `JpaRepository` gives automatically:

```text
save()
findById()
findAll()
delete()
deleteById()
count()
```

Custom query method examples:

```java
findByEmail(String email)
findByApplicantName(String applicantName)
existsByApplicationId(Long applicationId)
countByStatus(String status)
```

Interview answer:

> Spring Data JPA reduces boilerplate database code. By extending JpaRepository, we get CRUD methods automatically. We also define custom finder methods using naming conventions like findByEmail and countByStatus.

---

## 16. Exception Handling

Services have global exception handlers.

Purpose:

- Catch validation errors
- Catch runtime errors
- Return consistent JSON responses
- Map errors to HTTP status codes

Example response body:

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed"
}
```

Interview answer:

> Global exception handling is used to return consistent error responses across APIs. Instead of writing try-catch in every controller, exceptions are handled centrally using `@RestControllerAdvice`.

---

## 17. Validation

Validation is used in DTOs.

Common annotations:

```text
@Valid
@NotBlank
@NotEmpty
```

Where used:

- Signup request
- Login request
- Application request
- Bulk admin decision
- Password change

Interview answer:

> Validation ensures incorrect data does not enter the business logic. Controllers use `@Valid`, and DTO fields use annotations like `@NotBlank` and `@NotEmpty`.

---

## 18. Docker and Docker Compose

Docker is used to containerize services.

Each service has a `Dockerfile`.

Docker Compose runs the full system:

```text
postgres
rabbitmq
zipkin
sonarqube
service-registry
config-server
auth-service
application-service
document-service
admin-service
api-gateway
finflow-frontend
```

Common ports:

```text
Frontend:          4200
API Gateway:       9083 -> 8083
Auth Service:      9081 -> 8081
Application:       9084 -> 8084
Document:          9085 -> 8085
Admin:             9086 -> 8086
Eureka:            9761 -> 8761
Config Server:     9888 -> 8888
PostgreSQL:        5432
RabbitMQ:          5672, 15672
Zipkin:            9412 -> 9411
SonarQube:         9000
```

Interview answer:

> Docker is used to package each service with its runtime dependencies. Docker Compose is used to start all services together, including PostgreSQL, RabbitMQ, Eureka, Config Server, backend services, and frontend.

---

## 19. CI/CD

CI/CD file:

```text
.github/workflows/ci-cd.yml
```

CI means:

```text
Continuous Integration
```

It checks whether code builds and tests pass.

CD means:

```text
Continuous Delivery / Deployment
```

It prepares or deploys the application automatically.

Current pipeline does:

```text
On pull request:
  -> Build and test backend services
  -> Build Angular frontend
  -> Upload artifacts

On push to main/master/develop or version tag:
  -> Run CI
  -> Build Docker images
  -> Push Docker images to GitHub Container Registry
```

Interview answer:

> We implemented GitHub Actions for CI/CD. It builds and tests every Spring Boot service, builds the Angular frontend, uploads artifacts, and publishes Docker images to GitHub Container Registry after successful pushes.

---

## 20. SonarQube

SonarQube is used for code quality analysis.

It checks:

- Bugs
- Vulnerabilities
- Code smells
- Test coverage
- Duplications

Files:

```text
sonar-project.properties
scan-sonarqube-services.sh
scan-sonarqube-services.ps1
service-specific sonar-project.properties files
```

Interview answer:

> SonarQube is used to analyze code quality. It helps identify bugs, vulnerabilities, code smells, duplication, and coverage issues.

---

## 21. Testing

Test folders:

```text
src/test/java
```

Testing tools:

```text
JUnit
Mockito
Spring Boot Test
```

What tests cover:

- Service logic
- Controllers validation
- JWT utilities
- Listener behavior
- Global exception handlers

Interview answer:

> We wrote unit tests for service logic and important components using JUnit and Mockito. Tests help confirm business rules like login, application status changes, document access, and admin decisions.

---

## 22. Status Lifecycle

Application status flow:

```text
DRAFT
  -> SUBMITTED
  -> DOCS_VERIFIED
  -> APPROVED
```

Rejection flow:

```text
DRAFT
  -> SUBMITTED
  -> REJECTED
```

Another rejection flow:

```text
DRAFT
  -> SUBMITTED
  -> DOCS_VERIFIED
  -> REJECTED
```

Rules:

- User can update/delete only DRAFT applications.
- User can submit only DRAFT applications.
- Admin can approve only after DOCS_VERIFIED.
- Admin can reject SUBMITTED or DOCS_VERIFIED applications.
- Admin cannot approve/reject again after final decision.

Interview answer:

> The status lifecycle protects the loan process. Users start with DRAFT applications, submit them, admins verify documents, and then approve or reject. Business rules prevent invalid transitions.

---

## 23. Important Design Decisions

### Why separate Auth Service?

Authentication is a separate concern. Other services should not handle password logic.

### Why API Gateway?

Frontend needs one entry point instead of calling many service URLs.

### Why Eureka?

Services can discover each other dynamically.

### Why RabbitMQ?

Application and admin services need synchronization without tight coupling.

### Why DTOs?

They separate API data from database entities.

### Why JWT?

JWT supports stateless authentication across microservices.

### Why PostgreSQL?

It is reliable and suitable for structured relational data.

### Why Docker?

It makes local and deployment environments consistent.

### Why CI/CD?

It automates build, test, and image delivery.

---

## 24. File Reading Order

If you want to understand the project from zero, follow this order:

### Step 1: Architecture

```text
docker-compose.yml
api-gateway/src/main/resources/application.properties
```

### Step 2: Authentication

```text
auth-service/.../AuthController.java
auth-service/.../AuthService.java
auth-service/.../User.java
auth-service/.../UserRepository.java
auth-service/.../JwtUtil.java
auth-service/.../SecurityConfig.java
```

### Step 3: Loan Application

```text
application-service/.../ApplicationController.java
application-service/.../ApplicationService.java
application-service/.../LoanApplication.java
application-service/.../ApplicationRepository.java
application-service/.../ApplicationStatusUpdateListener.java
```

### Step 4: Documents

```text
document-service/.../DocumentController.java
document-service/.../DocumentService.java
document-service/.../Document.java
document-service/.../DocumentRepository.java
```

### Step 5: Admin

```text
admin-service/.../AdminController.java
admin-service/.../AdminService.java
admin-service/.../Application.java
admin-service/.../ApplicationListener.java
admin-service/.../ApplicationRepository.java
```

### Step 6: Frontend

```text
finflow-frontend/src/app/app.routes.ts
finflow-frontend/src/app/core/services/auth.service.ts
finflow-frontend/src/app/core/interceptors/auth.interceptor.ts
finflow-frontend/src/app/core/guards/auth.guard.ts
finflow-frontend/src/app/core/guards/admin.guard.ts
```

### Step 7: DevOps

```text
Dockerfile files
docker-compose.yml
.github/workflows/ci-cd.yml
sonar-project.properties
```

---

## 25. Common Interview Questions and Answers

### Q1. What is FinFlow?

FinFlow is a microservices-based loan management system where users can apply for loans, upload documents, and track status, while admins can verify documents, approve or reject loans, manage users, and view reports.

### Q2. Why did you use microservices?

We used microservices to separate responsibilities like authentication, loan applications, documents, and admin operations. This improves maintainability, scalability, and independent deployment.

### Q3. What is the role of API Gateway?

API Gateway is the single entry point for the frontend. It validates JWT tokens, checks admin authorization, adds user headers, and routes requests to backend services.

### Q4. How is authentication implemented?

Authentication is implemented using JWT. Auth service validates login credentials and returns a signed token. The frontend stores it and sends it in the Authorization header. Gateway validates it for protected APIs.

### Q5. How do you protect admin APIs?

The JWT contains the user's role. API Gateway checks if the requested path is an admin path. If the role is not ADMIN, it returns 403 Forbidden.

### Q6. What happens when a user creates an application?

Application service saves the application as DRAFT, then publishes an application snapshot to RabbitMQ. Admin service consumes that message and stores a copy for admin operations.

### Q7. Why does admin service have its own Application entity?

Admin service maintains a local copy of applications so admin operations are decoupled from the user-facing application service. It receives updates asynchronously from RabbitMQ.

### Q8. How does status sync happen?

Application service sends application updates to admin service through `application_queue`. Admin service sends decision/status updates back through `application_status_update_queue`.

### Q9. Why use RabbitMQ instead of REST calls?

RabbitMQ makes communication asynchronous and loosely coupled. Application service does not need to directly wait for admin service when syncing application data.

### Q10. How are documents uploaded?

Frontend sends a multipart file to document service. Document service validates that the user owns the application, then stores file metadata and binary data in PostgreSQL.

### Q11. How does document service check ownership?

Document service calls application-service internal owner endpoint using RestTemplate. If the logged-in user's email matches the application owner, access is allowed.

### Q12. What is the status lifecycle?

Applications start as DRAFT, become SUBMITTED after user submission, become DOCS_VERIFIED after admin document verification, and finally become APPROVED or REJECTED.

### Q13. What is DTO and why did you use it?

DTO means Data Transfer Object. We use DTOs to avoid exposing entity classes directly and to define clean request/response structures.

### Q14. What is JPA?

JPA is used for object-relational mapping. It maps Java entities to database tables and allows database operations through repositories.

### Q15. What is the difference between entity and DTO?

Entity represents the database table. DTO represents data exchanged through APIs. Entity is internal, DTO is external.

### Q16. What is `JpaRepository`?

`JpaRepository` provides ready-made CRUD methods like save, findById, findAll, delete, and count.

### Q17. Where is encapsulation used?

Entity classes use private fields and public getters/setters, for example `User`, `LoanApplication`, and `Document`.

### Q18. Where is inheritance used?

JWT filters extend Spring filter classes like `OncePerRequestFilter`. Request wrappers extend `HttpServletRequestWrapper`.

### Q19. Where are interfaces used?

Repository classes are interfaces extending `JpaRepository`, for example `UserRepository` and `ApplicationRepository`.

### Q20. Where are collections used?

`List` is used for collections of users/applications/documents. `Map` is used in exception responses and JWT claims. Streams are used to convert entity lists into DTO lists.

### Q21. What is Docker used for?

Docker packages each service into a container. Docker Compose runs all services and dependencies together.

### Q22. What is CI/CD in your project?

GitHub Actions builds and tests backend services, builds the frontend, uploads artifacts, and pushes Docker images to GitHub Container Registry.

### Q23. What is SonarQube used for?

SonarQube checks code quality, bugs, vulnerabilities, code smells, duplication, and coverage.

### Q24. What is Eureka?

Eureka is a service registry. Services register themselves with Eureka, and gateway discovers them using service names.

### Q25. What is Config Server?

Config Server centralizes configuration so services can load properties from one place instead of duplicating them.

---

## 26. Strong Interview Explanation Script

Use this if the interviewer asks you to explain end to end:

> FinFlow is a loan management system built with Angular and Spring Boot microservices. The user first signs up or logs in through the auth service. Auth service validates credentials, stores users in PostgreSQL, encodes passwords using BCrypt, and returns a JWT token. The Angular frontend stores this token and sends it in every request.
>
> All frontend requests go through API Gateway. The gateway validates the JWT, extracts the user email and role, checks admin authorization when required, and routes the request to the correct microservice using Eureka service discovery.
>
> When a user creates a loan application, application-service stores it in PostgreSQL with DRAFT status and publishes a message to RabbitMQ. Admin-service consumes that message and keeps an admin-side copy of the application.
>
> The user can submit the application, changing its status to SUBMITTED. The user can also upload documents through document-service. Document-service verifies that the user owns the application by calling application-service internally, then stores the document metadata and file data.
>
> Admin users access the admin dashboard. Admin-service allows admins to view applications, verify whether documents exist, add notes, approve or reject applications, and manage users. When admin updates a status, admin-service publishes a status update message to RabbitMQ. Application-service listens to that queue and updates the original user-facing application status.
>
> The whole system is containerized using Docker and Docker Compose. CI/CD is implemented using GitHub Actions, which builds and tests all services, builds the frontend, and publishes Docker images to GitHub Container Registry.

---

## 27. How to Answer If Asked About Your Contribution

You can say:

> I worked on building and understanding the complete FinFlow system, including microservice communication, JWT security, loan application flow, document upload flow, admin decision flow, Docker setup, SonarQube quality checks, and GitHub Actions CI/CD. I also prepared project documentation and end-to-end flow explanations for maintainability and interview readiness.

If they ask about a specific technical contribution:

> I implemented/understood the CI/CD pipeline using GitHub Actions. It runs Maven verification for each Spring Boot service, builds the Angular frontend, uploads build artifacts, and publishes Docker images to GitHub Container Registry.

---

## 28. Quick Revision Cheat Sheet

```text
Frontend -> Angular
Backend -> Spring Boot microservices
Database -> PostgreSQL
Security -> JWT + Spring Security
Gateway -> Spring Cloud Gateway
Discovery -> Eureka
Config -> Config Server
Messaging -> RabbitMQ
ORM -> JPA/Hibernate
Container -> Docker
CI/CD -> GitHub Actions
Quality -> SonarQube
Tracing -> Zipkin
Docs -> Swagger/OpenAPI
```

Core flow:

```text
Signup/Login
  -> JWT
  -> Create loan application
  -> Submit application
  -> Upload documents
  -> Admin verifies documents
  -> Admin approves/rejects
  -> User sees updated status
```

Most important services:

```text
auth-service          -> identity and JWT
application-service   -> loan lifecycle
document-service      -> file upload
admin-service         -> approval/rejection
api-gateway           -> routing and security
service-registry      -> discovery
config-server         -> config
```

Most important queues:

```text
application_queue
application_status_update_queue
```

Most important statuses:

```text
DRAFT
SUBMITTED
DOCS_VERIFIED
APPROVED
REJECTED
```

---

## 29. Final Memory Map

Remember FinFlow like this:

```text
Auth creates identity.
Gateway protects and routes requests.
Application service owns loan applications.
Document service owns uploaded documents.
Admin service owns decisions.
RabbitMQ syncs application and admin status.
PostgreSQL stores service data.
Eureka discovers services.
Config server centralizes properties.
Angular gives the user interface.
Docker runs everything.
GitHub Actions builds and delivers images.
```

If you can explain this map clearly, you can answer most interview questions about this project.

