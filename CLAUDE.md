# Klink Backend — Claude Code Rules

---

## SECTION 1 — MANDATORY: Use context7 for all code in this project

Before writing, editing, or generating any code in this project, use context7 to fetch the latest official documentation for every library involved in the task. This rule applies to every task, every file, every session, without exception.

### How to apply it
1. Identify every library the task touches
2. Call context7 for each one before writing anything
3. Use what context7 returns — not memorized training data
4. If context7 docs differ from what you were about to write, context7 wins

### Libraries to always check via context7
- Spring Boot
- Spring Security
- Spring Data JPA
- Spring Web MVC
- JWT / jjwt
- BCrypt / Spring Security Crypto
- Hibernate
- Jakarta Bean Validation
- Lombok
- Spring Mail / JavaMailSender
- JUnit 5 / Mockito / Spring Test

If the task involves any library not on this list, still fetch its docs via context7 before proceeding.

### At the start of every session
1. Read pom.xml and extract all dependencies and versions
2. Use context7 to fetch docs for Spring Boot, Spring Security, and jjwt at those exact versions
3. Say "Context7 loaded for [library list]. Ready." before accepting any task

---

## SECTION 2 — Commands

Run the app:
```
.\mvnw.cmd spring-boot:run
```

Build (skip tests):
```
.\mvnw.cmd package -DskipTests
```

Run all tests (requires live PostgreSQL on port 54321):
```
.\mvnw.cmd test
```

Run a single test class:
```
.\mvnw.cmd test -Dtest=FacilityServiceTest
```

---

## SECTION 3 — What This Project Is

This repository is the **backend** for **Klink** — a fully online church management system. The product that members, pastors, and church leaders use is a **mobile app** built with React Native. This repository is the backend REST API that the mobile app talks to.

The backend is built with **Spring Boot 3.5 / Java 17** and uses **Supabase** for the PostgreSQL database and file storage. It exposes a REST API consumed by the React Native mobile app over HTTP.

Klink is **multi-church**. Any church can create an account and get access to the full platform. Each church's data is completely private and isolated from every other church — enforced by filtering every single database query by `church_id`.

### Architecture
- Mobile app (React Native) — the client. NOT in this repo.
- This repository — the backend REST API built with Spring Boot.
- Supabase — provides the PostgreSQL database and file storage.

The backend never renders any UI. It only returns JSON.

### Tech Stack
- Java 17
- Spring Boot 3.5
- Supabase (PostgreSQL database + file storage)
- Spring Data JPA / Hibernate (`ddl-auto: update` — NO Flyway)
- Spring Security with JWT (jjwt 0.12.6)
- Maven

---

## SECTION 4 — Package Structure

```
com.example.demo
├── model         — JPA entity classes, one per database table
├── repository    — Spring Data JPA repository interfaces
├── service       — all business logic lives here
├── controller    — REST API endpoint controllers
├── dto
│   ├── request   — request DTOs (validated with Jakarta Bean Validation)
│   └── response  — response DTOs (static from() factory methods)
├── security      — JWT filter, auth config, MemberPrincipal, RoleChecker, RateLimiterService
├── scheduler     — Spring @Scheduled background jobs
├── event         — ApplicationEvent classes and @TransactionalEventListener handlers
├── validation    — custom constraint annotations (@ValidPhoneNumber)
└── config        — FirebaseConfig, GlobalExceptionHandler, WebConfig
```

---

## SECTION 5 — Roles and Enums

### Role enum
```java
public enum Role {
    PASTOR, ELDER, MANAGER, FINANCIAL_SECRETARY,
    GROUP_ADMIN, GROUP_FINANCIAL_SECRETARY, MEMBER
}
```

### Member Categories
```java
public enum Category { ADULT, YOUTH, CHILDREN }
```

### Member / Auth enums
```java
public enum MemberStatus { ACTIVE, DEACTIVATED }
public enum AttendanceMethod { QR_SCAN, MANUAL }
public enum AttendanceStatus { PRESENT, ABSENT }
public enum PaymentType { OFFERING, TITHE, WELFARE, DUES, SPECIAL_CONTRIBUTION }
public enum PaymentStatus { CONFIRMED, PENDING }
public enum PledgeStatus { UNPAID, PAID }
public enum GroupStatus { DRAFT, ACTIVE }
public enum VerificationTokenType { EMAIL_VERIFICATION, PASSWORD_RESET }
```

### Facility enums
```java
public enum FacilityType {
    SANCTUARY, HALL, OFFICE, PARKING, SCHOOL, CLINIC, LAND, EQUIPMENT, VEHICLE, OTHER
}
public enum FacilityCondition { EXCELLENT, GOOD, FAIR, POOR, NEEDS_REPAIR }
```

