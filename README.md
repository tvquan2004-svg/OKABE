# ⚡ OKABE — Task Manager

> Trello / Notion-style task management app built with **Spring Boot**, **React**, and **MySQL**.

---

## 📦 Tech Stack

| Layer          | Technology                                           |
| -------------- | ---------------------------------------------------- |
| Backend        | Java 21 · Spring Boot 3.4 · Spring Security · JWT |
| Frontend       | React 19 · TypeScript · Vite · Redux Toolkit      |
| Database       | MySQL 8.0 · Redis 7                                 |
| Infrastructure | Docker · Docker Compose · Nginx                    |

---

## ⚙️ Prerequisites

Đảm bảo máy đã cài đặt:

- [Java JDK 21+](https://adoptium.net/)
- [Maven 3.9+](https://maven.apache.org/)
- [Node.js 20+](https://nodejs.org/) & npm
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)

---

## 🚀 Khởi chạy dự án

### Cách 1 — Chạy từng phần (Development — Recommended)

#### Bước 1: Khởi động Database & Redis\

```bash
docker compose up mysql redis -d
```

Chờ ~10s để MySQL khởi động xong, kiểm tra:

```bash
docker compose ps
```

#### Bước 2: Chạy Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

> ⚠️ **PowerShell**: Phải đặt `-D...` trong dấu `""` để tránh lỗi parse.

> Backend chạy tại: **http://localhost:8080**
> Swagger UI: **http://localhost:8080/swagger-ui.html**
> Health check: **http://localhost:8080/api/v1/health**

#### Bước 3: Cài dependencies & Chạy Frontend (React)

```bash
cd frontend
npm install
npm run dev
```

> Frontend chạy tại: **http://localhost:5173**

---

### Cách 2 — Chạy tất cả bằng Docker Compose

```bash
# Build và chạy toàn bộ services
docker compose up --build -d

# Xem logs
docker compose logs -f
```

> Frontend: **http://localhost:3000**
> Backend API: **http://localhost:8080**
> MySQL: **localhost:3306**

---

## 🛑 Dừng dự án

```bash
# Dừng tất cả containers
docker compose down

# Dừng và xóa volumes (reset database)
docker compose down -v
```

---

## 🔧 Các lệnh hữu ích

### Backend

```bash
# Build JAR (skip tests)
cd backend
mvn package "-DskipTests"

# Chạy tests
mvn test "-Dspring.profiles.active=test"

# Clean build
mvn clean install
```

### Frontend

```bash
cd frontend

# Cài dependencies
npm install

# Dev server (hot reload)
npm run dev

# Build production
npm run build

# Chạy tests
npm run test

# Lint check
npm run lint
```

### Docker

```bash
# Chỉ khởi động DB
docker compose up mysql redis -d

# Rebuild 1 service
docker compose up --build backend -d

# Xem logs của 1 service
docker compose logs -f backend

# Truy cập MySQL CLI
docker exec -it okabe-mysql mysql -u okabe -p123456 okabe_db

# Truy cập Redis CLI
docker exec -it okabe-redis redis-cli
```

---

## 🌐 URLs khi chạy

| Service           | URL                                   |
| ----------------- | ------------------------------------- |
| Frontend (dev)    | http://localhost:5173                 |
| Frontend (docker) | http://localhost:3000                 |
| Backend API       | http://localhost:8080                 |
| Swagger UI        | http://localhost:8080/swagger-ui.html |
| Health Check      | http://localhost:8080/api/v1/health   |
| MySQL             | localhost:3306                        |
| Redis             | localhost:6379                        |

---

## 📁 Project Structure

```
okabe/
├── backend/          # Spring Boot API
├── frontend/         # React + Vite SPA
├── docker-compose.yml
├── .env.example
└── .gitignore
```

---

## 📄 License

MIT © 2026 OKABE
