# PROJECT PLAN — BTWEB10 JWT Authentication

## Goal
Add stateless JWT authentication/authorization to the (initially empty) Spring Boot project, adapting the
"Json Web Token" course PDF (Spring Boot 3 / Security 6) to **Spring Boot 4.1.1 / Spring Security 7**, and fixing the
PDF's discrepancies rather than copying them.

## Technology
Java 17 (pom) · Spring Boot 4.1.1 · Spring Security · Spring Data JPA (Hibernate) · MySQL (runtime) · H2 (tests only) ·
Nimbus JOSE + JWT (`com.nimbusds:nimbus-jose-jwt`, version managed by Boot; replaced JJWT) · Lombok · Thymeleaf (HTML shells) · vanilla JS frontend.

## Modules (package `vn.iotstar.btweb10`)
`entity` User · `models` DTOs · `repository` UserRepository · `services` UserService / AuthenticationService / JwtService ·
`configs` ApplicationConfiguration / SecurityConfiguration / RestAuthenticationEntryPoint / RestAccessDeniedHandler ·
`filter` JwtAuthenticationFilter · `controllers` Authentication/User/Page controllers · `exception` GlobalExceptionHandler, ApiError, ErrorResponseWriter.

## JWT architecture
HS256, secret from `JWT_SECRET` (Base64, >= 32 bytes decoded, validated at startup), lifetime from `JWT_EXPIRATION` (ms) — no hardcoded
lifetime. Claims: `sub`=email, `iat`, `exp`, `roles`. Client sends `Authorization: Bearer <jwt>`. Logout = client deletes token.

## API / Database / Frontend / Security
See `API.md`, `ARCHITECTURE.md`. Table `users` (unique email, BCrypt password, role default ROLE_USER, nullable images).
Frontend: `login.html` (login + signup tabs), `profile.html`, `static/mainjs.js`. Security: STATELESS, CSRF off (no cookies), custom 401/403 JSON,
JWT filter before UsernamePasswordAuthenticationFilter, `/users/**` needs ROLE_USER, `/auth/**` + page shells + static public, everything else authenticated.

## Phases
1 Scan · 2-3 Mapping · 4 Implement (steps 1-11) · 5-8 Flow/Security/Exceptions/PDF review · 9 Test · 10 Report. Status in `PROGRESS.md`.

## Test plan
See `TESTING.md` (17 MockMvc integration tests + manual checklist for frontend).
