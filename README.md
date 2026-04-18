# OKABE - Task Manager

Trello / Notion-style task management app built with Spring Boot, React, MySQL, and Redis.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 3.4, Spring Security, JWT |
| Frontend | React, TypeScript, Vite, Redux Toolkit |
| Database | MySQL 8, Redis 7 |
| Infra | Docker, Docker Compose, Nginx |

## Prerequisites

Make sure these tools are installed:

- Java JDK 21+
- Maven 3.9+
- Node.js 20+ and npm
- Docker Desktop

## Run The Project

### Option 1: Run each part locally

1. Start MySQL and Redis:

```bash
docker compose up mysql redis -d
docker compose ps
```

2. Run backend:

```bash
cd backend
mvn spring-boot:run
```

Backend URLs:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health check: `http://localhost:8080/api/v1/health`

Note:

- The project now defaults to the `dev` profile, so `mvn spring-boot:run` is enough.
- If MySQL or Redis is not running, backend startup will fail.

3. Run frontend:

```bash
cd frontend
npm install
npm run dev
```

Frontend URL:

- App: `http://localhost:5173`

### Option 2: Run everything with Docker Compose

```bash
docker compose up --build -d
docker compose logs -f
```

Docker URLs:

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080`
- MySQL: `localhost:3306`
- Redis: `localhost:6379`

## Stop The Project

```bash
docker compose down
```

Reset database volumes:

```bash
docker compose down -v
```

## Useful Commands

### Backend

```bash
cd backend

# Run app
mvn spring-boot:run

# Build jar without tests
mvn package -DskipTests

# Run tests
mvn test -Dspring.profiles.active=test

# Clean build
mvn clean install
```

### Frontend

```bash
cd frontend

# Install dependencies
npm install

# Run dev server
npm run dev

# Build production bundle
npm run build

# Run tests
npm run test

# Run lint
npm run lint
```

### Docker

```bash
# Start only database services
docker compose up mysql redis -d

# Rebuild one service
docker compose up --build backend -d

# View backend logs
docker compose logs -f backend

# Open MySQL shell
docker exec -it okabe-mysql mysql -u okabe -p123456 okabe_db

# Open Redis shell
docker exec -it okabe-redis redis-cli
```

## Project Structure

```text
okabe/
|-- backend/
|-- frontend/
|-- docker-compose.yml
|-- .env
|-- .env.example
`-- README.md
```

## License

MIT Copyright 2026 OKABE
