# Week 6: Load Testing Instakill with Locust

## Setup

- Load generator: Locust 2.47.0, `backend/locustfile.py`, each simulated user logs in as `alice` and loops over feed reads (`/api/feed` with no sort, `sort=trending`, `sort=new`) and like toggles (`POST /api/posts/:id/likes/toggle`).
- Test ramp: 1, 2, 5, 10 users, spawn rate 1 user/s, 90 s per run, statistics reset before each run.
- Dataset: generated with `backend/populate_db.py` using default parameters (10,000 users, ~150k posts, ~750k comments, ~1.5M likes). The SQLite and PostgreSQL datasets have the same size, but they are generated independently and are not identical.
- Backend: Instakill, Spring Boot 3.3.2, started with `./gradlew bootRun`. Port 8082 was used because ports 8080 and 8081 were occupied by an unrelated process.
- SQLite: default profile (`backend/instakill.db`).
- PostgreSQL 16 in Docker (`postgres:16`), Spring profile `postgres` (`backend/application-postgres.yaml`).

## Results

| Users | DB | Requests | Failures | Avg (ms) | Median (ms) | p95 (ms) | RPS | Backend CPU avg / max | Backend RSS max |
|---|---|---|---|---|---|---|---|---|---|
| 1 | SQLite | 37 | 0 (0.0 %) | 1032 | 1300 | 2000 | 0.41 | 28 % / 94 % | 148 MB |
| 2 | SQLite | 63 | 1 (1.6 %) | 1347 | 1500 | 2600 | 0.71 | 83 % / 181 % | 168 MB |
| 5 | SQLite | 148 | 6 (4.1 %) | 1534 | 1800 | 2400 | 1.68 | 234 % / 450 % | 235 MB |
| 10 | SQLite | 186 | 13 (7.0 %) | 3269 | 3900 | 5200 | 2.08 | 549 % / 707 % | 287 MB |
| 1 | PostgreSQL | 58 | 0 (0.0 %) | 126 | 38 | 720 | 0.64 | 1 % / 3 % | 155 MB |
| 2 | PostgreSQL | 115 | 0 (0.0 %) | 88 | 32 | 220 | 1.27 | 0 % / 3 % | 90 MB |
| 5 | PostgreSQL | 292 | 0 (0.0 %) | 47 | 34 | 98 | 3.21 | 2 % / 6 % | 235 MB |
| 10 | PostgreSQL | 582 | 0 (0.0 %) | 64 | 38 | 190 | 6.39 | 5 % / 17 % | 153 MB |

Notes on the measurements:

- The request counts differ between runs because the number of requests in a 90 s window depends on response time and the random behaviour of the users.
- CPU and RSS were sampled every 5 s with `ps` for the Instakill JVM only. The PostgreSQL server runs in its own container and its CPU was **not** measured, so the PostgreSQL CPU values understate the total load on the machine.
- The SQLite run with 1 user shows high response times already, because the feed queries run against a 1.5M-row likes table with per-post like lookups.
- Screenshots of the Locust *Statistics* and *Charts* pages for the 10-user run are in `screenshots/`.
- The SQLite dataset was regenerated during the work, see "Data note" below.

## Data note

The original `backend/instakill.db` (527 MB, generated earlier with `populate_db.py`) was found replaced by a 57 KB file containing only the seed data during this work. The cause is not known. The SQLite results above were measured on the original file, before this happened. The SQLite screenshots were taken after regenerating the database with the original SQLite version of `populate_db.py` (default parameters, 10,000 users, 150k posts, 748k comments, 1.5M likes). That dataset is statistically equivalent but not identical to the original.

## Failures with SQLite

All failed requests were `POST /api/posts/:id/likes/toggle` with HTTP 500. The backend log shows `[SQLITE_BUSY] The database file is locked`: SQLite allows only one writer at a time, so concurrent like writes fail once the lock cannot be acquired in time. The first failure appeared with 2 concurrent users.

## Failures with PostgreSQL

No failures in any run.

## Analysis

**Scaling with concurrent users.** With SQLite, response time grows steadily with load (avg 1.0 s at 1 user, 3.3 s at 10 users), errors appear from 2 users onwards, and backend CPU rises to about 7 cores at 10 users. With PostgreSQL, average response time stays between 47 and 126 ms and throughput scales close to linearly from 0.64 to 6.39 RPS. The first run is affected by warm-up (JIT, connection pool), which explains the higher 1-user average and p95.

**Impact of switching to PostgreSQL.** Average latency drops by roughly an order of magnitude at equal load (e.g. 3269 ms → 64 ms at 10 users), throughput is about three times higher at 10 users (2.08 → 6.39 RPS), and the failure rate goes from 7 % to 0 %.

**Possible reasons.**
- SQLite serialises all writes through a single database-level lock. Like toggles therefore queue up and time out (`SQLITE_BUSY`), which becomes HTTP 500s. PostgreSQL uses row-level locking and MVCC, so readers and writers do not block each other.
- The SQLite database file is accessed from one JVM process with a single connection for writes; the query planner and page cache of SQLite are less suited to a 1.5M-row table than PostgreSQL's indexes (`idx_likes_post_user`, `idx_posts_created_at`).
- The much lower backend CPU on PostgreSQL is partly explained by the work moving into the database process, which was not measured here.
- Observed application issue (not fixed here): under concurrent toggles of the same post by the same user, the toggle can attempt a duplicate insert and fail with a unique-constraint violation (`idx_likes_post_user`). This surfaced as HTTP 500 in a separate threaded test against PostgreSQL (46 of 400 toggles), but not in the Locust runs.

## Open items

- The PostgreSQL database-side CPU/memory utilisation (e.g. `docker stats instakill-postgres`) has not been recorded.
- Submission to GitHub Classroom with this analysis, the screenshots and the repository URL is still to be done.
