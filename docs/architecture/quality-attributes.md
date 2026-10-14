# Instakill – Quality Attribute Assessment

The ratings describe the current monolith (`backend/`, `frontend/`, SQLite). Scale: 1 = poor coverage, 10 = excellent coverage. The architecture diagrams are in `workspace.dsl`.

| Attribute | Rating (1–10) | Justification |
|---|---|---|
| Performance | 5 | The trending feed sorts by `likeCount`, `commentCount` and `createdAt` in a single query (`JpaPostRepository`) without a matching index. The `size` parameter on `/api/feed` and comment listing is not capped, so one request can ask for an arbitrarily large page. Open-in-view is disabled, which avoids accidental lazy loading in the web layer. |
| Scalability | 3 | The backend is a single process with SQLite as its only datastore. SQLite allows one writer at a time and lives in a local file, so horizontal scaling is blocked by the database even though the JWT-based API is stateless. There is no cache, queue or read replica. |
| Reliability | 4 | There is no external service, so there are no network failure modes to handle, but everything runs in one process and one file. No backup, retry or circuit-breaker strategy exists. `ddl-auto: update` changes the schema automatically at startup, which carries risk for data. |
| Maintainability | 6 | Packages are split by feature (`post`, `feed`, `interaction`, `user`) with domain repository interfaces and JPA adapters in `infrastructure`, and the feed uses a strategy pattern. However, the feature packages depend on each other in both directions (`post` ↔ `interaction`, `feed` → `post` → `feed`), and the frontend is a single `App.vue` without a router. Test coverage is thin: 8 test methods in 4 classes. |
| Security | 4 | Passwords are hashed with BCrypt, and the JWT is stored in an HttpOnly cookie with `SameSite=Lax`. However, the JWT secret is committed in `application.yaml`, the cookie has no `Secure` flag, CSRF is disabled for `/api/**`, and there is no rate limiting on login. The only role is `ROLE_USER`, so there is no authorization model beyond "is logged in". |
| Usability | 5 | Both a Thymeleaf UI and a Vue SPA exist, which duplicates the user flows and makes it unclear which one is the primary interface. Pagination is available through `page` and `size` parameters, and the API has central error handling. This rating is partly outside the architecture, so it is the least certain. |

## Trade-offs and gaps

- **Monolith vs. extraction:** The package structure is prepared for splitting the system into services, but the cyclic dependencies between feature packages would have to be removed first.
- **SQLite:** It is simple to run locally and needs no extra container, which suits a starter project. It is the main obstacle to scaling and to running several backend instances.
- **Domain events:** `DomainEventPublisher` is currently a no-op. The extension point exists, but nothing consumes `LikeCreatedEvent` yet.
- **Admins and external services:** The current code has no admin role and no external integrations. Those are therefore not part of the context diagram.
