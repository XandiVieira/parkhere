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
- [ ] Trust score calculation (basic version)

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