### Project enums
```java
public enum ProjectType {
    CONSTRUCTION, RENOVATION, PURCHASE, COMMUNITY_OUTREACH, EDUCATION, HEALTH, TECHNOLOGY, OTHER
}
public enum ProjectStatus {
    PROPOSED, APPROVED, FUNDRAISING, IN_PROGRESS, ON_HOLD, COMPLETED, CANCELLED
}
public enum ContributionPaymentMethod { CASH, CHEQUE, MOBILE_MONEY, BANK_TRANSFER }
```

### Store enums
```java
public enum StoreItemStatus { AVAILABLE, SOLD_OUT }
public enum CollectionStatus { AWAITING, COLLECTED }
```

### What each role can do
- **PASTOR** — full oversight; assigns Elders and Managers; cannot appoint another Pastor (only Elders can); cannot demote Elders
- **ELDER** — same as Pastor + can appoint Pastor (existing Pastor is auto-demoted to Member); cannot assign Managers; cannot appoint or demote other Elders; max 25 per church
- **MANAGER** — operational access (content, store, gallery, attendance, facilities); can demote Pastor or Elder to Member; cannot access finances; cannot manage groups; max 10 per church; only Pastor assigns Managers
- **FINANCIAL_SECRETARY** — finances only (offering, tithe, welfare, pledges, project contributions)
- **GROUP_ADMIN** — posts messages in their group; reads dues; cannot record dues
- **GROUP_FINANCIAL_SECRETARY** — records group dues; cannot post messages
- **MEMBER** — view-only for most things; votes on polls; buys store items; views own records

---

## SECTION 6 — Security Rules

### JWT
- JWT claims: `memberId`, `churchId`, `role`, `tokenVersion`; subject is email
- Access token TTL: 15 minutes
- Refresh token TTL: 30 days
- Every protected endpoint extracts `churchId` from the JWT — **NEVER from the request body**
- `MemberPrincipal` exposes: `getChurchId()`, `getMemberId()`, `getRole()`, `getMember()`

### JwtFilter guard chain (in order)
1. Token invalid signature/expiry → 401
2. Token is an attendance QR token (subject `"attendance-qr"`) used as Bearer → 401
3. Member not found → 401
4. Member status is DEACTIVATED → 401
5. Member has neither email nor phone verified → 403
6. Token issued before `member.passwordChangedAt` → 401 (handles post-password-change revocation)
7. Token `tokenVersion` doesn't match `member.tokenVersion` → 401 (handles logout and forced revocation)

### Token versioning
- `Member.tokenVersion` starts at 0 and is incremented on: logout, password change, email change
- Incrementing the version invalidates all existing JWTs for that member instantly
- All refresh tokens are revoked at the same time as the version bump

### Refresh token rotation
- Every call to `/auth/refresh` issues a new refresh token and revokes the old one
- Tokens are stored as SHA-256 hashes — raw token is never persisted
- Tokens belong to a `familyId`; if a revoked token is presented, the entire family is revoked (stolen token protection)
- Weekly cleanup job removes expired and revoked tokens from the database

### Rate limiting (RateLimiterService — in-memory sliding window)
- Login: 5 attempts per email+IP combination per 15 minutes; 20 attempts per IP per 15 minutes
- Register: 3 attempts per IP per hour
- Verify-email / resend-verification: 10 per IP per 15 minutes
- Login rate limiting is applied inside `AuthService` (not `RateLimitFilter`) because it needs the parsed email from the request body
- Other rate-limited endpoints are handled in `RateLimitFilter` before the JWT filter

### Account lockout
- 5 consecutive failed login attempts → account locked for 15 minutes (`lockedUntil` field)
- 24 hours of inactivity after a failure automatically resets the counter
- `incrementFailedLoginAttempts` uses a `@Modifying` bulk UPDATE — `clearAutomatically = true` ensures the next SELECT sees the fresh value

### Passwords and verification codes
- Passwords: BCrypt strength 12; never stored plaintext
- Verification codes: 6-digit, SHA-256 hashed before storage; raw code sent only via email/SMS
- Weak codes rejected at generation (all-same digit, sequential runs)
- Phone verification: max 5 attempts; `incrementPhoneVerificationAttemptsIfUnderLimit` is atomic

### Constant-time login
- When the email is not found, a dummy BCrypt hash is compared to prevent timing-based email enumeration

### Deactivated members
- Blocked in `JwtFilter` even with a valid, unexpired, correct-version JWT

