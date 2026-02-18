-- Add timezone support to organizations
ALTER TABLE organizations ADD COLUMN timezone VARCHAR(50) NOT NULL DEFAULT 'UTC';

-- Create notification logs table for tracking email notifications
CREATE TABLE notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id UUID NOT NULL REFERENCES schedules(id) ON DELETE CASCADE,
    recipient_type VARCHAR(20) NOT NULL CHECK (recipient_type IN ('STAFF', 'CLIENT')),
    recipient_email VARCHAR(255) NOT NULL,
    notification_type VARCHAR(50) NOT NULL CHECK (notification_type IN ('SCHEDULE_CREATED', 'SCHEDULE_UPDATED', 'SCHEDULE_CANCELLED')),
    sent_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    status VARCHAR(20) NOT NULL CHECK (status IN ('SENT', 'FAILED')),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for notification logs
CREATE INDEX idx_notification_logs_schedule_id ON notification_logs(schedule_id);
CREATE INDEX idx_notification_logs_recipient_email ON notification_logs(recipient_email);
CREATE INDEX idx_notification_logs_created_at ON notification_logs(created_at);
