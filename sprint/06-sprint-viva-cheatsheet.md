# Sprint Viva Cheatsheet

## 30-Second Introduction

FinFlow is a microservice-based loan application system. Users can register, log in, create loan drafts, upload documents, submit applications, and track status. Admins can view synced applications, verify documents, approve or reject applications, manage users, and view reports. The Angular frontend talks to a Spring Cloud API Gateway, which validates JWTs and routes requests to auth, application, document, and admin services.

## Best Demo Flow

1. Open frontend.
2. Signup or login as user.
3. Create a loan draft.
4. Upload supporting document.
5. Submit application.
6. Login as admin using `admin@finflow.com` and `Admin@123`.
7. Open admin applications.
8. Verify documents.
9. Approve or reject.
10. Login as user and show updated status.

## Main Angular Points To Explain

- The app uses Angular standalone components, not traditional NgModules.
- `main.ts` bootstraps `AppComponent` with `appConfig`.
- `app.config.ts` registers router, HTTP interceptors, and API base URL.
- `app.routes.ts` defines lazy-loaded routes.
- `authGuard` protects user routes.
- `adminGuard` protects admin routes.
- `authInterceptor` adds JWT token to every backend request.
- `errorInterceptor` handles common errors globally with toast messages.
- Services wrap HTTP APIs so components stay cleaner.
- Signals store reactive state like loading, applications, auth session, and notifications.
- Computed values derive summaries, filters, EMI, eligibility, role checks, and report data.
- Reactive forms are used for login, signup, application creation, application editing, and profile/password forms.

## Main Backend Points To Explain

- API Gateway is the single entry point.
- Auth-service owns users and JWT generation.
- Application-service owns user applications.
- Document-service owns uploaded files and checks application ownership.
- Admin-service owns review workflow and stores synced admin copies.
- Eureka lets services find each other by name.
- Config Server centralizes configuration from `config-repo`.
- RabbitMQ syncs applications and status updates between application-service and admin-service.
- PostgreSQL stores each service's own data.
- Swagger is aggregated through gateway.

## Common Questions And Answers

### Why microservices?

The project separates responsibilities. Auth, applications, documents, admin review, gateway routing, config, and discovery are independent. This makes the system easier to scale and reason about than one large backend.

### Why API Gateway?

It gives the frontend one backend URL, validates JWTs, protects admin routes, injects user headers, and routes requests to the correct microservice.

### How is authentication done?

Auth-service creates JWT after signup/login. Angular stores the JWT in localStorage. The auth interceptor sends it in the Authorization header. Gateway validates it and passes user email/role headers to services.

### How does Angular know user is admin?

`AuthService` decodes the JWT payload and reads the `role` claim. `adminGuard` and navbar use that role. Backend gateway/admin-service still enforce actual security.

### How is a loan application created?

Angular create page sends `POST /applications`. Gateway adds user email from token. Application-service creates a `DRAFT` application for that email and publishes a RabbitMQ snapshot to admin-service.

### Why is `applicantName` an email?

The backend stores the authenticated user's email as the applicant identity. It comes from gateway header, not from user input.

### How does document upload prevent unauthorized access?

Document-service asks application-service for the application owner. If the requester is not admin and their email does not match the owner, it throws `Unauthorized`.

### How does admin see applications?

Application-service publishes every create/update/submit/delete event to RabbitMQ. Admin-service consumes t hose messages and maintainsits own `admin_db` copy.

### How does status update reach user side?

Admin-service publishes a status update to `application_status_update_queue`. Application-service listens and updates the original application.

### Why approve only after document verification?

It enforces a realistic loan review workflow: submitted application first needs supporting documents checked, then final decision.

### What is Config Server doing?

It reads service properties from `config-repo` and provides centralized configuration to services.

### What is Eureka doing?

Services register with Eureka so gateway and load-balanced RestTemplate can call logical names like `APPLICATION-SERVICE`.

