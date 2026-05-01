# Final Important Points For Sprint Evaluation

Read this file before evaluation. It is a quick checklist of what to say, what to show, and what not to get confused about.

## Project One-Line Answer

FinFlow is a microservice-based loan application system where users create and track loan applications, upload documents, and admins verify documents, approve/reject applications, manage users, and view reports.

## URLs To Remember

- Frontend: `http://localhost:4200`
- API Gateway Docker port: `http://localhost:9083`
- Eureka dashboard: `http://localhost:9761`
- RabbitMQ UI: `http://localhost:15672`
- Zipkin: `http://localhost:9412`
- SonarQube: `http://localhost:9000`

## Credentials To Remember

Application admin:

```text
email: admin@finflow.com
password: Admin@123
```

SonarQube first login:

```text
username: admin
password: admin
```

After first login, use the new password you changed to.

RabbitMQ default:

```text
username: guest
password: guest
```

## Best Demo Order

1. Show Docker containers are running with `docker ps`.
2. Open frontend `http://localhost:4200`.
3. Login/signup as normal user.
4. Create loan application draft.
5. Upload document.
6. Submit application.
7. Login as admin.
8. Open admin applications.
9. Verify document.
10. Add admin note and save.
11. Approve or reject application.
12. Login as user again.
13. Show status and admin note on application detail.
14. Show SonarQube projects passed.

## Most Important Architecture Points

- Frontend calls API Gateway, not individual services.
- Gateway validates JWT and injects user email/role headers.
- Auth-service owns users and JWT.
- Application-service owns user loan applications.
- Document-service owns uploaded documents.
- Admin-service owns review workflow.
- RabbitMQ syncs application data and status/note updates.
- PostgreSQL stores service data.
- Eureka handles service discovery.
- Config Server centralizes service configuration.
- SonarQube checks code quality.

## Status Flow

```text
DRAFT -> SUBMITTED -> DOCS_VERIFIED -> APPROVED
                              |
                              -> REJECTED
```

Explain:

- `DRAFT`: user can edit/delete.
- `SUBMITTED`: waiting for admin review.
- `DOCS_VERIFIED`: documents checked by admin.
- `APPROVED`: final approved.
- `REJECTED`: final rejected.

## Admin Note Flow

This is important because it was recently fixed.

Old issue:

- Admin note was saved in admin-service only.
- User detail page expected `adminNotes`, but application-service did not have the field.
- So user could not see admin note.

Current fix:

- Admin-service saves note.
- Admin-service publishes note update to RabbitMQ.
- Application-service listener consumes message.
- Application-service stores `adminNotes`.
- User API returns `adminNotes`.
- Angular user detail page displays Admin Note.

Say this if asked:

`Admin notes are synced asynchronously from admin-service to application-service through RabbitMQ, same concept as status update sync.`

## SonarQube Points

Your current SonarQube shows 7 projects:

- FinFlow Admin Service
- FinFlow API Gateway
- FinFlow Application Service
- FinFlow Auth Service
- FinFlow Config Server
- FinFlow Document Service
- FinFlow Service Registry

All are Quality Gate Passed.

Metrics meaning:

- Bugs: likely functional defects.
- Vulnerabilities: security issues.
- Security Hotspots: security-sensitive code needing review.
- Code Smells: maintainability problems.
- Coverage: percentage covered by tests.
- Duplications: repeated code.
- Lines: scanned code size.

Warnings:

- `No longer active version`: local SonarQube version old; okay for demo, upgrade for production.
- `Embedded database`: okay for local evaluation; production should use external DB.

## Docker Commands

Check running:

```powershell
docker ps
```

Start all:

```powershell
docker compose up -d
```

Rebuild changed services only:

```powershell
docker compose up -d --build admin-service application-service
```

Stop all:

```powershell
docker compose down
```

Check logs:

```powershell
docker logs finflow-application-service
docker logs finflow-admin-service
```

## If Something Fails During Demo

### Frontend not loading

Check:

```powershell
docker ps
```

Look for `finflow-frontend` on port `4200`.

### Login fails

Check auth-service and API gateway are running. Also confirm credentials.

### Admin cannot see application

Check RabbitMQ is running. Application must be submitted, not only draft.

### User cannot see updated status

Admin decision update is async. Refresh user page. Check application-service and RabbitMQ.

### User cannot see admin note

Save note again after latest fix. Then refresh user detail page. Old notes before fix may not sync automatically.

### SonarQube not opening

Check:

```powershell
docker ps
```

Look for `finflow-sonarqube` on port `9000`.

## High-Value Answers

### Why microservices?

Different business responsibilities are separated. Each service can be developed, tested, deployed, and scaled independently.

### Why API Gateway?

Single frontend entry point, JWT validation, role checks, routing, and user header injection.

### Why RabbitMQ?

Asynchronous communication between services, loose coupling, and eventual consistency.

### Why per-service database?

Each microservice owns its data. This avoids tight coupling and follows microservice architecture.

### Why Swagger?

To document and test APIs quickly during development and evaluation.

### Why SonarQube?

To prove code quality using measurable reports: bugs, vulnerabilities, smells, coverage, duplications, and quality gate.

## Files Evaluator May Ask

- `finflow-frontend/src/app/app.routes.ts`: route configuration and guards.
- `finflow-frontend/src/app/core/interceptors/auth.interceptor.ts`: adds JWT to requests.
- `finflow-frontend/src/app/core/services/auth.service.ts`: frontend auth/session logic.
- `api-gateway/.../JwtFilter.java`: validates JWT and adds user headers.
- `auth-service/.../AuthService.java`: signup/login/profile/password logic.
- `application-service/.../ApplicationService.java`: application create/update/submit logic.
- `document-service/.../DocumentService.java`: document upload/download and ownership check.
- `admin-service/.../AdminService.java`: admin verification, decision, notes, reports.
- `admin-service/.../ApplicationListener.java`: consumes app snapshots.
- `application-service/.../ApplicationStatusUpdateListener.java`: consumes admin status/note updates.
- `docker-compose.yml`: complete infrastructure setup.
- `sonar-project.properties`: SonarQube aggregate scan config.

## Common Mistakes To Avoid

- Do not say frontend directly calls all services. It calls gateway.
- Do not say admin and user share same application table. They are separate service databases/copies.
- Do not say RabbitMQ is database. It is a message broker.
- Do not say code smell is always bug. It is maintainability issue.
- Do not say `admin/admin` works after SonarQube password change. Use new password.
- Do not stop all Docker for small backend code changes. Rebuild only changed services.

## 2-Minute Final Explanation

FinFlow has Angular frontend and Spring Boot microservices. User logs in through auth-service and gets JWT. Frontend sends JWT to API Gateway. Gateway validates token and routes calls. User creates loan applications in application-service and uploads documents through document-service. When application is submitted, application-service publishes message to RabbitMQ, and admin-service receives it for admin review. Admin verifies documents, adds notes, approves or rejects. Admin-service publishes status and note updates back through RabbitMQ. Application-service consumes those updates, so user can see latest status and admin note. Docker Compose runs the full system, and SonarQube verifies code quality for all backend services.
