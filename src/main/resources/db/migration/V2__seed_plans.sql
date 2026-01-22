INSERT INTO plans (name, price, schedule_limit, features) VALUES
('FREE', '$0/month', 100, 'Basic features, Up to 100 schedules/month'),
('PRO', '$5/month', 500, 'Priority support, Up to 500 schedules/month, Advanced features'),
('ENTERPRISE', '$20/month', -1, 'Unlimited schedules, 24/7 support, All features, Custom integrations')
ON CONFLICT (name) DO NOTHING;
