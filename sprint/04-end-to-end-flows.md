# End To End Flows

## Flow 1: User Signup

```mermaid
sequenceDiagram
  participant U as User
  participant A as Angular Signup
  participant G as API Gateway
  participant Auth as Auth Service
  participant DB as auth_db

  U->>A: Enters signup details
  A->>G: POST /auth/signup
  G->>Auth: Public route, forwarded
  Auth->>DB: Check email exists
  Auth->>DB: Save user with BCrypt password and USER role
  Auth-->>A: JWT token
  A->>A: Store token and local profile
```

Key explanation:

- Signup is public.
- Auth-service validates email, phone, DOB, and age.
- Password is never stored as plain text. It is BCrypt encoded.
- Returned JWT logs user in immediately.

## Flow 2: Login And Role-Based Navigation

```mermaid
sequenceDiagram
  participant U as User/Admin
  participant A as Angular Login
  participant G as API Gateway
  participant Auth as Auth Service

  U->>A: Enters email and password
  A->>G: POST /auth/login
  G->>Auth: Public route
  Auth-->>A: JWT token
  A->>A: Decode token role
  alt role ADMIN
    A->>A: Navigate /admin/applications
  else role USER
    A->>A: Navigate /dashboard
  end
```

Key explanation:

- Angular decodes token for frontend navigation.
- Backend gateway still enforces security. Frontend checks are for user experience, not final security.

## Flow 3: Create Loan Draft

```mermaid
sequenceDiagram
  participant U as User
  participant C as ApplicationCreateComponent
  participant G as API Gateway
  participant App as Application Service
  participant DB as application_db
  participant MQ as RabbitMQ
  participant Admin as Admin Service
  participant ADB as admin_db

  U->>C: Fills loan form
  C->>G: POST /applications with JWT
  G->>G: Validate token
  G->>App: Add X-User-Email
  App->>DB: Save DRAFT application
  App->>MQ: Publish UPSERT snapshot
  MQ->>Admin: application_queue message
  Admin->>ADB: Upsert admin copy
  App-->>C: ApplicationResponse
  C->>C: Show success and navigate detail
```

Key explanation:

- Application starts as `DRAFT`.
- `applicantName` is not trusted from frontend. It is taken from gateway header.
- Admin gets a copy asynchronously through RabbitMQ.

## Flow 4: Upload Supporting Document

```mermaid
sequenceDiagram
  participant U as User
  participant UI as DocumentsUploadComponent
  participant G as API Gateway
  participant Doc as Document Service
  participant App as Application Service
  participant DB as document_db

  U->>UI: Chooses file and document type
  UI->>G: POST /documents/upload multipart
  G->>Doc: Add X-User-Email and X-User-Role
  Doc->>App: GET /internal/applications/{id}/owner
  App-->>Doc: owner email and status
  Doc->>Doc: Check owner matches user
  Doc->>DB: Save file metadata and bytes
  Doc-->>UI: DocumentResponse
```

Key explanation:

- Frontend validates type and size before upload.
- Backend validates ownership before saving.
- Files are stored in PostgreSQL as bytes.

## Flow 5: Submit Application

```mermaid
sequenceDiagram
  participant U as User
  participant UI as Angular
  participant G as API Gateway
  participant App as Application Service
  participant DB as application_db
  participant MQ as RabbitMQ
  participant Admin as Admin Service

  U->>UI: Clicks submit draft
  UI->>G: POST /applications/{id}/submit
  G->>App: X-User-Email
  App->>App: Check owner and DRAFT status
  App->>DB: Set status SUBMITTED
  App->>MQ: Publish UPSERT snapshot
  MQ->>Admin: Sync admin copy
  App-->>UI: Updated application
```

Key explanation:

- Once submitted, the application can no longer be edited or deleted by user.
- Admin can now start review.

## Flow 6: Admin Verifies Documents

```mermaid
sequenceDiagram
  participant AdminUser as Admin
  participant UI as Admin Detail Page
  participant G as API Gateway
  participant Admin as Admin Service
  participant Doc as Document Service
  participant MQ as RabbitMQ
  participant App as Application Service

  AdminUser->>UI: Clicks Mark Documents Verified
  UI->>G: PUT /admin/documents/{applicationId}/verify
  G->>G: Require ADMIN role
  G->>Admin: Forward request
  Admin->>Admin: Require status SUBMITTED
  Admin->>Doc: GET /documents/internal/applications/{id}/exists
  Doc-->>Admin: true/false
  Admin->>Admin: Set DOCS_VERIFIED if documents exist
  Admin->>MQ: Publish status update
  MQ->>App: application_status_update_queue
  App->>App: Update original application status
```

Key explanation:

- The endpoint path uses `documents`, but id is application id.
- Verification only checks if at least one document exists.
- Status sync back to user side happens through RabbitMQ.

## Flow 7: Admin Approves Or Rejects

```mermaid
sequenceDiagram
  participant AdminUser as Admin
  participant UI as Admin UI
  participant G as API Gateway
  participant Admin as Admin Service
  participant MQ as RabbitMQ
  participant App as Application Service

  AdminUser->>UI: Clicks Approve or Reject
  UI->>G: POST /admin/applications/{id}/decision
  G->>Admin: ADMIN-only route
  Admin->>Admin: Validate current status and requested decision
  Admin->>Admin: Save final status
  Admin->>MQ: Publish status update
  MQ->>App: Sync original user application
  App->>App: Save APPROVED or REJECTED
```

Rules:

- Approval requires current status `DOCS_VERIFIED`.
- Rejection can happen from `SUBMITTED` or `DOCS_VERIFIED`.
- A final application cannot be changed again.

## Flow 8: User Checks Status

```mermaid
sequenceDiagram
  participant U as User
  participant UI as Angular
  participant G as API Gateway
  participant App as Application Service

  U->>UI: Opens application detail or list
  UI->>G: GET /applications/{id}
  G->>App: X-User-Email
  App->>App: Check owner
  App-->>UI: Updated status
```

Key explanation:

- The user sees final status after admin-service has published status and application-service listener has processed it.

## Flow 9: Admin User Management

```mermaid
sequenceDiagram
  participant AdminUser as Admin
  participant UI as Admin Users Page
  participant G as API Gateway
  participant Admin as Admin Service
  participant Auth as Auth Service

  AdminUser->>UI: Opens Users page
  UI->>G: GET /admin/users
  G->>Admin: ADMIN-only route
  Admin->>Auth: GET /internal/users
  Auth-->>Admin: UserResponse list
  Admin-->>UI: Users
  AdminUser->>UI: Changes role
  UI->>G: PUT /admin/users/{id}
  Admin->>Auth: PUT /internal/users/{id}/role
  Auth-->>Admin: Updated user
```

Key explanation:

- Auth-service owns users and roles.
- Admin-service acts as a secure admin-facing facade.

## Flow 10: Reports

There are two report styles:

- Backend `/admin/reports` returns a summary string with status counts.
- Frontend admin reports page loads applications and computes charts locally.

Frontend computed values:

- status distribution.
- monthly application counts.
- total approved amount this month.

