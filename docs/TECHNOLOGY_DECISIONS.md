# JWT Library Decision

## Previous
JJWT 0.12.6 (jjwt-api / jjwt-impl / jjwt-jackson)

## Current
Nimbus JOSE + JWT (`com.nimbusds:nimbus-jose-jwt`, version managed by Spring Boot's dependency management)

## Reason
* Nimbus is the JOSE/JWT library used internally by Spring Security (spring-security-oauth2-jose / NimbusJwtDecoder).
* Supports signing, parsing and signature verification (JWTClaimsSet, SignedJWT, MACSigner, MACVerifier).
* Removes the jjwt-jackson (Jackson 2) runtime dependency that sat beside Boot 4's Jackson 3.
* Existing architecture (custom JwtAuthenticationFilter + UserDetailsService) is kept; only the JWT implementation changed.

## Algorithm
HS256, enforced server-side: header `alg` must equal HS256, otherwise 401 (covers `none`, HS384/512, RS256...).

## Token Validation
Nimbus parse + MACVerifier; then required claims (`sub`, `iat`, `exp`) and expiry checked on Nimbus' JWTClaimsSet. All failures → InvalidJwtException → 401.

## Secret Management
Environment variables: `JWT_SECRET` (Base64, decodes to >= 32 bytes), read through `security.jwt.secret-key`. No secret in source.

## Expiration
Configurable: `JWT_EXPIRATION` (ms) → `security.jwt.expiration-time`; the only source for both `expiresIn` and the token's `exp`.
