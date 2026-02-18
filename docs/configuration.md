# Configuration

Environment configuration guide for the ChronoSync backend.

## Environment Variables

### Database

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DATABASE_HOST` | No | `localhost` | PostgreSQL host |
| `DATABASE_PORT` | No | `5432` | PostgreSQL port |
| `DATABASE_NAME` | No | `chronosync` | Database name |
| `DATABASE_USERNAME` | No | `postgres` | Database username |
| `DATABASE_PASSWORD` | No | `postgres` | Database password |

### Application

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `APP_PORT` | No | `8080` | Application server port |
| `APP_HOST` | No | `0.0.0.0` | Server bind address |
| `FRONTEND_URL` | No | `http://localhost:3000` | Frontend application URL |

### JWT

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | No | (built-in) | JWT signing secret (min 256 bits) |
| `JWT_EXPIRATION` | No | `86400000` | Token expiration in milliseconds (24h) |

### Email (SMTP)

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SMTP_HOST` | No | `smtp.gmail.com` | SMTP server host |
| `SMTP_PORT` | No | `587` | SMTP server port |
| `SMTP_USERNAME` | Yes* | - | SMTP username |
| `SMTP_PASSWORD` | Yes* | - | SMTP password |

*Required for email functionality (magic links, password reset)

### Other

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `EMAIL_FROM` | No | `noreply@chronosync.com` | Default sender email |

### Timezone

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `IP_API_ENABLED` | No | `true` | Enable IP-based timezone detection |
| `IP_API_URL` | No | `https://ipapi.co` | IP geolocation service URL |

## Configuration Files

### application.yml

Main configuration file: `src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DATABASE_HOST:localhost}:${DATABASE_PORT:5432}/${DATABASE_NAME:chronosync}
    username: ${DATABASE_USERNAME:postgres}
    password: ${DATABASE_PASSWORD:postgres}
  
  mail:
    host: ${SMTP_HOST:smtp.gmail.com}
    port: ${SMTP_PORT:587}
    username: ${SMTP_USERNAME:}
    password: ${SMTP_PASSWORD:}

server:
  port: ${APP_PORT:8080}
  address: ${APP_HOST:0.0.0.0}

jwt:
  secret: ${JWT_SECRET:...}
  expiration: ${JWT_EXPIRATION:86400000}

app:
  frontend-url: ${FRONTEND_URL:http://localhost:3000}
  email-from: ${EMAIL_FROM:noreply@chronosync.com}
  timezone:
    ip-api:
      enabled: ${IP_API_ENABLED:true}
      url: ${IP_API_URL:https://ipapi.co}
```

### Environment File

Create a `.env` file in the project root:

```bash
# Database
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=chronosync
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=yourpassword

# Application
APP_PORT=8080
FRONTEND_URL=http://localhost:3000

# JWT
JWT_SECRET=your-super-secret-key-at-least-256-bits-long
JWT_EXPIRATION=86400000

# Email
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
EMAIL_FROM=noreply@chronosync.com
```

## Development Setup

### Local Development

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` with your values

3. Start the database:
   ```bash
   docker-compose up -d postgres
   ```

4. Run the application:
   ```bash
   ./gradlew bootRun
   ```

### Docker Development

```bash
# Start database
docker compose up -d db

# Run application with auto-migrations
./gradlew bootRun

# Or build and run in Docker
docker compose up --build -d app

# View logs
docker compose logs -f app
```

**Note:** Migrations run automatically when the Spring Boot application starts. No separate Flyway service needed.

## Production Setup

### Security Checklist

- [ ] Change default `JWT_SECRET` to a cryptographically secure random string
- [ ] Use strong database passwords
- [ ] Enable SSL/TLS for database connections
- [ ] Configure SMTP with authentication
- [ ] Set `FRONTEND_URL` to your production frontend URL
- [ ] Use HTTPS for `FRONTEND_URL`
- [ ] Enable database connection pooling
- [ ] Set up proper logging and monitoring

### Production Environment Example

```bash
# Database (use strong passwords)
DATABASE_HOST=prod-db.internal
DATABASE_PORT=5432
DATABASE_NAME=chronosync_prod
DATABASE_USERNAME=chronosync_app
DATABASE_PASSWORD=<strong-random-password>

# Application
APP_PORT=8080
APP_HOST=127.0.0.1  # Bind to localhost if behind reverse proxy
FRONTEND_URL=https://app.chronosync.com

# JWT (use a secure random string)
JWT_SECRET=<256-bit-random-hex-string>
JWT_EXPIRATION=86400000

# Email (production SMTP)
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USERNAME=apikey
SMTP_PASSWORD=<sendgrid-api-key>
EMAIL_FROM=noreply@chronosync.com
```

## Magic Link Configuration

Magic link emails contain URLs pointing to the frontend. The URL is constructed as:

```
{FRONTEND_URL}/api/auth/magic-link/verify?token={token}
```

**Important:** The frontend must implement this route. See [magic-link.md](./magic-link.md) for details.

## Logging Configuration

Default logging levels:

```yaml
logging:
  level:
    com.chronosync: DEBUG
    org.springframework.security: DEBUG
```

For production, reduce log levels:

```yaml
logging:
  level:
    com.chronosync: INFO
    org.springframework.security: WARN
    org.springframework.web: WARN
```

## Database Migrations

Database migrations are managed by Spring Boot Flyway and located in:

```
src/main/resources/db/migration/
```

### How It Works

- Migrations run **automatically** on application startup
- No manual steps or separate Docker services needed
- Flyway tracks applied migrations in `flyway_schema_history` table
- Failed migrations will prevent application startup

### Migration Naming Convention

```
V{version}__{description}.sql
```

Examples:
- `V1__create_initial_schema.sql`
- `V2__seed_plans.sql`
- `V4__timezone_and_notifications.sql`

### Configuration

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    clean-disabled: true  # Safety: prevents accidental data loss in production
```

### Troubleshooting

**Migration fails to apply:**
1. Check migration SQL syntax
2. Verify database connectivity
3. Review application logs for error details
4. Ensure migration version numbers are sequential

**Need to rollback:**
- Flyway doesn't support rollback in Community Edition
- Create a new migration to undo changes (e.g., `V5__undo_changes.sql`)

## Related Documentation

- [Authentication Overview](./authentication-overview.md)
- [Magic Link Authentication](./magic-link.md)
- [Schedule Notifications](./schedule-notifications.md)