### RoleChecker utility methods
```java
RoleChecker.require(principal, message, Role...)
RoleChecker.requirePastorOrElder(principal)
RoleChecker.requirePastorElderOrManager(principal)
RoleChecker.requirePastorOrManager(principal)
RoleChecker.requireManager(principal)
RoleChecker.requireFinancialSecretary(principal)
RoleChecker.requireFinancialSecretaryOrPrivileged(principal)  // FinSec + Pastor + Elder
RoleChecker.isPastorOrElder(principal)   // returns boolean
```

### HTTP security headers
- HSTS: 1 year + includeSubDomains
- CSP: `default-src 'none'; frame-ancestors 'none'`
- X-Frame-Options: DENY
- Referrer-Policy: NO_REFERRER
- Permissions-Policy: camera, microphone, geolocation all off
- CORS: disabled by default; only enabled if `CORS_ALLOWED_ORIGINS` env var is set

---

## SECTION 7 — Code Patterns

### Entity pattern
Every entity uses:
```java
@Entity @Table @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
```
- `@CreationTimestamp` for `createdAt` (updatable = false)
- `@UpdateTimestamp` for `updatedAt`
- `@Builder.Default` required for any field with a default value in a `@Builder` class
- Soft delete via `deletedAt` (LocalDateTime); hard delete is never used for member data
- Every table has a `church_id` column
- `Group` entity is declared as `@Entity(name = "ChurchGroup")` to avoid the JPQL reserved word `GROUP`

### Response DTO pattern
Every response DTO has a `static from(Entity)` factory method. No entity is ever returned from a controller.

### Repository pattern
- Every query includes church_id: `findByChurchIdAnd...`
- Soft delete queries: `AND e.deletedAt IS NULL`
- `@Modifying @Query` for bulk UPDATE operations (e.g., clearing isPrimary flags)
- Use `NOT EXISTS` subqueries, not `NOT IN`, for welfare defaulter and similar queries — `NOT IN` silently returns no rows when the subquery contains any NULL
- `@Lock(LockModeType.PESSIMISTIC_WRITE)` on store item purchase to prevent overselling race conditions

### Service pattern
- `@Service @RequiredArgsConstructor @Transactional`
- Read-only methods annotated `@Transactional(readOnly = true)`
- Role check first, then load entity, then business logic
- `church_id` always from `principal.getChurchId()`

### Controller pattern
- `@RestController @RequestMapping("/api/v1/...") @RequiredArgsConstructor`
- `@AuthenticationPrincipal MemberPrincipal principal` on every protected endpoint
- `@PageableDefault(size = 20) Pageable pageable` on list endpoints
- 201 CREATED for POST, 204 NO CONTENT for DELETE, 200 OK otherwise

### Jakarta Bean Validation
Use on every request DTO field. Common annotations:
- `@NotBlank`, `@NotNull`, `@Size(min=1, max=N)`, `@Email`
- `@DecimalMin("0.01")` for monetary amounts
- `@PastOrPresent` for dates
- `@Min`, `@Max` for integer fields
- `@Pattern(regexp = "\\d{4}-\\d{2}")` for YYYY-MM payment month strings
- `@ValidPhoneNumber` for E.164 phone numbers (custom annotation in `validation` package)
- `@AssertTrue` for cross-field validation (e.g. "must have email or phone, not neither")

### Event system
Notifications and emails are published as Spring `ApplicationEvent` objects and handled asynchronously after the transaction commits:
```java
// publish in a service method:
eventPublisher.publishEvent(new NotificationEvent(this, churchId, title, body));
eventPublisher.publishEvent(new VerificationEmailEvent(this, email, name, rawCode, false));
eventPublisher.publishEvent(new SmsVerificationEvent(this, phoneNumber, name, rawCode));

// listeners use:
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
```
This ensures notifications never fire if the transaction rolls back.

### AuditLogService
Log-only audit trail (SLF4J). Call it for auth events and sensitive data mutations:
```java
auditLogService.loginSuccess(memberId, email, ip);
auditLogService.loginFailure(email, ip);
auditLogService.accountLocked(email, ip);
auditLogService.passwordChanged(memberId, ip);
auditLogService.facilityCreated(actorId, facilityId, name);
auditLogService.projectStatusChanged(actorId, projectId, from, to);
auditLogService.contributionRecorded(actorId, projectId, memberId, amount);
// ... and others
```

### NotificationService
```java
boolean notifyMember(Member member, String title, String body)
int notifyMembers(List<Member> members, String title, String body)
int notifyAllMembers(UUID churchId, String title, String body)  // paginates 200 at a time
```
- FCM push notification for `hasSmartphone = true` members (if `fcmToken` is set)
- SMS for `hasSmartphone = false` members (if phone is set)
- Both services degrade gracefully if not configured

---

## SECTION 8 — Database Tables

Every table has a `church_id` column. Every query MUST filter by `church_id`.

