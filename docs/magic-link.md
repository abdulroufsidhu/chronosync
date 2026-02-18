# Magic Link Authentication

Magic link authentication provides a passwordless login experience where users receive a secure, time-limited link via email to authenticate into the application.

## Table of Contents

- [Overview](#overview)
- [Flow Diagram](#flow-diagram)
- [Frontend Requirements](#frontend-requirements)
- [API Endpoints](#api-endpoints)
- [Security Considerations](#security-considerations)
- [Error Handling](#error-handling)
- [Configuration](#configuration)

## Overview

Magic links allow users to sign in without entering a password. Instead, they:

1. Enter their email address
2. Receive an email with a secure, one-time use link
3. Click the link to be automatically authenticated

**Key Features:**
- Single-use tokens (consumed on verification)
- 15-minute expiration time
- Automatic email verification on first use
- Email enumeration protection (always returns success)

## Flow Diagram

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   Frontend  │         │   Backend   │         │    Email    │
└──────┬──────┘         └──────┬──────┘         └──────┬──────┘
       │                       │                       │
       │  POST /api/auth/      │                       │
       │  magic-link           │                       │
       │  {email}              │                       │
       │──────────────────────>│                       │
       │                       │                       │
       │  200 OK               │                       │
       │  (always success)     │                       │
       │<──────────────────────│                       │
       │                       │                       │
       │                       │  Generate token       │
       │                       │  Save to DB           │
       │                       │                       │
       │                       │  Send email           │
       │                       │──────────────────────>│
       │                       │                       │
       │                       │                       │ Sends
       │                       │                       │ magic link
       │                       │                       │ email
       │                       │                       │
       │    User clicks link   │                       │
       │    in email           │                       │
       │                       │                       │
       │  GET /api/auth/       │                       │
       │  magic-link/verify    │                       │
       │  ?token=xxx           │                       │
       │──────────────────────>│                       │
       │                       │                       │
       │                       │  Validate token       │
       │                       │  Mark as used         │
       │                       │  Generate JWT         │
       │                       │                       │
       │  200 OK               │                       │
       │  {accessToken,        │                       │
       │   user, organization} │                       │
       │<──────────────────────│                       │
       │                       │                       │
       │  Store JWT and        │                       │
       │  redirect to app      │                       │
       │                       │                       │
```

## Frontend Requirements

### Critical Implementation Detail

The **frontend application must provide a route** that matches this exact pattern:

```
{FRONTEND_URL}/api/auth/magic-link/verify?token={TOKEN}
```

**This is NOT a backend endpoint that the frontend calls.** Instead, this is a **frontend route** that:

1. Extracts the `token` query parameter from the URL
2. Makes a GET request to the backend: `GET /api/auth/magic-link/verify?token={token}`
3. Handles the authentication response

### Frontend Route Handler Example

#### React Example

```javascript
// Route configuration
<Route path="/api/auth/magic-link/verify" element={<MagicLinkVerify />} />

// MagicLinkVerify component
function MagicLinkVerify() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  useEffect(() => {
    if (!token) {
      navigate('/login?error=invalid-token');
      return;
    }

    // Call the backend verification endpoint
    fetch(`/api/auth/magic-link/verify?token=${token}`)
      .then(response => response.json())
      .then(data => {
        if (data.success) {
          // Store JWT token
          localStorage.setItem('accessToken', data.data.accessToken);
          localStorage.setItem('user', JSON.stringify(data.data.user));
          localStorage.setItem('organization', JSON.stringify(data.data.organization));
          
          // Redirect to dashboard
          navigate('/dashboard');
        } else {
          navigate('/login?error=' + encodeURIComponent(data.message));
        }
      })
      .catch(error => {
        navigate('/login?error=verification-failed');
      });
  }, [token, navigate]);

  return <div>Verifying magic link...</div>;
}
```

#### Vue.js Example

```javascript
// Route configuration
{
  path: '/api/auth/magic-link/verify',
  component: MagicLinkVerify,
  name: 'magic-link-verify'
}

// MagicLinkVerify component
export default {
  mounted() {
    const token = this.$route.query.token;
    
    if (!token) {
      this.$router.push('/login?error=invalid-token');
      return;
    }

    // Call the backend verification endpoint
    fetch(`/api/auth/magic-link/verify?token=${token}`)
      .then(response => response.json())
      .then(data => {
        if (data.success) {
          // Store JWT token
          localStorage.setItem('accessToken', data.data.accessToken);
          localStorage.setItem('user', JSON.stringify(data.data.user));
          localStorage.setItem('organization', JSON.stringify(data.data.organization));
          
          // Redirect to dashboard
          this.$router.push('/dashboard');
        } else {
          this.$router.push('/login?error=' + encodeURIComponent(data.message));
        }
      })
      .catch(() => {
        this.$router.push('/login?error=verification-failed');
      });
  }
}
```

### URL Generation

The backend generates the magic link URL using the `FRONTEND_URL` environment variable:

```kotlin
// From EmailService.kt
val magicLinkUrl = "$frontendUrl/api/auth/magic-link/verify?token=$token"
```

**Default URL:** `http://localhost:3000/api/auth/magic-link/verify?token={token}`

## API Endpoints

### 1. Request Magic Link

Send a magic link to the user's email address.

**Endpoint:** `POST /api/auth/magic-link`

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "message": "If an account exists with this email, you will receive a sign-in link."
  }
}
```

**Important:** This endpoint always returns success to prevent email enumeration attacks. Even if the email doesn't exist in the system, the response will be identical.

### 2. Verify Magic Link

Verify the magic link token and authenticate the user.

**Endpoint:** `GET /api/auth/magic-link/verify?token={token}`

**Query Parameters:**
- `token` (required): The magic link token from the email

**Success Response (200 OK):**
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

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Invalid or expired magic link"
}
```

## Security Considerations

### Token Properties

| Property | Value | Description |
|----------|-------|-------------|
| Length | 32 bytes (43 chars Base64) | Cryptographically secure random |
| Expiration | 15 minutes | Short-lived for security |
| Single-use | Yes | Marked as used after verification |
| Algorithm | SecureRandom | Java secure random generator |

### Security Features

1. **Email Enumeration Protection**: The request endpoint always returns success, preventing attackers from discovering which emails are registered.

2. **Single-Use Tokens**: Once a magic link is used, the token is marked as used and cannot be reused.

3. **Time-Limited**: Tokens expire after 15 minutes, reducing the window of attack.

4. **Automatic Email Verification**: If a user's email hasn't been verified yet, using a magic link automatically verifies it.

5. **Secure Token Generation**: Uses `SecureRandom` with 32 bytes of entropy, Base64 URL-encoded.

### Token Storage

Tokens are stored in the database with the following structure:

```sql
CREATE TABLE auth_tokens (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    token VARCHAR(255) UNIQUE NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'MAGIC_LINK'
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,         -- NULL until used
    created_at TIMESTAMP
);
```

**Database indexes:**
- `token` - For quick lookup
- `user_id` - For invalidating previous tokens
- `type` - For filtering by token type

## Error Handling

### Common Error Scenarios

| Scenario | Error Message | HTTP Status |
|----------|---------------|-------------|
| Invalid token | "Invalid or expired magic link" | 400 |
| Expired token | "Invalid or expired magic link" | 400 |
| Already used token | "Invalid or expired magic link" | 400 |
| Missing token parameter | Validation error | 400 |

### Frontend Error Handling Best Practices

1. **Always redirect to login on error**: Don't leave users on the verification page
2. **Show user-friendly messages**: Translate technical errors to actionable messages
3. **Provide fallback options**: Include links to request a new magic link or use password login

```javascript
// Example error handling
if (!data.success) {
  const errorMessages = {
    'Invalid or expired magic link': 'This sign-in link has expired or is invalid. Please request a new one.',
    'default': 'We could not sign you in. Please try again or use your password.'
  };
  
  const message = errorMessages[data.message] || errorMessages.default;
  navigate(`/login?error=${encodeURIComponent(message)}`);
}
```

## Configuration

### Backend Configuration

The magic link feature is configured via environment variables:

```yaml
# application.yml
app:
  frontend-url: ${FRONTEND_URL:http://localhost:3000}
```

**Environment Variables:**

| Variable | Default | Description |
|----------|---------|-------------|
| `FRONTEND_URL` | `http://localhost:3000` | Base URL of the frontend application |

### Frontend Configuration

Ensure your frontend routing is configured to handle the verification URL:

```
Path: /api/auth/magic-link/verify
Query Parameter: token
```

**Example production URLs:**
- Development: `http://localhost:3000/api/auth/magic-link/verify?token=abc123`
- Production: `https://app.chronosync.com/api/auth/magic-link/verify?token=abc123`

## Implementation Reference

### Backend Files

| File | Purpose |
|------|---------|
| `AuthController.kt` | REST endpoints for magic link request/verify |
| `AuthService.kt` | Business logic for token generation and verification |
| `EmailService.kt` | Email sending with magic link URL generation |
| `AuthToken.kt` | Entity for token storage |
| `AuthTokenType.kt` | Enum for token types (MAGIC_LINK) |

### Key Code Snippets

**Token Generation (AuthService.kt):**
```kotlin
private fun generateSecureToken(): String {
    val random = SecureRandom()
    val bytes = ByteArray(32)
    random.nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
```

**URL Generation (EmailService.kt):**
```kotlin
val magicLinkUrl = "$frontendUrl/api/auth/magic-link/verify?token=$token"
```

**Token Validation (AuthService.kt):**
```kotlin
fun verifyMagicLink(token: String): AuthResponse {
    val authToken = authTokenRepository.findByTokenAndType(token, AuthTokenType.MAGIC_LINK)
        ?: throw IllegalArgumentException("Invalid or expired magic link")

    if (!authToken.isValid()) {
        throw IllegalArgumentException("Invalid or expired magic link")
    }
    
    // Mark as used and generate JWT...
}
```

## Testing

### Manual Testing Steps

1. **Request magic link:**
   ```bash
   curl -X POST http://localhost:8080/api/auth/magic-link \
     -H "Content-Type: application/json" \
     -d '{"email": "user@example.com"}'
   ```

2. **Check email** (or logs if using mailtrap/dev mode)

3. **Extract token from URL** in the email

4. **Verify token:**
   ```bash
   curl "http://localhost:8080/api/auth/magic-link/verify?token=YOUR_TOKEN_HERE"
   ```

5. **Verify response contains** `accessToken`, `user`, and `organization`

### Integration Testing

Ensure your frontend integration tests cover:
- Successful magic link verification
- Expired token handling
- Invalid token handling
- Missing token parameter handling
- JWT storage and redirection

## FAQ

**Q: Can the same magic link be used multiple times?**
A: No. Magic links are single-use only. Once clicked, the token is marked as used.

**Q: How long do magic links last?**
A: 15 minutes from the time of generation.

**Q: What happens if the user's email isn't verified?**
A: The magic link automatically verifies the email address on first use.

**Q: Why does the request endpoint always return success?**
A: To prevent email enumeration attacks. Attackers cannot determine if an email is registered by checking the response.

**Q: Can I customize the magic link URL?**
A: The frontend URL path (`/api/auth/magic-link/verify`) is fixed and must be implemented by the frontend. Only the base URL (`FRONTEND_URL`) is configurable.
