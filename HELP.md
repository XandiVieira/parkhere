# ParkHere - Development Log

## Project Vision
A collaborative app for discovering parking spots in Brazil. Not just formal parking lots,
but street parking, informal spots, and everything in between. Think "Waze for parking availability + cost + reliability".

**Core value proposition:** "Discover where it's worth trying to park right now."

### Key Differentiators
- Crowd-sourced real-time info that nobody else has: availability, cost, informal charges, safety
- Probability-based (not binary) — shows confidence levels, not guarantees
- Trust score system — more confirmations from diverse users = higher reliability
- GPS-verified confirmations weigh more than remote reports

## Architecture

### Domain Model (Planned)
- **ParkingSpot** — a place where you can park (street, lot, mall, zona azul, terrain)
- **ParkingReport** — a user-submitted confirmation/update about a spot
- **User** — registered user with reputation score
- **UserReputation** — trust metrics (accepted/rejected confirmations, activity)
- **SpotAnalytics** — aggregated patterns (occupancy by hour/weekday, avg price)

### Services (Planned)
1. **User Service** — registration, authentication, reputation, preferences
2. **Location Service** — CRUD parking spots, geospatial search (radius), filters
3. **Report/Confirmation Service** — user confirmations, divergences, observations
4. **Trust Score Service** — recalculate confidence, temporal decay, abuse detection
5. **Analytics Service** — patterns by time, busiest regions, average prices

### Trust/Confidence System
- Score per spot based on: confirmation count, recency, user diversity, consistency, GPS proximity
- Information "decays" over time — old unconfirmed data loses trust
- GPS check-in confirmations worth more than remote ones
- New users have less weight initially
- Display levels: High / Medium / Low / No recent data

### Geospatial Strategy
- PostgreSQL + PostGIS for all location queries
- Primary search is by radius (e.g., "spots within 800m")
- Sort by combo of distance + confidence + availability chance
- Filter by price, type, recency of confirmation

## MVP Scope (Phase 1)
- [x] User registration and authentication (JWT)
- [x] Add parking spot (with type, location, price range)
- [x] Search spots by radius (geospatial)
- [x] Submit confirmation/report on a spot
- [x] Spot detail view with aggregated info (summary endpoint)
- [x] Trust score calculation (5-factor weighted algorithm)
- [x] User reputation system
- [x] Information decay (scheduled job)
- [x] Spot update endpoint (creator only)
- [x] Community-driven spot removal (3-confirmation threshold)
- [x] Search filters + pagination
- [x] Abuse/spam detection (30-min cooldown)
- [x] Report weighting by reputation
- [x] Favorites/bookmarks
- [x] Password change
- [x] Admin endpoints (deactivate spots, delete reports, ban/unban users)
- [x] Soft delete for spots

### NOT in MVP
- Reservations, payments, navigation, ML predictions, camera recognition, complex gamification

## Initial Niche Strategy
Focus on **bars/nightlife areas** or **hospitals/clinics** for cold-start — high pain, high recurrence.

## Sensitive Topics
- "Flanelinha" (informal parking attendants) should be referenced neutrally:
  - "frequent informal charge reported"
  - "frequent approach at this location"
  - "unofficial extra cost reported"

## Session Log

### Session 1 (2026-04-02)
- Project initialized with Spring Boot 4.0.5, Java 21
- Created CLAUDE.md with coding conventions
- Created project development log (this file)
- Created Postman collection structure
- Added missing dependencies: Flyway, JWT, Hibernate Spatial
- Established coding conventions: var for locals, functional style, minimal comments, SLF4J, unit test coverage
- Git repo initialized with local user config (personal account on work machine)
- **User Service implemented:**
  - User entity (UUID id, name, email, password, role, reputationScore, active, timestamps)
  - Role enum (USER, ADMIN)
  - UserRepository with findByEmail/existsByEmail
  - JWT auth: JwtService (generate/validate tokens), JwtAuthenticationFilter
  - SecurityConfig: stateless sessions, JWT filter, BCrypt, public auth endpoints
  - DTOs: RegisterRequest, LoginRequest, UpdateUserRequest, AuthResponse, UserResponse
  - AuthController: POST /api/auth/register (201), POST /api/auth/login (200)
  - UserController: GET /api/users/me, PUT /api/users/me (authenticated)
  - GlobalExceptionHandler: 409 email conflict, 401 bad credentials, 400 validation, 500 generic
  - Flyway migration V1: users table
  - 22 unit tests (JwtService: 5, UserService: 7, AuthController: 5, UserController: 4, contextLoads: 1)
  - Postman collection updated with all 4 endpoints + auto token extraction