### Auth tables
- `refresh_tokens` — id, token_hash (SHA-256), family_id (UUID), member_id, revoked (boolean), expires_at, created_at
- `verification_tokens` — id, email, type (EMAIL_VERIFICATION / PASSWORD_RESET), code_hash (SHA-256), used (boolean), created_at, expires_at

### Core tables
- `churches` — id, church_name, location, denomination, church_code (unique), welfare_amount, contact_phone, contact_email, created_at
- `members` — id, church_id, full_name, phone (display), phone_number (E.164, unique, for auth), email, role, category, has_smartphone, qr_code_value (unique), date_of_birth, status, password_hash, fcm_token, photo_url, token_version (int, default 0), password_changed_at, email_verified, phone_verified, phone_verification_code_hash, phone_verification_code_expires_at, phone_verification_attempts, last_phone_verification_attempt_at, failed_login_attempts, last_failed_at, locked_until, deactivated_by, deactivated_at, registered_by, auth_user_id, created_at, updated_at
- `attendance` — id, church_id, member_id, service_name, service_date, time_of_scan, marked_by, method (QR_SCAN / MANUAL), status (PRESENT / ABSENT). UNIQUE on (member_id, service_date, service_name).
- `attendance_sessions` — id, church_id, service_name, service_date, expires_at, processed (boolean), qr_token. Used by `AttendanceScheduler` to auto-mark absent members after a session expires.
- `payments` — id, church_id, member_id (null for offering), group_id (null for church payments), payment_type, amount, payment_month (YYYY-MM), payment_date, status (CONFIRMED / PENDING), recorded_by, created_at
- `pledges` — id, church_id, member_id, description, amount, amount_paid, paid_at, status (UNPAID / PAID), recorded_by, created_at
- `pledge_payments` — id, church_id, pledge_id, amount, payment_date

### Group tables
- `groups` — id, church_id, group_name, description, dues_amount, group_admin_id, group_fin_sec_id, status (DRAFT / ACTIVE), created_by, created_at. Entity declared as `@Entity(name = "ChurchGroup")`.
- `group_members` — id, church_id, group_id, member_id, joined_at. UNIQUE on (group_id, member_id).
- `group_messages` — id, church_id, group_id, content, posted_by, created_at

### Content tables
- `announcements` — id, church_id, title, body, flyer_url, posted_by, created_at
- `events` — id, church_id, title, description, event_date, reminder_sent (boolean), created_by, created_at
- `sermons` — id, church_id, preacher, title, memory_verse, scripture, sermon_date, audio_url, notes, posted_by, created_at
- `devotionals` — id, church_id, title, content, devotional_date, posted_by, created_at
- `gallery` — id, church_id, photo_url, caption, uploaded_by, uploaded_at
- `church_files` — id, church_id, title, category, language, file_url, uploaded_by, uploaded_at. Max 10 files per church.
- `polls` — id, church_id, question, options (jsonb — stored as `List<String>` via `@JdbcTypeCode(SqlTypes.JSON)`), closes_at, created_by, created_at
- `poll_votes` — id, church_id, poll_id, member_id, selected_option, voted_at. UNIQUE on (poll_id, member_id).
- `hall_of_fame` — id, church_id, member_id (optional), title, description, photo_url, posted_by, created_at

### Store tables
- `store_items` — id, church_id, name, description, price, quantity, category, photo_url, status (AVAILABLE / SOLD_OUT), created_by, created_at
- `store_payments` — id, church_id, member_id, item_id, amount, date_paid, collection_status (AWAITING / COLLECTED), collected_by, collected_at

### Facilities tables
- `facilities` — id, church_id, name, description, facility_type, address, capacity, year_acquired, estimated_value, currency, `condition_status` (column name — reserved word avoidance), is_active, notes, created_by, created_at, updated_at, deleted_at. Soft delete.
- `facility_images` — id, facility_id, church_id, image_url, caption, is_primary, uploaded_by, uploaded_at, sort_order

### Projects tables
- `church_projects` — id, church_id, title, description, project_type, status, target_amount, amount_raised (always recalculated, never from request), currency, start_date, expected_end_date, actual_end_date, location, contractor, facility_id (UUID — not a FK), created_by, approved_by, approved_at, is_public, created_at, updated_at, deleted_at. Soft delete.
- `project_updates` — id, project_id, church_id, title, content, posted_by, posted_at, updated_at
- `project_images` — id, project_id, update_id (optional), church_id, image_url, caption, is_primary, uploaded_by, uploaded_at, sort_order, phase
- `project_contributions` — id, project_id, member_id, church_id, amount, currency, contribution_date, payment_method, recorded_by, notes, created_at

