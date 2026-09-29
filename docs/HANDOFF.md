# HANDOFF STATUS

## MILESTONE 2 — JJWT → Nimbus (latest)
**Done (code only):** pom.xml (jjwt-* + `jjwt.version` removed, `com.nimbusds:nimbus-jose-jwt` added without version → managed by Boot),
`services/JwtService` rewritten with Nimbus (JWTClaimsSet/JWSHeader/SignedJWT/MACSigner/MACVerifier, alg forced to HS256, same public methods; `extractClaim` now takes `Function<JWTClaimsSet,T>`; `isTokenExpired` now public),
new `exception/InvalidJwtException` (reasons → 401), `GlobalExceptionHandler` (4 JJWT handlers → 1), `AuthFlowIntegrationTests` (ported + 3 new tests), docs + `docs/TECHNOLOGY_DECISIONS.md`.
Filter, SecurityConfiguration, controllers, frontend, User, API contract: unchanged.
**Not done / unverified:** NOTHING compiled or run (no Maven, network disabled in sandbox). Not committed to git.
**Risks to check first:** `nimbus-jose-jwt` version really managed by Boot 4.1.1 (else add `<version>`); Nimbus API names (`getIssueTime`, `getStringListClaim`); `MACSigner` needs HS512 key >= 64 bytes in test `unsupportedAlgorithmHs512Is401`.
**Next:** `./mvnw clean verify`; `./mvnw dependency:tree | grep -i -E "nimbus|jsonwebtoken"`; fix errors; update TESTING.md; then
`git add -A && git commit -m "refactor(auth): replace jjwt with nimbus"` (and `test(auth): verify nimbus jwt authentication` after green).

---
# (Previous handoff below)

## Current Phase
Phase 4-8 code complete. Phase 9 (build + test) **BLOCKED in the authoring sandbox**; must be run on a machine with Maven Central + JDK 17+.

## Completed
All JWT core, security config, controllers, exception handling, frontend demo, tests (written), docs. See PROGRESS.md.

## In Progress
Nothing partially written.

## Remaining
1. `./mvnw clean verify` and fix any compile/API mismatch (code was NEVER compiled — only parse-checked with javac; no dependency resolution).
2. Manual run against MySQL + browser check of login/profile/logout.
3. Update TESTING.md "Actual/Status" and PROGRESS.md with real results.

## Files Created
pom deps aside — `src/main/java/vn/iotstar/btweb10/`: entity/User, models/{LoginResponse,LoginUserModel,RegisterUserModel,UserResponse}, repository/UserRepository,
services/{UserService,AuthenticationService,JwtService}, configs/{ApplicationConfiguration,SecurityConfiguration,RestAuthenticationEntryPoint,RestAccessDeniedHandler},
filter/JwtAuthenticationFilter, controllers/{AuthenticationController,UserController,PageController}, exception/{GlobalExceptionHandler,ApiError,ErrorResponseWriter,EmailAlreadyExistsException};
resources: templates/{login,profile}.html, static/{mainjs.js,css/app.css}; test: AuthFlowIntegrationTests, src/test/resources/application.properties (H2 + throw-away test secret); `.env.example`; `docs/*`.

## Files Modified
pom.xml (deps), src/main/resources/application.properties (env-driven), .gitignore (.env), (Btweb10ApplicationTests unchanged; now uses test properties).

## Current Architecture
See ARCHITECTURE.md. JwtAuthenticationFilter is instantiated inside SecurityConfiguration (not @Component) to avoid double registration.

## Known Issues / Risks (verify first when compiling)
- Boot 4 / Security 7 / Jackson 3 APIs could not be verified offline. Likely spots: `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` import in tests;
  `ResponseEntityExceptionHandler.handleExceptionInternal` signature; `DaoAuthenticationProvider(UserDetailsService)` ctor; `org.springframework.lang.NonNull` (may be JSpecify-deprecated — swap/remove if it fails);
  `@Qualifier("handlerExceptionResolver")` bean name; (JJWT removed in M2; Nimbus has no Jackson dependency.)
- `GET /users/**` requires ROLE_USER; token roles claim is informational, authorities are always reloaded from DB.
- Token stored in localStorage (per PDF) → XSS-readable. Consider HttpOnly cookie later.
- No refresh token / no server-side revocation (stateless logout).
- ddl-auto=update is for dev; use migrations for production.

## Tests Passed
NONE executed.

## Tests Blocked
All (see TESTING.md).

## Exact Next Step
Set env vars (see `.env.example`; for tests none are needed) and run `./mvnw clean verify`. Fix compile errors per Known Issues, rerun until green, then update TESTING.md/PROGRESS.md.

## Commands
- Generate secret: `openssl rand -base64 48`
- Build+test: `./mvnw clean verify`
- Run: `DB_PASSWORD=... JWT_SECRET=... ./mvnw spring-boot:run` (MySQL at localhost:3306, DB `btweb10` auto-created)
- Try: `curl -X POST localhost:8080/auth/signup -H 'Content-Type: application/json' -d '{"email":"a@x.com","password":"secret1","fullName":"A"}'`, then `/auth/login`, then `curl -H "Authorization: Bearer $T" localhost:8080/users/me`.

## Important Decisions
Keep Boot 4.1.1; package `vn.iotstar.btweb10.*`; DB `btweb10`; env vars DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET/JWT_EXPIRATION; ROLE_USER for new users; REST + frontend demo.
PDF discrepancies fixed: 30h hardcoded expiry → configured expiry; `/user/**` matcher → `/users/**`; signature/expired 403 → 401; malformed token 500 → 401; no-token 403 → 401 entry point;
CORS bean actually wired via `.cors()`; `images` nullable; password hash never serialised (UserResponse DTO); no stack-trace printing; no `@Transactional` on controllers; secret/DB password from PDF NOT used;
`AntPathRequestMatcher` replaced by string matchers; `/auth/**` skipped by JWT filter; email normalised to lowercase; extra: locked/enabled flags for 403 on AccountStatusException.
