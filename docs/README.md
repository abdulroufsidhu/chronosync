# ChronoSync Documentation

This folder contains comprehensive documentation for the ChronoSync backend API.

## Table of Contents

- [Authentication Overview](./authentication-overview.md)
- [Magic Link Authentication](./magic-link.md) - Passwordless email-based login
- [Schedule Notifications](./schedule-notifications.md) - Email notifications with calendar integration
- [API Reference](./api-reference.md)
- [Configuration](./configuration.md)

## New Features

### 🌍 Organization Timezone Support
Each organization can now have its own timezone setting:
- Timezone is auto-detected from IP during registration
- All schedule times are displayed in the organization's timezone
- Automatic DST (Daylight Saving Time) handling
- See [Schedule Notifications](./schedule-notifications.md) for details

### 📧 Schedule Email Notifications
When schedules are created, updated, or cancelled:
- **Staff members** receive email notifications with appointment details
- **Clients** receive confirmation emails with calendar attachments
- **ICS calendar files** compatible with Google, Outlook, Apple Calendar
- **Direct calendar links** for one-click adding

## Quick Links

- [Authentication Flow](#authentication-flow)
- [Magic Link Special Cases](#magic-link-special-cases)
- [Schedule Notifications](./schedule-notifications.md)

## Authentication Flow

ChronoSync supports multiple authentication methods:

1. **Email/Password Login** - Traditional credentials-based authentication
2. **Magic Link** - Passwordless email-based authentication (see [magic-link.md](./magic-link.md))
3. **JWT Token** - Bearer token for API access after authentication

## Magic Link Special Cases

Magic link authentication has specific requirements for frontend implementation. The frontend **must** provide a route that matches the expected verification URL pattern:

```
{FRONTEND_URL}/api/auth/magic-link/verify?token={TOKEN}
```

See the [Magic Link Documentation](./magic-link.md) for complete implementation details.

## Getting Started

For API integration guides and endpoint documentation, see the individual documentation files in this folder.

## Environment Configuration

The backend relies on environment variables for configuration. Key variables include:

- `FRONTEND_URL` - The frontend application URL (used for generating magic link URLs)
- `JWT_SECRET` - Secret key for JWT token signing
- `SMTP_HOST`, `SMTP_USERNAME`, `SMTP_PASSWORD` - Email service configuration

See [configuration.md](./configuration.md) for the complete list.
