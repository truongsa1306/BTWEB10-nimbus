# PROGRESS

Status: TODO / IN_PROGRESS / DONE / BLOCKED

| Phase | Task | Status | Files | Test |
|---|---|---|---|---|
| 1 | Scan project (blank Initializr skeleton, Boot 4.1.1) | DONE | — | n/a |
| 2-3 | PDF→project mapping + discrepancy list | DONE | HANDOFF.md (section Important Decisions) | n/a |
| 4.1 | pom.xml deps (security, jpa, thymeleaf, test deps) | DONE | pom.xml | BLOCKED (no Maven) |
| 4.1 | application.properties from env vars, `.env.example`, `.gitignore` | DONE | application.properties, .env.example, .gitignore | BLOCKED |
| 4.2 | User entity (UserDetails, ROLE_USER, nullable images) | DONE | entity/User.java | BLOCKED |
| 4.3 | Models + validation + UserResponse (no password) | DONE | models/* | BLOCKED |
| 4.4 | UserRepository | DONE | repository/UserRepository.java | BLOCKED |
| 4.5 | UserService, AuthenticationService, JwtService | DONE | services/* | BLOCKED |
| 4.6 | ApplicationConfiguration (Security 7 API) | DONE | configs/ApplicationConfiguration.java | BLOCKED |
| 4.7 | JwtAuthenticationFilter | DONE | filter/JwtAuthenticationFilter.java | BLOCKED |
| 4.8 | SecurityConfiguration + entry point + denied handler | DONE | configs/* | BLOCKED |
| 4.9 | Controllers | DONE | controllers/* | BLOCKED |
| 4.10 | Exception handling | DONE | exception/* | BLOCKED |
| 4.11 | Frontend demo | DONE | templates/login.html, profile.html, static/mainjs.js, static/css/app.css | NOT TESTED (manual) |
| 9 | Integration tests written | DONE | src/test/** | written, NOT RUN |
| 9 | Build + run tests | BLOCKED | — | sandbox blocks repo.maven.apache.org; no mvn/MySQL |
| 7 | Docs + CONTINUE_PROMPT | DONE | docs/* | n/a |
| 9 | **Run `./mvnw clean verify` locally and fix compile/API issues** | TODO | — | — |
| M2 | **Replace JJWT with Nimbus JOSE + JWT** (pom, JwtService, InvalidJwtException, GlobalExceptionHandler, tests) | DONE (code) | pom.xml, services/JwtService, exception/InvalidJwtException, exception/GlobalExceptionHandler, AuthFlowIntegrationTests | NOT RUN (no Maven, network disabled) |
| M2 | Tests added: alg HS512 → 401, alg none → 401, token structure (3 parts, HS256, typ JWT, sub, exp, roles) | DONE (written) | AuthFlowIntegrationTests | NOT RUN |
| M2 | docs/TECHNOLOGY_DECISIONS.md + doc updates | DONE | docs/* | n/a |
| M2 | **`./mvnw clean verify` + `./mvnw dependency:tree`** | TODO | — | — |