---

## SECTION 9 — API Endpoints

All endpoints prefixed `/api/v1`. Protected endpoints require `Authorization: Bearer <JWT>`.

### Auth
```
POST   /api/v1/auth/register-church          Public — register church + create Pastor account
POST   /api/v1/auth/register                 Public — member self-signup with church code; rate-limited 3/IP/hour
POST   /api/v1/auth/login                    Public — email or phone + password; rate-limited 5/email+IP/15min
POST   /api/v1/auth/logout                   Protected (all) — revokes refresh token, bumps tokenVersion
POST   /api/v1/auth/refresh                  Public — refresh token rotation; reuse detection
POST   /api/v1/auth/forgot-password          Public — sends 6-digit reset code via email
POST   /api/v1/auth/reset-password           Public — verifies code, sets new password
POST   /api/v1/auth/change-password          Protected (all) — old password required; revokes all refresh tokens
POST   /api/v1/auth/verify-email             Public — verifies 6-digit email code; rate-limited 10/IP/15min
POST   /api/v1/auth/resend-verification      Public — resend email verification code; rate-limited
POST   /api/v1/auth/verify-phone             Protected (all) — verifies 6-digit SMS code; max 5 attempts
POST   /api/v1/auth/resend-phone-verification  Protected (all) — resend SMS verification code
PUT    /api/v1/auth/phone                    Protected (all) — update phone number; triggers re-verification
POST   /api/v1/auth/fcm-token               Protected (all) — register Firebase push token
```

### Members
```
POST   /api/v1/members/register              Pastor, Elder, Manager — register non-smartphone member
GET    /api/v1/members                       Pastor, Elder, Manager — list all members (paginated)
GET    /api/v1/members/{id}                  Pastor, Elder, Manager, own profile
PUT    /api/v1/members/{id}                  Pastor, Elder, Manager, own profile
GET    /api/v1/members/{id}/qr               Pastor, Elder, Manager, own QR
PUT    /api/v1/members/{id}/role             Role-based (see role assignment rules in Section 10)
POST   /api/v1/members/{id}/deactivate       Pastor, Elder
POST   /api/v1/members/{id}/reactivate       Pastor, Elder, Manager
```

### Attendance
```
POST   /api/v1/attendance/scan               All — self check-in via QR attendance token
POST   /api/v1/attendance/session            Pastor, Elder, Manager — generate QR session (creates AttendanceSession)
POST   /api/v1/attendance/manual             Pastor, Elder, Manager — manually mark a member present
GET    /api/v1/attendance                    Pastor, Elder, Manager — all records (paginated, @PageableDefault(size=50))
GET    /api/v1/attendance/me                 All — own attendance history (paginated)
```

### Finances
```
POST   /api/v1/finances/offering             Financial Secretary — no member_id; church total
POST   /api/v1/finances/tithe                Financial Secretary
POST   /api/v1/finances/welfare              Financial Secretary
GET    /api/v1/finances/welfare/defaulters   Fin Sec, Pastor, Elder (paginated)
POST   /api/v1/finances/welfare/remind       Financial Secretary — sends push/SMS to defaulters
GET    /api/v1/finances/me                   All — own tithe and welfare (paginated)
```

### Pledges
```
POST   /api/v1/pledges                       Financial Secretary
GET    /api/v1/pledges                       Fin Sec, Pastor, Elder (paginated)
GET    /api/v1/pledges/me                    All — own pledges (paginated)
PUT    /api/v1/pledges/{id}/pay              Financial Secretary
```

### Groups
```
POST   /api/v1/groups                        Pastor, Elder
GET    /api/v1/groups                        Pastor, Elder (paginated)
POST   /api/v1/groups/{id}/members           Pastor, Elder
POST   /api/v1/groups/{id}/messages          Group Admin
GET    /api/v1/groups/{id}/messages          Group members, Group Admin (paginated)
POST   /api/v1/groups/{id}/dues/pay          Group Financial Secretary
GET    /api/v1/groups/{id}/dues              Group Fin Sec, Group Admin
POST   /api/v1/groups/{id}/dues/generate     Manual trigger to generate dues records for the month
```

### Announcements
```
POST   /api/v1/announcements                 Manager
GET    /api/v1/announcements                 All (paginated)
DELETE /api/v1/announcements/{id}            Manager
```

### Events
```
POST   /api/v1/events                        Pastor, Elder, Manager
GET    /api/v1/events                        All (paginated)
DELETE /api/v1/events/{id}                   Pastor, Elder, Manager
```

### Sermons
```
POST   /api/v1/sermons                       Pastor, Elder, Manager
GET    /api/v1/sermons                       All (paginated)
DELETE /api/v1/sermons/{id}                  Pastor, Elder, Manager
```

