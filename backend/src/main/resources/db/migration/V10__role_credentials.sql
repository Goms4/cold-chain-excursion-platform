CREATE TABLE role_credential (
    role VARCHAR(32) PRIMARY KEY CHECK (role IN ('OPERATIONS', 'ANALYST')),
    password_hash VARCHAR(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

GRANT SELECT, INSERT, UPDATE ON role_credential TO coldchain_app;
