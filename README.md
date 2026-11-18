# Instakill

Instakill is a Spring Boot 3 / Vue 3 starter for an Instagram-style social feed. The project starts as a monolith but is structured for future extraction into independent services.

## Project layout

- `backend/` – Spring Boot application exposing REST and MVC endpoints, backed by SQLite.
- `frontend/` – Vue 3 SPA that consumes the REST API.
- `scripts/` – helper tooling including Gradle bootstrap.

## First run

Make sure you have Java 23 installed. 

```bash
# From the project root
./scripts/bootstrap-gradle.sh
./backend/gradlew bootRun
```

On Windows PowerShell:

```powershell
./scripts/bootstrap-gradle.ps1
./backend/gradlew.bat bootRun
```

The bootstrap downloads Gradle 8.9 and generates a local wrapper (ignored by git). Subsequent builds can use the wrapper directly.

## Useful endpoints

- Web feed: http://localhost:8080/feed
- Login page: http://localhost:8080/auth/login
- REST OpenAPI UI: http://localhost:8080/api/swagger
- Health: http://localhost:8080/actuator/health
- Make a post funnier (local LLM, login required): `POST http://localhost:8080/api/funnyfy` with `{"text": "..."}`

## Authentication

Authentication uses JWT stored in an `ACCESS_TOKEN` HttpOnly cookie. REST clients can authenticate by capturing the cookie returned from `/api/auth/login` or by sending the token manually.

Seed accounts (development profile):

| Username | Email             | Password |
|----------|-------------------|----------|
| alice    | alice@example.com | password |
| bob      | bob@example.com   | password |

## Local LLM ("Make It Funnier")

The "Make It Funnier" button in the post composer sends the caption to `POST /api/funnyfy`. The backend forwards it to a local LLM through the OpenAI-compatible `/v1/chat/completions` endpoint (for example LM Studio).

One-time setup:

```bash
# Install LM Studio (https://lmstudio.ai) and open it once, then:
~/.lmstudio/bin/lms bootstrap      # optional: puts `lms` on your PATH (open a new terminal afterwards)
lms get google/gemma-3-270m
```

Start the LLM:

```bash
lms server start --port 12345
lms load google/gemma-3-270m
curl http://localhost:12345/v1/models   # should list google/gemma-3-270m
```

Stop the LLM:

```bash
lms unload --all      # free the model from memory
lms server stop
```

`lms server status` shows whether the server is running.

You can set the connection with environment variables. The defaults are in `backend/src/main/resources/application.yaml`:

| Variable            | Default                                       |
|---------------------|-----------------------------------------------|
| `LOCAL_LLM_URL`     | `http://localhost:12345/v1/chat/completions`  |
| `LOCAL_LLM_MODEL`   | `google/gemma-3-270m`                         |
| `LOCAL_LLM_TIMEOUT` | `30s`                                         |

If the LLM server is down or times out, the endpoint responds with `503` and a `ProblemDetail` message, and the frontend shows that message under the button.

## Running tests

```bash
cd backend
./gradlew test
```

The tests cover feed ranking, like toggling, MVC flows, and an end-to-end REST happy-path.

## Frontend

The Vue application lives in `frontend/`. Install dependencies and start the dev server with:

```bash
cd frontend
npm install
npm run dev
```

With default settings, you will be able to access the frontend on `http://localhost:5173/` 
It assumes the backend is running at `http://localhost:8080`.
Check `vite.config.js` for these settings.

## Architecture

The architecture documentation lives in `docs/architecture/`:

- `workspace.dsl`: C4 model (Context, Container, Component) in Structurizr DSL.
- `diagrams/`: rendered PNG diagrams and their PlantUML sources.
- `quality-attributes.md`: assessment of the monolith's quality attributes, with trade-offs and gaps.