### Devotionals
```
POST   /api/v1/devotionals                   Pastor, Elder, Manager
GET    /api/v1/devotionals                   All (paginated)
```

### Gallery
```
POST   /api/v1/gallery                       Manager — photos only (jpg, png, webp); magic byte validated
GET    /api/v1/gallery                       All (paginated)
```

### Church Files
```
POST   /api/v1/files                         Pastor, Elder, Manager — PDF only, max 30MB, max 10 per church
GET    /api/v1/files                         All — filterable by ?category= and ?language=
DELETE /api/v1/files/{id}                    Pastor, Elder, Manager
```

### Store
```
POST   /api/v1/store/items                   Manager
PUT    /api/v1/store/items/{id}              Manager
GET    /api/v1/store/items                   All (paginated)
POST   /api/v1/store/pay                     All — buy an item (SELECT FOR UPDATE on quantity)
PUT    /api/v1/store/payments/{id}/collect   Manager
GET    /api/v1/store/payments                Fin Sec, Pastor, Elder (paginated)
GET    /api/v1/store/my-purchases            All (paginated)
```

### Polls
```
POST   /api/v1/polls                         Pastor, Elder, Manager
GET    /api/v1/polls                         All (paginated)
POST   /api/v1/polls/{id}/vote               All — one vote per member enforced
GET    /api/v1/polls/{id}/results            Pastor, Elder, Manager
```

### Hall of Fame
```
POST   /api/v1/hall-of-fame                  Pastor, Elder, Manager
GET    /api/v1/hall-of-fame                  All (paginated)
PUT    /api/v1/hall-of-fame/{id}             Pastor, Elder, Manager
DELETE /api/v1/hall-of-fame/{id}             Pastor, Elder, Manager
```

### Facilities
```
POST   /api/v1/facilities                    Pastor, Manager — create facility
GET    /api/v1/facilities                    All — list; filter by ?facilityType=, ?condition=, ?isActive=
GET    /api/v1/facilities/{id}               All — detail with images
PUT    /api/v1/facilities/{id}               Pastor, Manager — patch update
DELETE /api/v1/facilities/{id}               Pastor, Elder — soft delete
POST   /api/v1/facilities/{id}/images        Pastor, Elder, Manager — add image
GET    /api/v1/facilities/{id}/images        All
DELETE /api/v1/facilities/{id}/images/{imageId}  Pastor, Manager, or original uploader
PUT    /api/v1/facilities/{id}/images/{imageId}/primary  Pastor, Manager
```

### Projects
```
POST   /api/v1/projects                      Pastor, Elder, Manager — always starts as PROPOSED
GET    /api/v1/projects                      All public; privileged roles see all; filter by ?status=, ?projectType=
GET    /api/v1/projects/{id}                 All public; privileged see private
PUT    /api/v1/projects/{id}                 Pastor, Elder, Manager — patch update
PUT    /api/v1/projects/{id}/status          Pastor, Elder, Manager — status transition
DELETE /api/v1/projects/{id}                 Pastor, Elder — soft delete
GET    /api/v1/projects/dashboard            Pastor, Elder, Manager — aggregate stats
POST   /api/v1/projects/{id}/updates         Pastor, Elder, Manager — notifies contributors async
GET    /api/v1/projects/{id}/updates         All (visibility rules apply, paginated)
POST   /api/v1/projects/{id}/images          Pastor, Elder, Manager
GET    /api/v1/projects/{id}/images          All (visibility rules apply); filter by ?phase=
DELETE /api/v1/projects/{id}/images/{imageId}  Pastor, Elder, Manager
PUT    /api/v1/projects/{id}/images/{imageId}/primary  Pastor, Elder, Manager
POST   /api/v1/projects/{id}/contributions   Financial Secretary
GET    /api/v1/projects/{id}/contributions   All authenticated; members see anonymised data via fromAnonymous()
GET    /api/v1/projects/{id}/contributions/summary  All authenticated
GET    /api/v1/projects/my-contributions     All authenticated (paginated)
```

### Church Settings
```
GET    /api/v1/church/settings               All — get church info (includes deletedAt, scheduledDeletionAt)
PUT    /api/v1/church/settings               Pastor, Elder — update church name/location/contact/welfare
POST   /api/v1/church/photo                  Pastor, Elder, Manager — upload church photo
POST   /api/v1/church/regenerate-code        Pastor, Elder — generate new church join code
DELETE /api/v1/church                        Elder only — soft-delete; sets deletedAt; 30-day grace period
POST   /api/v1/church/restore                Elder only — clears deletedAt; only valid within 30 days of deletion
```

