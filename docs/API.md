# API

Error body (all errors): `{"timestamp","status","error","message","path"}`.

| Method | Endpoint | Auth | Request | Response |
|---|---|---|---|---|
| POST | /auth/signup | public | `{email, password(6-72), fullName, images?}` | 201 `UserResponse`; 400 validation; 409 duplicate email |
| POST | /auth/login | public | `{email, password}` | 200 `{token, expiresIn(ms)}`; 401 bad credentials; 403 locked/disabled |
| GET | /users/me | Bearer JWT, ROLE_USER | — | 200 `UserResponse`; 401 no/invalid/expired token; 403 missing role |
| GET | /users | Bearer JWT, ROLE_USER | — | 200 `UserResponse[]`; 401; 403 |
| GET | /, /login, /profile | public | — | HTML shells (no user data) |
| GET | /mainjs.js, /css/**, /js/**, /images/** | public | — | static |

`UserResponse` = `{id, fullName, email, images, role, createdAt, updatedAt}` — never contains password.

401 for JWT problems: malformed, invalid signature, expired, unsupported algorithm (alg != HS256 / none), missing claims. Messages are generic (no token content).

Example: `curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/users/me`
