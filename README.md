# ChronoSync Backend

A Spring Boot backend for the ChronoSync scheduling SaaS application. [documentation](./docs/README.md)

## Tech Stack

- Kotlin
- Spring Boot 3.2
- PostgreSQL
- Flyway Migrations
- JWT Authentication
- Docker & Docker Compose

## Quick Start

### Prerequisites

- Docker & Docker Compose
- JDK 17+

### Development

1. Start PostgreSQL:
```bash
docker compose up -d db
```

2. Run migrations:
```bash
docker compose up flyway
```

3. Run the application:
```bash
cd backend
./gradlew bootRun
```

### Production

```bash
docker compose up -d
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login
- `POST /api/auth/register` - Register

### Dashboard
- `GET /api/dashboard/today` - Get today's schedules

### Schedules
- `GET /api/schedules` - Get schedules (with date range)
- `GET /api/schedules/{id}` - Get schedule details
- `POST /api/schedules` - Create schedule
- `PUT /api/schedules/{id}` - Update schedule
- `DELETE /api/schedules/{id}` - Delete schedule

### Organization
- `GET /api/organization/users` - Get organization users
- `GET /api/organization/users/dropdown` - Get users for dropdown
- `POST /api/organization/invite` - Invite user
- `DELETE /api/organization/users/{id}` - Remove user
- `PUT /api/organization/users/{id}/role` - Update user role

### Usage & Billing
- `GET /api/usage` - Get current usage
- `GET /api/billing/plans` - Get available plans
- `POST /api/billing/subscribe` - Upgrade plan

### Profile
- `GET /api/profile` - Get user profile

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| DATABASE_HOST | PostgreSQL host | localhost |
| DATABASE_PORT | PostgreSQL port | 5432 |
| DATABASE_NAME | Database name | chronosync |
| DATABASE_USERNAME | Database username | postgres |
| DATABASE_PASSWORD | Database password | postgres |
| APP_HOST | Application host | 0.0.0.0 |
| APP_PORT | Application port | 8080 |
| JWT_SECRET | JWT signing secret | - |
| JWT_EXPIRATION | JWT expiration (ms) | 86400000 |

## License

MIT