### Media Upload
```
POST   /api/v1/media/upload                  Pastor, Elder, Manager
                                             Request: multipart/form-data, param: file, folder (optional)
                                             Validates magic bytes; max 10MB; JPEG/PNG/WebP/HEIC only
                                             folder param is sanitized: only [a-zA-Z0-9_\-] allowed
                                             Returns: { imageUrl, fileName, fileSize, mimeType }
```

---

## SECTION 10 — Business Rules

### Church isolation
- Every table has `church_id`
- Every query filters by `church_id`
- `church_id` always comes from the JWT token, never from the request body

### Church deletion (soft-delete with 30-day grace period)
- Only an **Elder** can call `DELETE /api/v1/church` — sets `Church.deletedAt` to now
- Once `deletedAt` is set, `JwtFilter` blocks ALL requests from members of that church with 403
- The only exception: an Elder can still call `POST /api/v1/church/restore` during the grace period
- `POST /api/v1/church/restore` clears `deletedAt` — only valid if `deletedAt + 30 days` is still in the future; returns 410 GONE if the window has passed
- `ChurchDeletionScheduler` runs daily at 2am; permanently deletes any church where `deletedAt < now - 30 days`
- Permanent deletion cascades in FK order through all tables, then deletes Supabase storage files (best-effort)
- `ChurchResponse` now includes `deletedAt` and `scheduledDeletionAt` (= deletedAt + 30 days, or null)

### Project status transitions
```
PROPOSED    → APPROVED, CANCELLED
APPROVED    → FUNDRAISING, IN_PROGRESS, CANCELLED
FUNDRAISING → IN_PROGRESS, ON_HOLD, CANCELLED
IN_PROGRESS → ON_HOLD, COMPLETED, CANCELLED
ON_HOLD     → IN_PROGRESS, CANCELLED
COMPLETED   → (terminal)
CANCELLED   → (terminal)
```
- When COMPLETED or CANCELLED: `actualEndDate` is set to today
- When APPROVED: `approvedBy` and `approvedAt` are set
- `amountRaised` is recalculated from SUM of contributions in the same `@Transactional` call; never accepted from request

### Facilities
- Soft delete only (Pastor or Elder)
- Condition column uses name `condition_status` to avoid SQL reserved word conflict
- `isActive` tracks operational status separately from soft delete
- At most one `isPrimary` image per facility; setting a new primary clears others atomically via `@Modifying` bulk UPDATE

### Contributions (projects)
- Only Financial Secretary records contributions
- Cannot contribute to COMPLETED or CANCELLED projects
- When a project update is posted, all distinct contributors to that project are notified (not all church members)
- Regular members see anonymised contribution list via `fromAnonymous()` (memberId, memberName, recordedBy, notes are null)
- Privileged roles (Pastor, Elder, Manager, FinSec) see full detail via `from()`

### Role assignment rules
- **PASTOR** can: appoint Elders (max 25); appoint/remove Managers (max 10); demote anyone to Member
- **PASTOR** cannot: appoint another Pastor; demote an Elder
- **ELDER** can: appoint a new Pastor (existing Pastor is automatically demoted to Member); demote a Pastor
- **ELDER** cannot: appoint other Elders; demote other Elders; assign Managers
- **MANAGER** can: demote a Pastor or Elder to Member only
- **MANAGER** cannot: appoint any role

### Payments table
- `group_id` null = church payment (offering/tithe/welfare)
- `group_id` with value = group dues
- `member_id` null for offering (church total, not per-member)
- Store purchases go in `store_payments`, never in `payments`

### Welfare
- Fixed amount per church set by Pastor (`church.welfareAmount`)
- Monthly reminder on last day of month to unpaid members
- Push notification for smartphone members; SMS for non-smartphone members

### Attendance
- Duplicate scan (same member + service date + service name) returns 409 CONFLICT
- Attendance QR tokens have subject `"attendance-qr"` and cannot be used as Bearer JWTs — `JwtFilter` rejects them
- `AttendanceSession` records are created when a QR session is started; `AttendanceScheduler` polls every 30 minutes for expired unprocessed sessions and bulk-inserts ABSENT records for members who never scanned in

### Store
- `buyItem` uses SELECT FOR UPDATE (`findByChurchIdAndIdForUpdate`) to prevent overselling
- On purchase: quantity is decremented; if it hits zero, status is set to SOLD_OUT

### File uploads
- Gallery: images only (jpg, png, webp); magic byte validated
- Church files: PDF only, max 30MB per file, max 10 files per church; magic byte validated
- Sermons: audio (mp3, m4a); magic byte validated
- Media endpoint: images only (JPEG/PNG/WebP/HEIC), max 10MB, magic byte validated

