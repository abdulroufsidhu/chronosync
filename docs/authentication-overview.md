# Authentication Overview

ChronoSync uses JWT (JSON Web Token) based authentication with multiple login methods to provide secure and flexible access to the API.

## Authentication Methods

### 1. Email/Password Login

Traditional credentials-based authentication.

**Endpoint:** `POST /api/auth/login`

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecureP@ss123"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "role": "OWNER"
    },
    "organization": {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "name": "My Organization",
      "plan": "FREE",
      "role": "OWNER"
    }
  }
}
```

### 2. Magic Link (Passwordless)

Email-based passwordless authentication. See detailed documentation in [magic-link.md](./magic-link.md).

**Key Points:**
- No password required
- Single-use, time-limited tokens (15 minutes)
- Frontend must implement: `GET /api/auth/magic-link/verify?token={token}`

**Endpoints:**
- `POST /api/auth/magic-link` - Request magic link
- `GET /api/auth/magic-link/verify?token={token}` - Verify and authenticate

### 3. Registration

Create a new organization with an admin user.

**Endpoint:** `POST /api/auth/register`

**Request:**
```json
{
  "email": "admin@example.com",
  "password": "SecureP@ss123",
  "organization": {
    "name": "My Organization",
    "type": "salon",
    "services": ["haircut", "beard", "facial"],
    "role": "OWNER"
  }
}
```

## JWT Token

After successful authentication, the API returns a JWT access token that must be included in subsequent requests.

### Token Structure

The JWT contains:
- `userId` - UUID of the authenticated user
- `email` - User's email address
- `organizationId` - UUID of the current organization
- `role` - User's role in the organization (OWNER, MANAGER, MEMBER)
- `iat` - Issued at timestamp
- `exp` - Expiration timestamp (default: 24 hours)

### Using the Token

Include the token in the `Authorization` header as a Bearer token:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### Token Expiration

Tokens expire after 24 hours by default (configurable via `JWT_EXPIRATION`).

## Password Reset Flow

1. **Request reset:** `POST /api/auth/forgot-password`
   ```json
   {"email": "user@example.com"}
   ```

2. **Check email** for reset link (1 hour expiry)

3. **Reset password:** `POST /api/auth/reset-password`
   ```json
   {
     "token": "reset-token-from-email",
     "newPassword": "NewSecureP@ss123"
   }
   ```

## Protected Endpoints

All endpoints except the following require authentication:

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `POST /api/auth/magic-link`
- `GET /api/auth/magic-link/verify`
- `GET /api/health`
- Swagger UI: `/swagger-ui/**`
- API Docs: `/v3/api-docs/**`

## Security Best Practices

1. **Store tokens securely**: Use httpOnly cookies or secure storage
2. **Handle token expiration**: Implement refresh logic or redirect to login
3. **Use HTTPS**: Always use HTTPS in production
4. **Validate inputs**: All endpoints validate and sanitize inputs
5. **Rate limiting**: Implement rate limiting for auth endpoints

## Error Handling

Common authentication errors:

| Status | Error | Description |
|--------|-------|-------------|
| 400 | Invalid credentials | Wrong email/password |
| 400 | Email already registered | Registration conflict |
| 400 | Invalid or expired token | Password reset/magic link |
| 401 | Unauthorized | Missing or invalid JWT |
| 403 | Forbidden | Insufficient permissions |

## Configuration

Authentication configuration via environment variables:

```yaml
jwt:
  secret: ${JWT_SECRET:your-secret-key}
  expiration: ${JWT_EXPIRATION:86400000}  # 24 hours in milliseconds

app:
  frontend-url: ${FRONTEND_URL:http://localhost:3000}
```

## Related Documentation

- [Magic Link Authentication](./magic-link.md) - Detailed magic link implementation
- [API Reference](./api-reference.md) - Complete API documentation
