# ParkHere - Project Conventions

## Overview
ParkHere is a collaborative parking spot discovery app — "Waze for parking".
Users discover where to park, how much it costs, the probability of finding a spot,
and how reliable that information is. Built with Java 21 + Spring Boot 4.

## Tech Stack
- Java 21, Spring Boot 4.0.5, Maven
- PostgreSQL + PostGIS (geospatial queries via Hibernate Spatial)
- Flyway (database migrations)
- Spring Security + JWT (authentication)
- Lombok (boilerplate reduction)
- SLF4J (logging)

## Code Style

### General
- Use `var` for local variables
- Prefer functional style (streams, lambdas, Optional chaining) when reasonable
- Minimal comments — only when logic is non-obvious
- No unnecessary abstractions or premature generalization

### Logging
- Use SLF4J (`org.slf4j.Logger` / `org.slf4j.LoggerFactory`) via `@Slf4j` Lombok annotation
- Every feature must have relevant log entries (info for business events, debug for flow, warn/error for failures)
- Use parameterized messages: `log.info("Spot {} confirmed by user {}", spotId, userId)`

### Testing
- All new code must be covered by relevant unit tests
- Tests live in `src/test/java` mirroring the main package structure
- Use JUnit 5 + Mockito for unit tests
- Use `@DataJpaTest` for repository tests, `@WebMvcTest` for controller tests

### API & Postman
- A Postman collection is maintained at `postman/parkhere.postman_collection.json`
- Every endpoint change (create, update, remove) must update the Postman collection
- REST endpoints follow standard conventions: plural nouns, proper HTTP methods

### Database
- Flyway migrations in `src/main/resources/db/migration`
- Migration naming: `V{number}__{description}.sql`
- Never modify existing migrations — create new ones

### Git & Commits
- **Atomic commits** — each commit should represent one logical change (one feature, one fix, one refactor)
- Do not bundle unrelated changes in the same commit
- **Never mention Claude, AI, or any co-author in commit messages** — no `Co-Authored-By` lines, no references to AI assistance
- This is a personal project on a professional MacBook — git user is configured locally per-repo to avoid mixing accounts
- Local config: `user.name = Alexandre Vieira`, `user.email = xandivieira@gmail.com`
- Remote: `https://github.com/XandiVieira/parkhere.git`
- Never touch the global git config

## Project Documentation
- `HELP.md` is the project development log — used to preserve context across Claude sessions
- Update HELP.md when making significant progress, decisions, or architectural changes

## Build & Run
```bash
./mvnw spring-boot:run        # run the app
./mvnw test                    # run tests
./mvnw clean package           # build jar
```