### Group status
- Group `status` stays DRAFT until `group_admin_id` is assigned; only then does it become ACTIVE
- `group_fin_sec_id` is optional at all times

---

## SECTION 11 — Scheduled Jobs

All use `@Scheduled`. `@EnableScheduling` is on `Demo5Application`.

| Job | Cron | What |
|-----|------|------|
| Welfare defaulter reminder | `0 0 9 L * *` (last day of month, 9:00am) | Finds members with no welfare payment for current month; sends push/SMS |
| Group dues generation | `0 0 0 1 * *` (1st of month, midnight) | Creates PENDING dues record for every active group member |
| Group dues reminder | `0 30 9 L * *` (last day of month, 9:30am) | Notifies members with unpaid group dues |
| Birthday check | `0 0 8 * * *` (daily, 8:00am) | Sends personalised birthday push/SMS to all churches globally |
| Monthly pledge reminder | `0 0 8 1 * *` (1st of month, 8:00am) | Notifies all members with any UNPAID pledge |
| Event reminder | `0 0 8 * * *` (daily, 8:00am) | Notifies members of events in next 48 hours; only sets `reminderSent=true` if at least one notification was sent |
| Attendance absent marking | `0 0/30 6-23 * * *` (every 30 min, 6am–11:30pm) | Finds expired unprocessed `AttendanceSession` records; bulk-inserts ABSENT records for members who didn't scan; marks session as processed |
| Token cleanup | `0 0 3 * * SUN` (Sunday, 3:00am) | Deletes all revoked and expired refresh tokens from the database |
| Church purge | `0 0 2 * * *` (daily, 2:00am) | Permanently deletes churches where `deletedAt < now - 30 days`; cascades all data; cleans Supabase storage |

---

## SECTION 12 — Important Notes for Claude Code

- This repo is **backend only**. Never generate UI, screens, or frontend code. Only build the REST API.
- No Flyway. Schema is managed by Hibernate `ddl-auto: update`. Define schema via JPA entity annotations and `@Table(indexes={...})`.
- Always filter every database query by `church_id` — no exceptions.
- Never hardcode roles; always use the `Role` enum.
- Never trust `churchId` from the request body; always use `principal.getChurchId()`.
- `@Builder.Default` is required for any field that has a default value on a `@Builder` entity.
- The `condition` field on `Facility` uses `@Column(name = "condition_status")` to avoid the SQL reserved word.
- The `Group` entity uses `@Entity(name = "ChurchGroup")` to avoid the JPQL reserved word `GROUP`. Use `ChurchGroup` in all JPQL queries referencing this entity.
- `Member` has two phone fields: `phoneNumber` (E.164 format, unique, used for login/verification) and `phone` (display string, shown in the app). They are separate columns.
- `ContributionPaymentMethod` is named that way (not `PaymentMethod`) to avoid naming conflicts with other library classes.
- `amountRaised` on `ChurchProject` is always recalculated from `SUM(amount) FROM project_contributions` in the same `@Transactional` method; it is never accepted in any request DTO.
- Deactivated members must be blocked even if their JWT is still valid — this is enforced in `JwtFilter`.
- Attendance QR tokens (subject `"attendance-qr"`) must never be accepted as Bearer auth tokens — `JwtFilter` rejects them explicitly.
- `Member.tokenVersion` must be incremented (and all refresh tokens revoked) on: logout, password change, email change.
- All monetary values must use `BigDecimal` — never `float` or `double`.
- Soft delete everywhere for member and church data — `deletedAt` field, never hard delete.
- No entity is ever returned directly from a controller — always map to a response DTO via `static from()`.
- When posting a project update, notify all contributors to that project (not all church members). Get contributor IDs via `projectContributionRepository.findDistinctMemberIdsByProjectId()`.
- When approving a project, set `approvedBy` and `approvedAt`; when completing or cancelling, set `actualEndDate`.
- `ProjectService.getDashboard` is restricted to Pastor, Elder, Manager.
- Regular members viewing contributions see `fromAnonymous()` — no PII exposed.
- Media upload endpoint validates magic bytes before accepting; declared `Content-Type` is not trusted alone.
- The `folder` parameter in `MediaUploadService.upload()` is sanitized by stripping everything except `[a-zA-Z0-9_\-]` to prevent path traversal.
- Use `NOT EXISTS` subqueries (not `NOT IN`) for "members who have not done X" queries — `NOT IN` returns no rows when the subquery contains any NULL.
- When a store item is purchased, always decrement quantity and set status to SOLD_OUT if quantity reaches zero.
- All event/notification publishing must use `@TransactionalEventListener(phase = AFTER_COMMIT)` so notifications are not sent if the transaction rolls back.
