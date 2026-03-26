# FinFlow Swagger API Testing Guide

## Purpose

This document explains how to test the FinFlow microservices using Swagger UI.

It covers:

- startup order
- Swagger URLs
- authentication flow
- end-to-end test order
- sample request bodies
- expected results
- important negative test cases

## Services and ports

These are the current service ports from the config repository:

- `service-registry` -> `8761`
- `config-server` -> `8888`
- `auth-service` -> `8081`
- `api-gateway` -> `8083`
- `application-service` -> `8084`
- `document-service` -> `8085`
- `admin-service` -> `8086`

## Recommended startup order

Start services in this order:

1. `service-registry`
2. `config-server`
3. `auth-service`
4. `application-service`
5. `document-service`
6. `admin-service`
7. `api-gateway`

## Swagger URLs

### Best option: use the gateway Swagger UI

Open:

- [Gateway Swagger UI](http://localhost:8083/swagger-ui.html)

This is the easiest option because it aggregates the docs for:

- auth-service
- application-service
- document-service
- admin-service

### Direct service Swagger URLs

If you want to test each service separately:

- [Auth Swagger](http://localhost:8081/swagger-ui.html)
- [Application Swagger](http://localhost:8084/swagger-ui.html)
- [Document Swagger](http://localhost:8085/swagger-ui.html)
- [Admin Swagger](http://localhost:8086/swagger-ui.html)

## Important auth behavior

### Public endpoints

These can be called without a token:

- `POST /auth/signup`
- `POST /auth/login`
- `/test` endpoints
- Swagger endpoints

### Protected endpoints

All application, document, and admin business APIs require a JWT token.

### Roles

- normal signup users get role `USER`
- admin endpoints require role `ADMIN`

### Default admin account

The current config bootstraps a default admin:

- email: `admin@finflow.com`
- password: `Admin@123`

## End-to-end test flow

Use this order for testing:

1. create/login user
2. create loan application
3. submit loan application
4. upload document for that application
5. login as admin
6. verify the document
7. approve or reject the application
8. re-check application status as the user

## Step 1: Sign up a normal user

Service:

- `auth-service`

Endpoint:

- `POST /auth/signup`

Sample body:

```json
{
  "email": "user1@finflow.com",
  "password": "User@123"
}
```

Expected result:

- response like `User registered`

If the user already exists:

- expected error like `Email already registered!`

## Step 2: Login as the normal user

Endpoint:

- `POST /auth/login`

Sample body:

```json
{
  "email": "user1@finflow.com",
  "password": "User@123"
}
```

Expected result:

- JWT token string in the response body

Copy this token.

## Step 3: Authorize Swagger

In Swagger UI:

1. click `Authorize`
2. paste:

```text
Bearer <your-jwt-token>
```

3. click `Authorize`
4. close the dialog

## Step 4: Create a loan application

Service:

- `application-service`

Endpoint:

- `POST /applications`

Sample body:

```json
{
  "name": "ignored-by-service",
  "amount": 50000
}
```

Important note:

- `name` is not trusted
- the service uses the logged-in user email as the applicant

Expected result:

- application object is returned
- `status` should be `DRAFT`
- `applicantName` should match the logged-in user email

Save the returned `id`.

## Step 5: View your applications

Endpoint:

- `GET /applications`

Expected result:

- only the logged-in user's applications are shown

Also test:

- `GET /applications/my`

Expected result:

- same idea, only your applications

## Step 6: Submit the application

Endpoint:

- `POST /applications/{id}/submit`

Use the application id from Step 4.

Expected result:

- returned status becomes `SUBMITTED`
- admin-service should receive the synced application through RabbitMQ

## Step 7: Check user-side status

Endpoint:

- `GET /applications/{id}/status`

Expected result:

- `SUBMITTED`

## Step 8: Upload a document

Service:

- `document-service`

Endpoint:

- `POST /documents/upload`

Request type:

- `multipart/form-data`

Parameters:

- `file` -> choose any PDF, image, or test file
- `applicationId` -> use the application id from Step 4

Expected result:

- response contains document metadata
- returned `applicationId` should match your loan application

Important logic now enforced:

- a user can upload only for their own application
- admin can access document APIs, but normal users cannot upload for another user's application

Save the returned document `id`.

## Step 9: Download the uploaded document

Endpoint:

- `GET /documents/{id}`

Expected result:

- the file content is returned
- the browser may download the file

Important logic now enforced:

- the owner can fetch the document
- another normal user should not be able to fetch it

## Step 10: Login as admin

Use:

- `POST /auth/login`

Sample body:

```json
{
  "email": "admin@finflow.com",
  "password": "Admin@123"
}
```

Expected result:

- admin JWT token

Replace the Swagger token with the admin token using `Authorize`.

## Step 11: View submitted applications as admin

Service:

- `admin-service`

Endpoints:

- `GET /admin/applications`
- `GET /admin/applications/{id}`

Expected result:

- the application submitted by the user should be visible

## Step 12: Verify document as admin

Endpoint:

- `PUT /admin/documents/{id}/verify`

Important note:

- in the current code, this `{id}` is the application id, not the document id
- use the application id from Step 4

Expected result:

- message:

```text
Document verified. Application status updated to DOCS_VERIFIED.
```

What this endpoint now does:

- checks the application status is `SUBMITTED`
- calls document-service to verify that at least one document exists for the application
- only then updates the status to `DOCS_VERIFIED`

## Step 13: Approve the application as admin

Endpoint:

- `POST /admin/applications/{id}/decision`

Sample body:

```json
{
  "status": "APPROVED",
  "comments": "Documents verified and approved"
}
```

Expected result:

- returned application status becomes `APPROVED`

Important logic now enforced:

- approval is allowed only after `DOCS_VERIFIED`

## Step 14: Re-check status as the normal user

Login again as the normal user if needed and re-authorize Swagger with the user token.

Endpoints:

- `GET /applications/{id}`
- `GET /applications/{id}/status`

Expected result:

- status should now be `APPROVED`

## Alternative admin test: reject application

Instead of approving, you can reject:

Endpoint:

- `POST /admin/applications/{id}/decision`

Sample body:

```json
{
  "status": "REJECTED",
  "comments": "Rejected by admin"
}
```

Expected result:

- status becomes `REJECTED`

## Reports test

Endpoint:

- `GET /admin/reports`

Expected result:

- summary string showing totals by status

Example:

```text
Total Applications: 1, Submitted: 0, Approved: 1, Rejected: 0, Docs Verified: 0
```

## User management test

Endpoint:

- `GET /admin/users`

Expected result:

- returns distinct applicant names seen in submitted applications

Endpoint:

- `PUT /admin/users/{id}`

Sample body:

```json
{
  "role": "ADMIN"
}
```

Expected result:

- informational string only
- this does not actually change auth-service users yet

## Negative test cases

These are very important to test.

### 1. Create application without token

Call:

- `POST /applications`

Expected result:

- `401 Unauthorized`

### 2. Use one user to read another user's application

Test flow:

1. create application with `user1`
2. login as `user2`
3. call `GET /applications/{user1-application-id}`

Expected result:

- error because ownership is enforced

### 3. Use one user to delete another user's application

Call:

- `DELETE /applications/{other-users-id}`

Expected result:

- unauthorized failure

### 4. Upload document for another user's application

Login as `user2` and upload using `user1` application id.

Expected result:

- unauthorized failure

### 5. Download another user's document

Login as `user2` and call:

- `GET /documents/{user1-document-id}`

Expected result:

- unauthorized failure

### 6. Verify document before upload

As admin, call:

- `PUT /admin/documents/{applicationId}/verify`

without uploading any document first.

Expected result:

- failure like:

```text
No documents uploaded for application <id>
```

### 7. Approve before document verification

As admin, directly call:

- `POST /admin/applications/{id}/decision`

with:

```json
{
  "status": "APPROVED",
  "comments": "trying early approval"
}
```

Expected result:

- failure like:

```text
Application can be approved only after documents are verified
```

### 8. Non-admin token on admin APIs

Login as normal user and call:

- `GET /admin/applications`
- `PUT /admin/documents/{id}/verify`
- `POST /admin/applications/{id}/decision`

Expected result:

- `403 Forbidden`

## Fast smoke test sequence

If you want the shortest useful flow, test in this exact order:

1. `POST /auth/signup`
2. `POST /auth/login`
3. `POST /applications`
4. `POST /applications/{id}/submit`
5. `POST /documents/upload`
6. login as admin
7. `PUT /admin/documents/{applicationId}/verify`
8. `POST /admin/applications/{id}/decision` with `APPROVED`
9. login as normal user
10. `GET /applications/{id}/status`

## Troubleshooting

### Swagger opens but APIs fail with 401

Check:

- you clicked `Authorize`
- token is pasted as `Bearer <token>`
- token is not expired

### Admin APIs fail with 403

Check:

- you are using the admin token
- the admin login is `admin@finflow.com`
- the role inside the JWT is `ADMIN`

### Config looks wrong

Check:

- `config-server` is running on `8888`
- service logs contain `Fetching config from server at : http://localhost:8888`

### Admin verify fails even after upload

Check:

- you used the application id, not the document id
- the document upload succeeded for that same application id
- `document-service` is running and registered in Eureka

### A service does not appear in gateway Swagger

Check:

- the service is running
- the service is registered in Eureka
- gateway is running on `8083`

## Notes for current codebase

- `/admin/documents/{id}/verify` currently uses application id
- `/internal/...` endpoints are for service-to-service use and should not be part of manual Swagger business testing
- `api-gateway` is the best place to test real user flow because it forwards auth context headers automatically

## Recommended testing location

Use gateway Swagger first:

- [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)

That gives the most realistic end-to-end behavior because:

- JWT is checked at gateway
- user email and role headers are forwarded downstream
- admin path restrictions are applied consistently