- **Spring Boot 4 notes:**
  - `@WebMvcTest` moved to `org.springframework.boot.webmvc.test.autoconfigure`
  - ObjectMapper not auto-wired in `@WebMvcTest` — instantiate manually
  - Need `HttpStatusEntryPoint(UNAUTHORIZED)` in SecurityConfig for proper 401 responses
- **BaseEntity** extracted with id, createdAt, updatedAt — all entities extend it with @SuperBuilder
- **i18n implemented** following mailing-service pattern:
  - DomainException (messageKey + arguments), LocalizedMessageService, MessageSourceConfig
  - messages_en.properties + messages_pt.properties in src/main/resources/i18n/
  - GlobalExceptionHandler uses translated messages
- **API versioning**: all endpoints at /api/v1/...
- **Postman E2E Flow** folder with sequential test suite
- **Location Service implemented:**
  - ParkingSpot entity (extends BaseEntity, PostGIS Point geometry, SpotType, price range, trust score)
  - SpotType enum: STREET, PARKING_LOT, MALL, TERRAIN, ZONA_AZUL
  - ParkingSpotRepository with native PostGIS query (ST_DWithin + ST_Distance for radius search)
  - ParkingSpotService: create, searchByRadius, getById, getByUser
  - ParkingSpotController: POST /api/v1/spots, GET /api/v1/spots (radius search), GET /api/v1/spots/{id}, GET /api/v1/spots/mine
  - Flyway migration V2: parking_spots table with PostGIS extension + GIST index
  - SpotNotFoundException with i18n
  - 39 total tests (14 new for spots)
- **Report/Confirmation Service implemented:**
  - ParkingReport entity (extends BaseEntity, refs spot + user, availability, price, safety, informal charge, note, GPS distance)
  - AvailabilityStatus enum: AVAILABLE, UNAVAILABLE, UNKNOWN
  - ParkingReportRepository with recent reports queries (24h window for summary)
  - ParkingReportService: submitReport (with Haversine GPS distance calc), getReportsForSpot, getSummary
  - Summary aggregation: dominant availability, avg price, avg safety, informal charge %, last report time
  - ParkingReportController: POST /api/v1/spots/{id}/reports, GET .../reports, GET .../summary
  - Flyway migration V3: parking_reports table with indexes
  - 51 total tests (12 new for reports)

### Session 2 (2026-04-06)

Major feature expansion — all high and medium priority backend features implemented.

- **Soft Delete Infrastructure:**
  - Added `active` boolean to ParkingSpot (migration V6)
  - All queries filter by active=true — inactive spots hidden from search, reports, details
  - TrustLevel enum (HIGH/MEDIUM/LOW/NO_DATA) added to SpotResponse and SpotSummaryResponse

- **Trust Score Calculation (TrustScoreService):**
  - 5-factor weighted algorithm (0.0-1.0 score) over 72h report window:
    - Confirmation volume (0.25 weight) — saturates at 10 reports
    - Recency (0.20) — exponential decay, 12h half-life
    - User diversity (0.20) — saturates at 5 distinct users
    - Consistency (0.20) — agreement ratio on availability status
    - GPS proximity (0.15) — reports within 500m, closer = higher
  - Recalculated after every new report submission

- **User Reputation System (ReputationService):**
  - 0.0-100.0 score from 4 components:
    - Report count (40pts max, 0.5 pts/report)
    - GPS proximity (30pts max, avg proximity factor)
    - Consistency (20pts max, agreement with dominant status)
    - Account age (10pts max, 300 days to max)
  - Recalculated after every report

