# Frontend Angular Detailed Guide

## Frontend Big Picture

The Angular app in `finflow-frontend` is a standalone-component Angular 21 application. It does not use traditional Angular modules like `AppModule`. Instead, `src/main.ts` bootstraps `AppComponent` directly with `appConfig`.

The frontend has these major areas:

- Auth pages: login and signup.
- User pages: dashboard, applications list, create application, application detail, profile.
- Document upload component: reusable upload UI inside application detail.
- Admin pages: admin shell, applications, application detail, users, reports.
- Shared UI: navbar, loader, status badge, toast outlet, confirm dialog.
- Core: guards, interceptors, services, models, and injection token.

## Angular Bootstrapping

### `src/main.ts`

This file starts Angular:

```ts
bootstrapApplication(AppComponent, appConfig).catch((error) => console.error(error));
```

Meaning:

- `AppComponent` is the root component.
- `appConfig` provides router, HTTP client, interceptors, global error listeners, zone change detection, and API base URL.

### `src/app/app.config.ts`

This file configures the application-level providers:

- `provideRouter(routes, ...)`: enables Angular Router.
- `withComponentInputBinding()`: allows route params/query params to bind to component inputs if used.
- `withInMemoryScrolling({ scrollPositionRestoration: 'top' })`: scrolls to top on route navigation.
- `withViewTransitions()`: enables browser view transitions for smoother route changes.
- `provideHttpClient(withInterceptors([authInterceptor, errorInterceptor]))`: configures HTTP and registers interceptors.
- `API_BASE_URL`: custom injection token for backend base URL.

Runtime config logic:

- Reads `globalThis.__FINFLOW_CONFIG__?.apiBaseUrl`.
- If missing, falls back to `http://localhost:8083`.

This allows the same Angular build to work in local and Docker environments.

## Root Component

### `src/app/app.component.ts/html/css`

`AppComponent` is the layout shell. It listens to router navigation and decides whether navbar should appear.

Important logic:

- Navbar is hidden on `/login` and `/signup`.
- Navbar is shown on dashboard, applications, profile, and admin routes.
- The template always includes:
  - `<router-outlet />` for routed pages.
  - `<app-toast-outlet />` for global toast messages.

This means every page can trigger toast messages through `ToastService`.

## Routing

### `src/app/app.routes.ts`

Top-level routes:

- `/` redirects to `/login`.
- `/login` lazy-loads `LoginComponent`.
- `/signup` lazy-loads `SignupComponent`.
- `/dashboard` requires `authGuard`.
- `/applications` requires `authGuard` and lazy-loads application child routes.
- `/profile` requires `authGuard`.
- `/admin` requires `adminGuard` and lazy-loads admin child routes.
- unknown routes redirect to login.

Angular lazy loading is used with `loadComponent` and `loadChildren`, which reduces initial bundle size and keeps features separated.

### `src/app/features/applications/applications.routes.ts`

Application routes:

- `/applications`: list page.
- `/applications/new`: create page.
- `/applications/:id`: detail page.

### `src/app/features/admin/admin.routes.ts`

Admin routes:

- `/admin`: loads `AdminShellComponent`.
- `/admin/applications`: admin applications list.
- `/admin/applications/:id`: admin application detail.
- `/admin/users`: admin user management.
- `/admin/reports`: reports page.

The admin shell contains a sidebar and nested router outlet.

## Guards

### `auth.guard.ts`

The auth guard checks `AuthService.isLoggedIn()`.

- If logged in, it returns `true`.
- If not logged in, it redirects to `/login`.

This protects user pages.

### `admin.guard.ts`

The admin guard checks two things:

- User must be logged in.
- User role from token must be `ADMIN`.

If user is not logged in, redirect to login. If logged in but not admin, show a toast and redirect to dashboard.

## Interceptors

### `auth.interceptor.ts`

Every outgoing HTTP request goes through this interceptor.

- Reads JWT token from `AuthService`.
- If token exists, clones request and adds:

```http
Authorization: Bearer <token>
```

The frontend does not manually add this header in each service. Centralizing it keeps API calls cleaner.

### `error.interceptor.ts`

This handles common HTTP errors globally:

- `0`: server unreachable.
- `401`: session expired, clears session, redirects to login.
- `403`: permission denied.
- `400`, `404`, `409`: extracts backend error message.
- `500+`: generic server error toast.

This means components do not need duplicate error handling for every API failure.

## Models

Models define TypeScript interfaces/types for API payloads and frontend state.

### `auth.model.ts`

Defines:

- `AuthCredentials`: email and password.
- `SignupPayload`: registration data.
- `UserProfile`: profile response.
- `ChangePasswordPayload`.
- `JwtPayload`: decoded token shape.
- `UserRole`: `USER`, `ADMIN`, or `UNKNOWN`.

### `application.model.ts`

Defines:

