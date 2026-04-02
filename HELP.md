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
- [ ] User registration and authentication (JWT)
- [ ] Add parking spot (with type, location, price range)
- [ ] Search spots by radius (geospatial)
- [ ] Submit confirmation/report on a spot
- [ ] Trust score calculation (basic version)
- [ ] Spot detail view with aggregated info

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