- **Trust Score Decay Job (TrustScoreDecayJob):**
  - @Scheduled hourly cron job
  - Recalculates trust for active spots with score > 0
  - Configurable via `parkhere.decay.cron` and `parkhere.decay.enabled`
  - Disabled in test profile

- **Spot Update Endpoint:**
  - PUT /api/v1/spots/{id} — only creator can update
  - Updatable: name, prices, booking, estimated spots, notes, schedules
  - Immutable: location, type
  - UnauthorizedSpotModificationException (403) if non-creator attempts

- **Abuse/Spam Detection:**
  - 30-minute cooldown per user per spot
  - ReportCooldownException (429) when violated

- **Report Weighting by Reputation:**
  - Summary aggregation weighted by user reputation
  - Weight formula: 1.0 + (reputationScore / 100.0)
  - Applied to: dominant availability, avg price, avg safety

- **Search Filters + Pagination:**
  - Custom repository (ParkingSpotRepositoryCustomImpl) with dynamic native SQL
  - Optional filters: type, maxPrice, requiresBooking, minTrustScore
  - Pagination (page/size) on: search, getMySpots, getReportsForSpot, getFavorites

- **Community-Driven Spot Removal:**
  - Any user can request removal (POST /spots/{id}/removal-requests)
  - Other users confirm (POST .../confirm) — can't confirm own request
  - After 3 confirmations: spot deactivated (active=false)
  - Entities: SpotRemovalRequest, SpotRemovalConfirmation
  - Migration V7: two new tables with indexes

- **Favorites/Bookmarks:**
  - POST /spots/{id}/favorite, DELETE /spots/{id}/favorite, GET /users/me/favorites
  - UserFavorite entity, migration V8
  - Paginated favorites list

- **Password Change:**
  - PUT /users/me/password — verifies current password, encodes new one

- **Admin Endpoints:**
  - PUT /admin/spots/{id}/deactivate, DELETE /admin/reports/{id}
  - PUT /admin/users/{id}/ban, PUT /admin/users/{id}/unban
  - Role-based access: ADMIN role required
  - SecurityConfig updated with hasRole("ADMIN")

- **Test coverage:** 51 → 101 tests (50 new)
- **Migrations:** V6 (active flag), V7 (removal system), V8 (favorites)
- **i18n:** 17 new message keys in EN and PT

### Session 3 (2026-04-07)

Five remaining features implemented:

- **Password Reset (Forgot Password):**
  - Spring Mail + SMTP integration
  - Token-based flow: request reset -> email with token link -> validate token + new password
  - 1-hour token expiry, single-use, no email enumeration
  - POST /auth/forgot-password, POST /auth/reset-password (public)
  - PasswordResetToken entity, migration V9

- **Reverse Geocoding (Nominatim):**
  - Interface-based design (GeocodingService interface, NominatimGeocodingService impl)
  - Converts lat/lng to human-readable address on spot creation
  - address field on ParkingSpot + SpotResponse + SpotSummaryResponse
  - @ConditionalOnProperty for easy swap to Google Maps in prod
  - Migration V10

- **Gamification System (fully implemented from design):**
  - Points: +5 per report, +3 GPS bonus (<100m), +1-7 streak bonus
  - 10 badges (FIRST_STEPS through COMMUNITY_GUARDIAN) with bonus points
  - Streak tracking (consecutive days, longest streak)
  - GamificationService hooks into report submission + spot creation
  - Popular spot bonus (+10) when spot reaches 10 confirmations
  - GET /users/me/gamification, GET /users/{id}/gamification
  - Entities: UserPoints, UserBadge, UserStreak. Migration V11

- **SpotAnalytics (Precomputed Heatmap):**
  - Aggregated parking patterns by day-of-week and hour (0-23)
  - Availability rate, avg price, avg safety, informal charge rate per time bucket
  - Daily 3 AM scheduled job recomputes for all active spots
  - GET /spots/{spotId}/analytics — returns heatmap-style data
  - SpotAnalytics entity, migration V12