- `ApplicationStatus`: `DRAFT`, `SUBMITTED`, `DOCS_VERIFIED`, `APPROVED`, `REJECTED`.
- `LoanType`: `PERSONAL`, `HOME`, `VEHICLE`, `EDUCATION`, `BUSINESS`.
- `INTEREST_RATES`: frontend EMI rates per loan type.
- `ApplicationRequest`: create/update payload.
- `ApplicationResponse`: user-facing response.
- `AdminApplicationResponse`: admin-facing response.

### `document.model.ts`

Defines:

- `DocumentType`: salary slip, bank statement, ID proof, address proof, other.
- `DocumentResponse`: document metadata returned by backend.

### `admin-user.model.ts`

Defines admin user records shown in user management.

### `toast.model.ts`

Defines toast type and message structure.

## Core Services

### `AuthService`

This is one of the most important frontend files.

Responsibilities:

- Login and signup through `/auth/login` and `/auth/signup`.
- Store JWT in localStorage under `finflow_token`.
- Decode JWT to get email and role.
- Expose Angular signals/computed values:
  - `email`
  - `role`
  - `loggedIn`
- Fetch and update profile.
- Change password.
- Store a local profile cache under `finflow_profile`.
- Logout and clear session.

Important Angular concept:

`signal` stores mutable reactive state. `computed` derives values from signals. When the token changes, `loggedIn`, `email`, and `role` update automatically.

### `ApplicationService`

Wraps user application APIs:

- `create(payload)` -> `POST /applications`
- `getAll()` -> `GET /applications`
- `getMine()` -> `GET /applications/my`
- `getById(id)` -> `GET /applications/{id}`
- `update(id, payload)` -> `PUT /applications/{id}`
- `delete(id)` -> `DELETE /applications/{id}`
- `submit(id)` -> `POST /applications/{id}/submit`
- `getStatus(id)` -> `GET /applications/{id}/status`

Angular components call this service instead of using `HttpClient` directly.

### `DocumentService`

Wraps document APIs:

- `upload(file, applicationId, documentType)` sends `FormData` to `/documents/upload`.
- `download(id)` gets a `Blob` response from `/documents/{id}`.
- `getByApplication(applicationId)` fetches metadata for an application.
- `hasDocuments(applicationId)` checks whether documents exist.

The upload method appends:

- `file`
- `applicationId`
- `documentType`

### `AdminService`

Wraps admin APIs:

- Get all applications.
- Get one application.
- Approve/reject one application.
- Bulk approve/reject.
- Update admin notes.
- Verify documents.
- Get users.
- Update user role.
- Get reports.

It also normalizes older/string user responses into `AdminUserRecord` objects.

### `ToastService`

Stores a signal list of toast messages and auto-removes each toast after 4 seconds.

Convenience methods:

- `success`
- `error`
- `info`
- `warning`

### `NotificationService`

Stores frontend-only notifications in localStorage under `finflow_notifications`.

It is used by admin pages to create applicant notifications after decisions. This is not backend-persisted. It works in the same browser profile.

## Shared Components

### `NavbarComponent`

The navbar changes based on role:

- User sees Dashboard and Applications.
- Admin sees Applications, Users, Reports.

It also shows:

- FinFlow logo.
- Notification bell.
- User email.
- Role dropdown.
- Profile link.
- Help link.
- Logout button.
- Mobile menu.

It uses:

- `AuthService` for role/email/logout.
- `NotificationService` for notification count and list.
- Angular `computed` for admin check and home link.
- `HostListener('document:click')` to close dropdowns when clicking outside.

### `StatusBadgeComponent`

Displays statuses with visual classes:

- `APPROVED` -> approved.
- `REJECTED` -> rejected.
- `SUBMITTED` -> submitted.
- `DOCS_VERIFIED` -> verified.
- default -> draft.

It replaces underscores with spaces for the label.

### `ConfirmDialogComponent`

Reusable modal for dangerous or important actions.

Inputs:

- `open`
- `title`
- `message`
- `confirmLabel`
- `tone`

Outputs:

- `confirmed`
- `cancelled`

Used for delete and decision confirmations.

### `LoaderComponent`

Skeleton loading UI shown while data is fetching.

### `ToastOutletComponent`

Renders all active toast messages from `ToastService`.

### Currency Pipes

There are two INR pipes:

- `currency-inr.pipe.ts`: accepts number/null/undefined and formats INR.
- `inr-currency.pipe.ts`: accepts number/string/null/undefined and formats INR.

Both use `Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' })`.

## Auth Pages

### Login Component

Files:

- `login.component.ts`
- `login.component.html`
- `login.component.css`

Key behavior:

- Reactive form with email and password.
- Validates required email, valid email, and required password.
- Calls `AuthService.login`.
- Shows loading state while submitting.
- On success, shows toast and navigates based on role:
  - admin -> `/admin/applications`
  - user -> `/dashboard`
- Has password show/hide toggle.
- Has shake animation state for invalid/error submissions.

### Signup Component

