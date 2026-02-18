# Schedule Notifications

ChronoSync automatically sends email notifications when schedules are created, updated, or cancelled. Both staff members and clients receive notifications with calendar integration.

## Table of Contents

- [Overview](#overview)
- [Email Notifications](#email-notifications)
- [Calendar Integration](#calendar-integration)
- [Timezone Handling](#timezone-handling)
- [Configuration](#configuration)
- [Email Templates](#email-templates)

## Overview

When a schedule is created or modified, the system automatically sends notifications to:

1. **Assigned Staff Member** - If a user is assigned to the schedule
2. **Client** - If client email is provided

### Notification Triggers

| Action | Notification Type | Recipients |
|--------|------------------|------------|
| Create Schedule | `SCHEDULE_CREATED` | Staff + Client |
| Update Schedule | `SCHEDULE_UPDATED` | Staff + Client |
| Delete Schedule | `SCHEDULE_CANCELLED` | Staff + Client |

### Notification Logging

All notifications are logged in the `notification_logs` table for tracking:
- Recipient type (STAFF/CLIENT)
- Notification type
- Send status (SENT/FAILED)
- Timestamp
- Error messages (if any)

## Email Notifications

### Staff Email

Staff members receive emails with:
- Appointment title and details
- Date and time (in organization's timezone)
- Client information (name, phone, email, address)
- Notes and special instructions
- ICS calendar attachment
- Link to view in ChronoSync

**Subject**: "New Appointment Scheduled - [Client Name]"

### Client Email

Clients receive professional emails with:
- Organization branding
- Appointment confirmation details
- ICS calendar attachment
- Direct "Add to Calendar" buttons for Google, Outlook, Yahoo
- Location and notes

**Subject**: "Your Appointment Confirmation - [Organization Name]"

## Calendar Integration

### ICS Calendar Files

Every notification includes an `.ics` file attachment that:
- Works with all major calendar apps (Google, Outlook, Apple, etc.)
- Contains event title, description, location
- Uses the organization's timezone
- Supports DST transitions automatically
- For updates: Updates existing calendar events
- For cancellations: Marks events as cancelled

### Direct Calendar Links

Client emails include one-click buttons:
- **Google Calendar** - Direct add URL
- **Outlook** - Web calendar compose URL
- **Yahoo Calendar** - Event creation URL

## Timezone Handling

### Organization Timezone

Each organization has a timezone setting (IANA format):
- **Default**: UTC
- **Auto-detection**: Detected from IP during registration
- **Format**: IANA timezone IDs (e.g., `America/New_York`, `Europe/London`)
- **Storage**: Stored in `organizations.timezone` column

### Time Display

All schedule times are:
1. **Stored in database**: UTC (ISO 8601 format)
2. **Displayed in emails**: Organization's local timezone
3. **Shown in calendar files**: Organization's timezone with DST rules
4. **Returned by API**: UTC with timezone field for reference

### DST Support

The system automatically handles Daylight Saving Time:
- Calendar files include VTIMEZONE blocks
- DST transitions calculated based on timezone rules
- No manual intervention required

### Registration Flow

```
User registers
    ↓
Timezone provided? 
    ↓ Yes → Use provided timezone
    ↓ No
Detect from IP
    ↓
Create organization with timezone
```

## Configuration

### Application Settings

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    clean-disabled: true  # Safety: prevent accidental data loss

app:
  timezone:
    ip-api:
      enabled: true
      url: https://ipapi.co
```

### Environment Variables

No additional configuration required. The system uses existing database and email settings.

### IP Detection

Timezones are detected using [ipapi.co](https://ipapi.co) service:
- Free tier: 30,000 requests/month
- No API key required
- Can be disabled by setting `app.timezone.ip-api.enabled=false`
- Falls back to UTC if detection fails

## Email Templates

### Template Location

Email templates are embedded in `EmailService.kt`:
- `buildStaffScheduleEmail()` - Staff notifications
- `buildClientScheduleEmail()` - Client notifications

### Template Features

- Responsive HTML design
- Organization branding
- Calendar attachment
- Direct action buttons
- Fallback plain text

### Customization

To customize email templates, modify the `EmailService.kt` file:
- Update HTML content in `buildStaffScheduleEmail()`
- Update HTML content in `buildClientScheduleEmail()`
- Modify subjects and formatting

## API Changes

### Schedule DTO

```json
{
  "id": "uuid",
  "title": "Appointment",
  "startDateTime": "2024-01-15T14:00:00Z",
  "endDateTime": "2024-01-15T15:00:00Z",
  "timezone": "America/New_York",  // NEW
  "organization": {
    "id": "uuid",
    "name": "My Org",
    "plan": "FREE",
    "role": "OWNER",
    "timezone": "America/New_York"  // NEW
  },
  ...
}
```

### Organization DTO

```json
{
  "id": "uuid",
  "name": "My Organization",
  "plan": "FREE",
  "role": "OWNER",
  "timezone": "America/New_York"  // NEW
}
```

### Registration Request

```json
{
  "email": "admin@example.com",
  "password": "SecureP@ss123",
  "organization": {
    "name": "My Salon",
    "services": ["haircut", "beard"],
    "type": "salon",
    "role": "OWNER",
    "timezone": "America/New_York"  // Optional, auto-detected if not provided
  }
}
```

## Testing

### Manual Testing

1. **Create a schedule** with client email
2. **Check notifications** are sent to both staff and client
3. **Verify ICS file** opens correctly in calendar apps
4. **Test timezone** by creating schedules in different timezones
5. **Test DST** by creating schedules across DST transition dates

### Test Email Setup

For testing without real email:
- Use [Mailtrap](https://mailtrap.io) for catching emails
- Configure SMTP settings in `.env` file
- All emails will be caught in Mailtrap inbox

## Troubleshooting

### Emails Not Sending

1. Check SMTP configuration in application.yml
2. Verify `app.email-from` is set
3. Check notification_logs table for errors
4. Review application logs for email service errors

### Wrong Timezone

1. Check organization's timezone in database
2. Verify IP detection is working (check logs)
3. Manually update timezone if needed:
   ```sql
   UPDATE organizations SET timezone = 'America/New_York' WHERE id = 'uuid';
   ```

### Calendar Files Not Working

1. Verify ICS content is valid (use online validators)
2. Check timezone is valid IANA format
3. Ensure email client supports ICS attachments

## Related Documentation

- [API Reference](./api-reference.md) - Complete API documentation
- [Configuration](./configuration.md) - Environment setup
- [Authentication Overview](./authentication-overview.md) - User authentication
