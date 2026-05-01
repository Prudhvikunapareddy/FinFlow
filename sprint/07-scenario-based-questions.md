# Scenario Based Sprint Evaluation Questions

Use this file for last-minute practice. Answers are written in simple interview style, so you can speak them directly.

## How To Start Answering

If evaluator asks any scenario, follow this pattern:

1. First explain user action.
2. Then explain frontend component/service.
3. Then explain API Gateway/JWT.
4. Then explain backend service/database/message queue.
5. Finally explain result shown in UI.

Example sentence:

`User action frontend nundi API Gateway ki vellutundi, gateway JWT validate chesi correct service ki route chestundi, service database/message queue update chestundi, response frontend lo display avuthundi.`

## User And Auth Scenarios

### 1. User login chesthe internally em jaruguthundi?

User email/password enter chestaru. Angular login component `AuthService` ni call chestundi. Request API Gateway through `auth-service` ki vellutundi. Auth-service password validate chesi JWT token generate chestundi. Frontend token ni `localStorage` lo store chestundi. Taruvatha `authInterceptor` every request ki `Authorization: Bearer token` add chestundi.

### 2. User invalid password enter chesthe?

Auth-service credentials validate cheyyadu. Error response vasthundi. Angular `errorInterceptor` common error handling chesi toast message chupisthundi. Login success avvadu and token store avvadu.

### 3. Admin route user open cheyyadaniki try chesthe?

Frontend lo `adminGuard` user role check chestundi. Role admin kakapothe route block chestundi. Backend side lo kuda API Gateway/admin endpoints role-based protection untundi. So frontend bypass chesina backend secure ga untundi.

### 4. JWT token expire or invalid ayithe?

Gateway JWT validate cheyyadu and request reject avuthundi. Frontend error interceptor error catch chesi user ki message chupisthundi. User malli login cheyyali.

## Loan Application Scenarios

### 5. User new loan application create chesthe status enduku DRAFT?

First user details save chestadu kani review ki submit cheyyaledu. So application-service status ni `DRAFT` ga set chestundi. Draft lo user edit/delete cheyyachu.

### 6. User submitted application edit cheyyadaniki try chesthe?

Application-service lo business rule undi: only `DRAFT` applications can be updated/deleted. Status `SUBMITTED` ayithe RuntimeException throw chestundi. This protects submitted records from changes during review.

### 7. User application submit chesthe admin side lo ela kanipisthundi?

Application-service status ni `SUBMITTED` ga update chestundi and RabbitMQ lo application snapshot publish chestundi. Admin-service `ApplicationListener` queue nundi message consume chesi admin database lo application copy create/update chestundi. So admin dashboard lo application kanipisthundi.

### 8. Why do we use RabbitMQ instead of direct database sharing?

Microservices own databases principle follow chestunnam. Application-service and admin-service direct same table share cheyyavu. RabbitMQ async communication use chesthe services loosely coupled ga untayi. One service temporary slow ayina message queue lo wait chestundi.

### 9. Application-service down unte admin update immediate ga user side lo kanipisthunda?

No, immediate ga kanipinchakapovachu. Admin-service message RabbitMQ queue ki publish chestundi. Application-service malli up ayyaka listener consume chesi status/note update chestundi. This is async eventual consistency.

## Document Scenarios

### 10. User document upload chesthe unauthorized access ela prevent chestaru?

Document-service application owner ni application-service nundi check chestundi. Logged-in user email application owner email tho match ayithe upload/download allow chestundi. Admin role ki review kosam access allow chestundi.

### 11. Admin documents verify cheyyakunda approve chesthe?

Admin-service approve validation chestundi. Application status `DOCS_VERIFIED` kakapothe approve reject chestundi with message: application can be approved only after documents are verified.

### 12. User documents upload cheyyaledu, admin verify click chesthe?

Admin-service document-service internal endpoint call chestundi to check documents exist. Documents lekapothe error: no documents uploaded for application id.

## Admin Scenarios

### 13. Admin approve/reject chesthe user status ela update avuthundi?

Admin-service application status update chestundi. Taruvatha RabbitMQ `application_status_update_queue` ki status message publish chestundi. Application-service listener message consume chesi original user application status update chestundi. User detail/list page refresh chesthe updated status kanipisthundi.

### 14. Admin note save chesthe user ki ela kanipisthundi?

Admin notes admin-service lo save avuthayi. Recent fix prakaram admin-service note update message queue ki publish chestundi. Application-service listener `adminNotes` store chestundi and user application response lo return chestundi. User application detail page lo `Admin Note` section display avuthundi.

### 15. Admin note user ki kanipinchakapothe where to check?

Check these:

1. Admin note save click chesara.
2. `admin-service` and `application-service` containers latest build lo running aa.
3. RabbitMQ running aa.
4. User same application detail page open chesara.
5. Browser hard refresh `Ctrl + F5`.
6. Existing old notes ayithe once again save cheyyali, because old notes before fix sync avvaledu.