### What is Zipkin doing?

Zipkin receives tracing spans so service-to-service calls can be observed in a distributed system.

## Important Endpoints

Auth:

- `POST /auth/signup`
- `POST /auth/login`
- `GET /auth/profile`
- `PUT /auth/profile`
- `PUT /auth/password`

Applications:

- `POST /applications`
- `GET /applications/my`
- `GET /applications/{id}`
- `PUT /applications/{id}`
- `DELETE /applications/{id}`
- `POST /applications/{id}/submit`
- `GET /applications/{id}/status`

Documents:

- `POST /documents/upload`
- `GET /documents/{id}`
- `GET /documents/applications/{applicationId}`

Admin:

- `GET /admin/applications`
- `GET /admin/applications/{id}`
- `PUT /admin/documents/{applicationId}/verify`
- `POST /admin/applications/{id}/decision`
- `POST /admin/applications/bulk-decision`
- `PUT /admin/applications/{id}/notes`
- `GET /admin/users`
- `PUT /admin/users/{id}`
- `GET /admin/reports`

## Status Rules

- `DRAFT`: created but not submitted. User can edit/delete/submit.
- `SUBMITTED`: submitted for admin review. User cannot edit/delete.
- `DOCS_VERIFIED`: admin verified documents. Ready for decision.
- `APPROVED`: final positive decision.
- `REJECTED`: final negative decision.

## Angular Terms You Should Be Able To Say

- Standalone component: component declares its own imports and does not require NgModule.
- Signal: reactive state container.
- Computed: derived reactive value based on signals.
- Reactive form: form model defined in TypeScript using `FormBuilder`, validators, and controls.
- Guard: route protection function.
- Interceptor: HTTP middleware that can modify requests or handle responses globally.
- Lazy loading: route loads component/code only when needed.
- Injection token: custom dependency key used to inject API base URL.

## Backend Terms You Should Be Able To Say

- Controller: exposes REST endpoints.
- Service: contains business logic.
- Repository: database access layer.
- Entity: JPA table mapping.
- DTO: request/response object used between layers/API.
- JWT: signed token carrying email and role.
- RabbitMQ queue: async message channel.
- Eureka: service registry.
- Config Server: centralized config provider.
- RestTemplate with `@LoadBalanced`: service-to-service HTTP client using service names.

## Strong Points To Highlight

- Role-based navigation and backend role enforcement.
- Ownership checks for applications and documents.
- RabbitMQ event sync between user and admin sides.
- Clean Angular separation: components for UI, services for API, models for types.
- Reusable components: navbar, status badge, loader, confirm dialog, toast outlet.
- Docker Compose can run the whole ecosystem.
- Swagger guide gives a repeatable API test flow.
- Unit tests cover critical service rules.

## Honest Improvement Points

- Add persisted backend notifications instead of localStorage-only notifications.
- Store documents in object storage instead of database blobs.
- Add `submittedAt` to backend entity because frontend model expects it.
- Rename `/admin/documents/{id}/verify` or clarify that id means application id.
- Add frontend automated tests.
- Improve encoding artifacts in frontend templates.
- Add stronger file validation on backend, not only frontend.

## If Asked To Explain One File Quickly

- `app.config.ts`: Angular providers and API URL.
- `app.routes.ts`: all main routes and guards.
- `auth.service.ts`: frontend session/JWT/profile logic.
- `application.service.ts`: frontend application API wrapper.
- `JwtFilter.java`: gateway security and user header injection.
- `AuthService.java`: signup/login/profile/password/role business logic.
- `ApplicationService.java`: draft/submit/update/delete and RabbitMQ publish logic.
- `DocumentService.java`: file save/download with ownership check.
- `AdminService.java`: admin decisions, document verification, reports, user proxy calls.
- `ApplicationListener.java`: admin-service listens to application sync messages.
- `ApplicationStatusUpdateListener.java`: application-service listens to admin status updates.

