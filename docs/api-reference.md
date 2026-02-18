# API Reference

Complete API reference for the ChronoSync backend.

## Base URL

```
Development: http://localhost:8080
Production: https://api.chronosync.com
```

All endpoints are prefixed with `/api` unless otherwise specified.

## Authentication

All protected endpoints require a JWT Bearer token in the Authorization header:

```http
Authorization: Bearer {accessToken}
```

See [authentication-overview.md](./authentication-overview.md) for authentication details.

## Endpoints

### Authentication

| Method | Endpoint | Description | Public |
|--------|----------|-------------|--------|
| POST | `/auth/login` | Email/password login | Yes |
| POST | `/auth/register` | Create organization and user | Yes |
| POST | `/auth/forgot-password` | Request password reset | Yes |
| POST | `/auth/reset-password` | Reset password with token | Yes |
| POST | `/auth/magic-link` | Request magic link | Yes |
| GET | `/auth/magic-link/verify` | Verify magic link token | Yes |

### Profile

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/profile` | Get current user profile |
| PUT | `/profile` | Update user profile |

### Organization

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/organization/users` | List organization users |
| GET | `/organization/users/dropdown` | Get users for dropdown |
| POST | `/organization/invite` | Invite user to organization |
| DELETE | `/organization/users/{id}` | Remove user from organization |
| PUT | `/organization/users/{id}/role` | Update user role |

### Schedules

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/schedules/today` | Get today's schedules |
| GET | `/schedules` | Get schedules by date range |
| GET | `/schedules/{id}` | Get schedule by ID |
| POST | `/schedules` | Create new schedule |
| PUT | `/schedules/{id}` | Update schedule |
| DELETE | `/schedules/{id}` | Delete schedule |

### Dashboard

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/dashboard/today` | Get today's dashboard data |

### Billing

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/usage` | Get current usage statistics |
| GET | `/billing/plans` | List available plans |
| POST | `/billing/subscribe` | Subscribe to a plan |

### Health

| Method | Endpoint | Description | Public |
|--------|----------|-------------|--------|
| GET | `/health` | Health check endpoint | Yes |

## Request/Response Formats

### Standard Response Wrapper

All API responses follow this format:

```json
{
  "success": true,
  "data": { ... },
  "message": "Optional message"
}
```

### Error Response

```json
{
  "success": false,
  "message": "Error description"
}
```

### Date/Time Format

- Dates: ISO 8601 format (`2024-01-15`)
- DateTime: ISO 8601 format (`2024-01-15T10:30:00Z`) - All times are in UTC
- Timezone: IANA format (e.g., `America/New_York`, `Europe/London`)

### Timezone Handling

All schedule times are stored and returned in UTC. The API includes a `timezone` field in responses:

```json
{
  "id": "uuid",
  "title": "Appointment",
  "startDateTime": "2024-01-15T14:00:00Z",
  "endDateTime": "2024-01-15T15:00:00Z",
  "timezone": "America/New_York",
  "organization": {
    "timezone": "America/New_York"
  }
}
```

The frontend should use the `timezone` field to display times in the organization's local time.

### Pagination

List endpoints support pagination via query parameters:

```
GET /api/organization/users?page=0&size=20
```

Response includes:
```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 100,
  "totalPages": 5
}
```

## HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Authentication required |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found |
| 409 | Conflict - Resource already exists |
| 500 | Internal Server Error |

## Rate Limiting

API endpoints are rate-limited per IP address:

- Authentication endpoints: 5 requests per minute
- All other endpoints: 100 requests per minute

## OpenAPI/Swagger

Interactive API documentation is available at:

```
http://localhost:8080/swagger-ui/index.html
```

OpenAPI spec available at:

```
http://localhost:8080/v3/api-docs
```
