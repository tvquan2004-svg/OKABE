# ⚡ OKABE - Modern Task Management & Collaboration Platform

> Nền tảng quản trị công việc toàn diện, kết hợp giữa hiệu năng mạnh mẽ của Spring Boot và trải nghiệm người dùng tinh tế của React.

OKABE là một ứng dụng quản lý tác vụ theo phong cách Kanban (tương tự Trello/Notion), được thiết kế để tối ưu hóa quy trình làm việc của đội nhóm thông qua giao diện trực quan, tính năng cộng tác thời gian thực và hệ thống phân quyền chặt chẽ.

---

## ✨ Tính năng nổi bật (Key Features)

* **⚡ Trải nghiệm Premium**: Giao diện Dark Mode hiện đại, hiệu ứng mượt mà và tốc độ phản hồi tức thì.
* **🔐 Bảo mật đa lớp**: Xác thực JWT, xác thực Email và Google OAuth 2.0 tích hợp thông minh.
* **📂 Quản lý Workspace**: Hỗ trợ nhiều không gian làm việc với hệ thống phân quyền OWNER/ADMIN/MEMBER.
* **📋 Bảng Kanban thông minh**: Kéo thả Board/List/Card linh hoạt, tùy chỉnh hình nền Board và gắn sao bảng quan trọng.
* **💬 Cộng tác Real-time**: Bình luận, nhắc tên (@mention) và nhận thông báo tức thời qua WebSocket.
* **📊 Dashboard Thống kê**: Theo dõi tổng quan tiến độ và hoạt động của tất cả các dự án.

---

## 🛠️ Hệ sinh thái Công nghệ (Tech Stack)

| Layer                    | Technology                                                                           |
| :----------------------- | :----------------------------------------------------------------------------------- |
| **Backend**        | Java 17+, Spring Boot 3.4, Spring Security, Spring Data JPA, WebSocket, MySQL, Redis |
| **Frontend**       | React 18, TypeScript, Vite, Redux Toolkit, RTK Query, CSS Modules, dnd-kit           |
| **Infrastructure** | Docker & Docker Compose, Flyway (DB Migration), Cloudinary (Image Hosting), MailHog  |

---

## 🚀 Hướng dẫn cài đặt (Prerequisites)

Yêu cầu các công cụ sau đã được cài đặt:

- **Java JDK 21+**
- **Maven 3.9+**
- **Node.js 20+**
- **Docker Desktop**

---

## 💻 Cách chạy dự án (Run the Project)

### Cách 1: Chạy từng phần cục bộ (Local Development)

1. **Khởi động Database (MySQL & Redis)**:

   ```bash
   docker compose up mysql redis -d
   ```
2. **Chạy Backend**:

   ```bash
   cd backend
   mvn spring-boot:run
   ```

   * API: `http://localhost:8080`
   * Swagger UI: `http://localhost:8080/swagger-ui.html`
3. **Chạy Frontend**:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   * App: `http://localhost:5173`

### Cách 2: Chạy toàn bộ với Docker Compose

```bash
docker compose up --build -d
```

* Frontend: `http://localhost:3000`
* Backend: `http://localhost:8080`

---

## 📂 Cấu trúc dự án (Structure)

```text
okabe/
|-- backend/          # Spring Boot Source
|-- frontend/         # React + Vite Source
|-- docker-compose.yml
|-- .env              # Environment Variables
`-- README.md
```

---

## 🛡️ Git Safety Rules

Before performing ANY git operation:

1. **Inspect first:**
   ```bash
   git status
   git branch
   git log --graph --oneline --decorate -20
   ```

2. **Never execute:**
   - `git push --force`
   - `git push --force-with-lease`
   - `git reset --hard`
   - `git rebase`
   - `git commit --amend`

3. **Always create a new commit instead.**

4. **If history appears inconsistent:**
   - STOP
   - Explain the issue
   - Ask for confirmation

5. **Preserve commit history.**

6. **Use only safe operations:**
   ```bash
   git add .
   git commit -m "<message>"
   git push
   ```

---

## 📜 License

MIT Copyright 2026 **OKABE Team** - *Đưa quản lý dự án lên tầm cao mới.*