### 16. Bulk decision use enti?

Admin multiple applications select chesi same status action apply cheyyachu. It saves time in review workflow. Service internally each id ki decision rules apply chestundi.

## Frontend Scenarios

### 17. Frontend direct microservices ni call cheyyada?

No. Frontend API Gateway ni call chestundi. Gateway single entry point. It validates JWT and routes to correct service. Docker lo frontend API base usually `http://localhost:9083`.

### 18. Why Angular services are used?

Components UI and user interaction handle chestayi. API calls services lo untayi. This makes code cleaner, reusable, and easier to test.

### 19. Why Reactive Forms?

Reactive Forms validations and form state TypeScript lo manage cheyyadaniki useful. Login, signup, create application, profile forms all validation clean ga untayi.

### 20. Why signals?

Signals reactive state management kosam. Loading, user session, applications, filters, notifications lanti state changes UI lo automatically reflect avuthayi.

## Docker And Infrastructure Scenarios

### 21. Docker Compose use enti?

Complete ecosystem one command tho run cheyyadaniki. It starts PostgreSQL, RabbitMQ, Zipkin, Eureka, Config Server, all microservices, frontend, and SonarQube.

### 22. Containers running aa check ela?

Command:

```powershell
docker ps
```

Important containers:

- `finflow-frontend`
- `finflow-api-gateway`
- `finflow-auth-service`
- `finflow-application-service`
- `finflow-admin-service`
- `finflow-document-service`
- `finflow-postgres`
- `finflow-rabbitmq`
- `finflow-sonarqube`

### 23. Service code change chesthe em cheyyali?

Only changed service rebuild/restart chalu. Example:

```powershell
docker compose up -d --build admin-service application-service
```

Full Docker stop required kaadu unless infrastructure issue.

### 24. Config Server use enti?

All service properties `config-repo` lo centralized ga maintain chestam. Services start ayyaka config-server nundi properties load chestayi. This avoids duplicating config inside every service.

### 25. Eureka use enti?

Service discovery. Services logical names tho communicate chestayi, example `APPLICATION-SERVICE`, `DOCUMENT-SERVICE`. Hardcoded host/port dependency reduce avuthundi.

## SonarQube Scenarios

### 26. SonarQube use enti?

SonarQube code quality scanner. It checks bugs, vulnerabilities, security hotspots, code smells, coverage, duplications, and quality gate.

### 27. Screenshot lo Quality Gate Passed ante enti?

Project defined quality rules pass ayyayi. Critical bugs/vulnerabilities/coverage/duplication thresholds satisfy ayyayi.

### 28. Coverage meaning enti?

Tests code lo entha part execute chestunnayo percentage. Higher coverage means more code tested. But only coverage enough kaadu; meaningful assertions also important.

### 29. Code smell ante bug aa?

Code smell direct bug kaadu. But maintainability problem. Example duplicate code, complex method, bad naming, unnecessary logic. Future bugs chances increase avuthayi.

### 30. Vulnerability and Security Hotspot difference?

Vulnerability confirmed security risk. Security hotspot means review required area. Hotspot must be checked manually to decide safe or unsafe.

### 31. SonarQube local warnings explain cheyyali ante?

Current local SonarQube uses embedded database and old version warning. For local demo/evaluation okay. For production, active SonarQube version and external database like PostgreSQL should be used.

## Testing Scenarios

### 32. What tests did you run?

Admin service tests and application service tests. Specifically business rules around admin decisions, notes sync, and application listener are tested.

Speak like this:

`I ran targeted Maven tests in Docker Maven container because local mvn was not installed. AdminServiceTest and ApplicationStatusUpdateListenerTest passed.`

### 33. Why unit tests important?

They verify business rules without starting full system. Example: only draft can be edited, approve only after document verification, admin notes sync message published.

## Tough Questions

### 34. Why admin-service keeps copy of applications?

Admin-service needs review-specific data and independent admin dashboard. Instead of querying application-service every time, it consumes snapshots through RabbitMQ and maintains its own read model.

### 35. Is duplicate data bad?

It is acceptable here because it is a read model for admin workflow. Source of truth for user application is application-service. Admin copy is synced asynchronously.

### 36. What is eventual consistency?

When admin updates status, user side may update after message is consumed. It is not instant shared database consistency, but queue-based reliable sync.

### 37. What improvement would you suggest?

- Persist notifications in backend.
- Add frontend automated tests.
- Store files in object storage.
- Add stronger file validation backend side.
- Add audit logs for admin actions.
- Use production-ready SonarQube DB.
- Add retries/dead-letter queues for RabbitMQ.

## Best Closing Answer

`FinFlow demonstrates real microservice concepts: API Gateway, JWT security, service discovery, centralized config, per-service database ownership, RabbitMQ async sync, Docker deployment, Angular role-based UI, and SonarQube quality checks. The main business flow from user application to admin review and back to user status is fully connected.`
