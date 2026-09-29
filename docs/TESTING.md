# TESTING

**Nothing has been executed yet.** The sandbox blocks Maven Central, has no Maven and no MySQL, so every "Actual" below is NOT RUN.
Run locally: `./mvnw clean verify` (tests use in-memory H2 + `src/test/resources/application.properties`).

| # | Test (AuthFlowIntegrationTests unless noted) | Expected | Actual | Status |
|---|---|---|---|---|
| 1 | Btweb10ApplicationTests.contextLoads | context starts | — | NOT RUN |
| 2 | signupSucceedsStoresBcryptAndHidesPassword | 201, no password in JSON, DB hash starts `$2` | — | NOT RUN |
| 3 | signupDuplicateEmailIs409 | 409 | — | NOT RUN |
| 4 | signupInvalidBodyIs400 | 400 | — | NOT RUN |
| 5 | loginValidReturnsTokenAndExpiresIn | 200, expiresIn=3600000 | — | NOT RUN |
| 6 | tokenExpiryMatchesConfiguration | exp-iat == configured (±1s) | — | NOT RUN |
| 7 | loginWrongPasswordIs401 | 401 | — | NOT RUN |
| 8 | loginUnknownUserIs401 | 401 | — | NOT RUN |
| 9 | lockedAccountLoginIs403 | 403 | — | NOT RUN |
| 10 | protectedWithoutTokenIs401 | 401 ApiError | — | NOT RUN |
| 11 | validTokenGetsMeAndUsersWithoutPassword | 200, no password | — | NOT RUN |
| 12 | wrongSignatureIs401 | 401 "signature" | — | NOT RUN |
| 13 | malformedTokenIs401 | 401 | — | NOT RUN |
| 14 | expiredTokenIs401 | 401 "expired" | — | NOT RUN |
| 15 | tokenOfDeletedUserIs401 | 401 | — | NOT RUN |
| 16 | userWithoutRequiredRoleIs403 | 403 | — | NOT RUN |
| 17 | staleTokenDoesNotBlockLogin | 200 | — | NOT RUN |
| 18 | loginPageIsPublic / unknownPathWithoutTokenIs401NotPublic | 200 / 401 | — | NOT RUN |
| 19 | Frontend: login → profile → logout (manual, browser) | profile shown; logout clears token and returns to /login | — | NOT TESTED |
| 20 | Manual: MySQL schema created by JPA, `users.password` is BCrypt | yes | — | NOT TESTED |

## Milestone 2 (Nimbus) — added/changed tests (all NOT RUN)
| # | Test | Expected | Status |
|---|---|---|---|
| 21 | generatedTokenHasThreePartsHs256AndClaims | 3 parts, alg HS256, typ JWT, sub, exp, roles=[ROLE_USER] | NOT RUN |
| 22 | unsupportedAlgorithmHs512Is401 | 401 "algorithm" | NOT RUN |
| 23 | algNoneTokenIs401 | 401 | NOT RUN |
| 12 | wrongSignatureIs401 (now forged with Nimbus) | 401 "signature" | NOT RUN |
| 6 | tokenExpiryMatchesConfiguration (uses `getIssueTime`) | exp-iat == configured (±1s) | NOT RUN |