- **Image Attachments on Reports:**
  - Interface-based storage (ImageStorageService, LocalImageStorageService)
  - Up to 3 images per report, max 5MB each, JPEG/PNG/WebP only
  - Report endpoint now supports both multipart (with images) and JSON (without)
  - GET /images/{filename} (public) serves stored images
  - ReportImage entity, migration V13
  - Ready for S3 swap in production

- **Dependencies added:** spring-boot-starter-mail
- **Test coverage:** 112 -> 139 tests (27 new)
- **Migrations:** V9 (reset tokens), V10 (address), V11 (gamification), V12 (analytics), V13 (images)
- **i18n:** 12 new message keys in EN and PT
- **Prod notes:** Image storage (local) and geocoding (Nominatim) designed as interfaces for easy swap to S3 and Google Maps

## Gamification System (Implemented)

### Points System
- Submit a report: +5 points
- GPS-verified report (within 100m): +3 bonus points
- Report that aligns with majority: +2 points
- Create a spot that gets 10+ confirmations: +10 points
- First to report a new spot's status each day: +3 points
- Streak bonus: +1 point per consecutive day with a report (caps at +7)

### Badges
| Badge | Requirement | Points Bonus |
|-------|------------|-------------|
| First Steps | Submit first report | 0 |
| Regular | 10 reports submitted | 5 |
| Veteran | 50 reports submitted | 20 |
| Centurion | 100 reports submitted | 50 |
| Spot Discoverer | Create first spot | 0 |
| Cartographer | Create 10 spots | 10 |
| Reliable | 20 consecutive accurate reports | 15 |
| Night Owl | 10 reports between 22:00-06:00 | 5 |
| Early Bird | 10 reports between 06:00-09:00 | 5 |
| Community Guardian | Participate in 5 removal confirmations | 10 |

### Streaks
- Track consecutive days with at least one report
- Streak milestones: 3 days, 7 days, 14 days, 30 days
- Breaking a streak resets counter to 0
- Streak displayed on user profile

### Leaderboards
- Weekly leaderboard by area (geohash-based regions)
- Monthly global leaderboard
- Categories: Most Reports, Highest Accuracy, Longest Streak
- Top 10 displayed per category

### Session 4 (2026-04-07)

Four API maturity features implemented:

- **Leaderboards (fully implemented):**
  - Weekly and monthly leaderboards with categories: MOST_POINTS, LONGEST_STREAK
  - LeaderboardEntry entity, migration V14
  - LeaderboardJob: weekly (Monday 1 AM), monthly (1st 2 AM) — computes leaderboards then resets points
  - GET /api/v1/leaderboards?period=WEEKLY&category=MOST_POINTS
  - Point reset: weeklyPoints zeroed every Monday, monthlyPoints every 1st

- **OpenAPI/Swagger Documentation:**
  - springdoc-openapi-starter-webmvc-ui integrated
  - /swagger-ui accessible without auth
  - All controllers tagged (@Tag) for organized API docs
  - JWT security scheme defined

- **Rate Limiting (Bucket4j):**
  - Per-user rate limiting: 100 requests/minute (configurable)
  - Keyed by JWT token prefix or IP for unauthenticated requests
  - Skips /swagger-ui, /v3/api-docs, /images endpoints
  - Returns 429 when exceeded

- **Search by Name/Address:**
  - `query` parameter added to GET /api/v1/spots search
  - ILIKE search on name and address columns (case-insensitive, partial match)
  - No migration needed — uses existing columns

- **Dependencies added:** springdoc-openapi-starter-webmvc-ui 2.8.6, bucket4j_jdk17-core 8.14.0
- **Test coverage:** 139 -> 148 tests
- **Migration:** V14 (leaderboard_entries)

### Session 5 (2026-04-08)

Frontend UX improvements and feature additions:

- **Google OAuth:** Backend service + frontend Google Sign-In button on login page
- **Navigate to spot:** "Navegar" button opens Google Maps directions from popup and detail page
- **Price on map pins:** Price labels ("R$5-15" or "Grátis") below each marker
- **Quick report:** Floating 📍 button finds nearest spot, opens bottom-sheet report modal
- **List view:** Map/Lista toggle shows spots sorted by distance with SpotCard components
- **User preferences:** Backend entity + endpoints, profile page UI for default filters, auto-applied on app load
- **Availability rate:** Backend calculates availabilityRate in spot summary
- **Nickname + profile pic:** Backend fields + upload endpoint, avatar in navbar
- **Filter panel:** Trust level, spot type, free only checkboxes with legend
- **Heart favorite:** Toggle with ❤️/🤍, auto-detects existing favorites
- **i18n:** Portuguese as default language for UI
- **@Transactional fixes:** All read services now have readOnly=true to prevent lazy loading issues

- **Backend:** 153 tests, V1-V4 migrations (consolidated from V1-V14)
- **Frontend:** Next.js 15, 11 routes, full Portuguese UI

### Session 6 (2026-04-09 — 2026-04-10)

Deployment to Render, flanelinha feature, admin dashboard, email verification, and comprehensive UX polish.

- **Render Deployment:**
  - Backend + frontend deployed to Render free tier (port 10000, Docker)
  - PostgreSQL on Render with Flyway baseline migration
  - Frontend Docker: NEXT_PUBLIC_* vars as build ARGs, HOSTNAME=0.0.0.0 binding
  - Cold start handling (~2 min backend) with login spinner

- **Flanelinha (Informal Charge) as First-Class Feature:**
  - Spot-level `informalChargeFrequency` field (UNKNOWN/NEVER/SOMETIMES/OFTEN/ALWAYS)
  - Report-level fields: type, amount, aggressiveness (1-5), note
  - Map indicators: red ⚠ badge on pins for OFTEN/ALWAYS
  - Filter: "Sem cobranca informal" checkbox
  - Map legend: informal charge indicator added
  - Migrations V6 (report fields) + V7 (spot frequency)

- **Email Verification System:**
  - Backend: token-based, 24h expiry, EmailVerificationService
  - Frontend: /verify-email page with success/error states
  - Unverified banner below navbar with "Resend" button
  - Migration V5 (email_verification_tokens table)

- **Admin Dashboard Completion:**
  - GET /admin/users, /admin/spots, /admin/reports (paginated) + /admin/stats
  - Frontend admin page with stats cards, tabbed tables, ban/deactivate/delete actions

- **Anonymous Access:**
  - Map, spots, leaderboards accessible without login
  - Action buttons visible but redirect to /register when clicked
  - SecurityConfig: public GET on /spots/** and /leaderboards/**

- **Frontend UX Improvements:**
  - Password reset page (/reset-password?token=...)
  - Spot edit page (owner only)
  - Report image upload (up to 3 images, thumbnails in report cards)
  - Google OAuth on both login AND register pages
  - Login spinner + disabled inputs during authentication
  - Availability probability: shown on spot detail summary (color-coded percentage)
  - Mobile bottom sheet: slide-up panel on marker tap (md:hidden)
  - Analytics heatmap: visual grid replacing flat table (day x hour, color-coded availability)
  - i18n fix: navigator.languages check for Portuguese detection
  - Address cleanup: filters out "Regiao Geografica" noise from Nominatim
  - Distance badge moved inside SpotCard as prop
  - Safety rating: shows "5/5" instead of "5.0/5"

- **Test Coverage:** 167 -> 190 tests
  - New: EmailVerificationServiceTest (6 tests)
  - Extended: AdminServiceTest (+4), AdminControllerTest (+9), AuthControllerTest (+4)

- **Postman Collection:** 39 E2E steps (was 33)
  - 6 new endpoints added (verify-email, resend-verification, admin list/stats)
  - Report submission bodies updated with informal charge fields
  - E2E step 11 validates informal charge response fields

- **Migrations:** V5 (email verification), V6 (informal charge report fields), V7 (informal charge spot frequency)
- **Frontend:** 15 routes, full Portuguese UI with email verification and mobile bottom sheet
