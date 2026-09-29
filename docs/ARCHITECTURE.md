# ARCHITECTURE

## Technology
JWT Library: Nimbus JOSE + JWT · Algorithm: HS256 (enforced; any other `alg`/`none` → 401) · Authentication: Spring Security (custom JwtAuthenticationFilter) ·
Password: BCrypt · Token transport: `Authorization: Bearer <token>`.
JwtService (Nimbus: JWTClaimsSet, JWSHeader, SignedJWT, MACSigner, MACVerifier) parses → checks alg == HS256 → verifies signature → checks sub/iat/exp → else `InvalidJwtException` (→ 401 via GlobalExceptionHandler).
Claims: `sub` (email), `iat`, `exp`, `roles` (e.g. `ROLE_USER`, informational; authorities are reloaded from DB).

## Package structure
```
vn.iotstar.btweb10
├── configs      ApplicationConfiguration, SecurityConfiguration, RestAuthenticationEntryPoint, RestAccessDeniedHandler
├── controllers  AuthenticationController (/auth), UserController (/users), PageController (/, /login, /profile)
├── entity       User (JPA + UserDetails)
├── exception    GlobalExceptionHandler, ApiError, ErrorResponseWriter, EmailAlreadyExistsException, InvalidJwtException
├── filter       JwtAuthenticationFilter (created in SecurityConfiguration, NOT a @Component)
├── models       LoginUserModel, RegisterUserModel, LoginResponse, UserResponse
├── repository   UserRepository
└── services     UserService, AuthenticationService, JwtService
```

## Authentication flow
Signup: POST /auth/signup → validate → AuthenticationService.signup (lowercase email, exists check, BCrypt, ROLE_USER) → save → 201 UserResponse.
Login: POST /auth/login → AuthenticationManager (DaoAuthenticationProvider + UserDetailsService + BCrypt) → JwtService.generateToken → LoginResponse{token, expiresIn}.

## Request flow (protected)
Request → JwtAuthenticationFilter (skips `/auth/**`; no Bearer header → continue unauthenticated) → Nimbus parse + HS256 check + signature verify + exp/sub/iat
→ load User by `sub` → isTokenValid → SecurityContextHolder → authorization rules → controller.
JWT error (InvalidJwtException) → HandlerExceptionResolver → GlobalExceptionHandler → 401 ApiError. No/invalid auth at rule level → RestAuthenticationEntryPoint 401;
forbidden → RestAccessDeniedHandler 403.

## Database
Single table `users`: id (identity), full_name, email (unique), images (nullable), password (BCrypt), role, enabled, locked, created_at, updated_at. No relations. Schema by `ddl-auto=update`.

## Frontend/backend flow
login.html → POST /auth/login → token in localStorage("token") → /profile → GET /users/me with Bearer → render via textContent.
Logout → remove token → /login. 401/403 on /users/me → token cleared → /login.