The signup page collects full user details:

- first name
- last name
- date of birth
- phone number
- email
- password
- referral code
- terms agreement

It uses reactive forms, password strength style UI, validation, and then calls `AuthService.signup`. Successful signup stores a local profile and JWT, then routes the user into the app.

## Dashboard

### `DashboardComponent`

The dashboard is the user home page after login.

It loads the current user's applications through `ApplicationService.getMine()`.

It calculates:

- total applications
- pending applications
- approved applications
- rejected applications
- total borrowed from approved applications
- applications this month
- next EMI date and amount based on latest approved application
- EMI preview calculator
- eligibility estimate based on monthly salary

Important Angular concepts:

- `signal` stores applications/loading/profile name.
- `computed` calculates summary, recent applications, stats, EMI breakdown, eligibility, greeting.
- `toSignal` converts reactive form `valueChanges` into signals.

## User Application Pages

### Applications List

Loads applications with `ApplicationService.getMine()`.

Features:

- Search by loan name or id.
- Filter by status.
- Pagination with page size 10.
- Timeline display per application.
- Submit draft directly from list.
- Delete with confirm dialog.

Business rule shown in UI:

- Submit is only available for `DRAFT`.
- Delete dialog is available from list, but backend enforces that only drafts can actually be deleted.

### Create Application

Creates a draft loan application.

Features:

- Loan type cards.
- Amount validation.
- Tenure selection.
- Interest rate preview from frontend constant.
- EMI preview.
- Autosave draft to localStorage every 30 seconds.
- Manual save draft.
- Submit form creates backend draft with `ApplicationService.create`.

Important localStorage keys:

- `finflow_application_draft`
- `finflow_application_draft_saved_at`

Backend result:

- Created application status is always `DRAFT`.

### Application Detail

Loads a single application by route id.

Features:

- View application details.
- Edit name and amount if draft.
- Upload supporting documents.
- View loaded document metadata.
- Submit application.
- Delete draft.
- Confirm dialog for submit/delete.

It imports `DocumentsUploadComponent`, so document upload is embedded inside this page.

## Document Upload Component

### `DocumentsUploadComponent`

This reusable component receives:

- `applicationId` input.

It emits:

- `uploaded` output with uploaded `DocumentResponse`.

It supports:

- File picker.
- Drag and drop.
- Document type selection.
- File type validation for PDF, JPG, JPEG, PNG.
- Max size validation of 5 MB.
- Upload progress state.
- Toast success/error.

Backend call:

```ts
documentService.upload(file, applicationId, selectedDocumentType)
```

## Profile Page

### `ProfileComponent`

Features:

- Loads cached profile first.
- Fetches current profile from backend.
- Edit profile mode.
- Save profile changes.
- Change password form.

Profile form:

- first name
- last name
- email disabled/read-only
- phone number with 10-digit pattern
- date of birth
- created at

Password form:

- current password
- new password with min length 8

## Admin Pages

### Admin Shell

The admin shell provides a fixed sidebar and nested router outlet.

It links to:

- applications
- users
- reports

It also loads report text from admin-service, though the current template does not visibly use `reportSummary`.

### Admin Applications

Admin can:

- Load all synced applications.
- Filter/search applications.
- Select applications for bulk decisions.
- Approve/reject individual applications.
- Export filtered rows as CSV.
- Create local applicant notifications.

It also loads users from auth-service through admin-service so applicant email can be displayed as full name when possible.

### Admin Application Detail

Admin can:

- View one application.
- View attached documents.
- Save admin notes.
- Mark documents verified.
- Approve or reject after document verification.
- View/download a document.

Important logic:

- If status is `SUBMITTED`, admin sees "Mark Documents Verified".
- If status is `DOCS_VERIFIED`, admin sees approve/reject decision buttons.
- If status is final, UI shows review completed.

### Admin Users

Admin can:

- Load users from auth-service through admin-service.
- Search by email or id.
- Change role draft in a select.
- Save role updates.

### Admin Reports

Admin reports loads all admin applications and computes:

- approved/rejected/pending counts.
- percentages.
- applications per month.
- total approved amount in current month.

Reports are computed in the frontend from application data, while `AdminService.getReports()` also exists as backend summary text.

## CSS And UI System

`src/styles.css` defines the global visual system:

- dark background colors
- text colors
- accent colors
- border radius
- shadows
- buttons
- forms
- table styles
- skeleton loading
- utility classes
- mobile media query

Individual component CSS files then add page-specific layout.

## How Angular Talks To Backend

Example: User creates application.

1. User fills create form.
2. `ApplicationCreateComponent.submit()` calls `ApplicationService.create`.
3. `ApplicationService.create()` sends `POST /applications`.
4. `authInterceptor` adds JWT.
5. Gateway validates JWT and adds `X-User-Email`.
6. application-service creates draft under that email.
7. Response returns to Angular.
8. Angular shows toast and navigates to detail page.

