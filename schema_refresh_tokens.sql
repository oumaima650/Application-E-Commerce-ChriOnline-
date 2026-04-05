-- Schema for Refresh Token persistence and rotation
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token_hash VARCHAR(64) NOT NULL, -- SHA-256 hash of the UUID (fixed length)
    expires_at DATETIME NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    is_revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_token_hash (token_hash),
    FOREIGN KEY (user_id) REFERENCES Utilisateur(IdUtilisateur) ON DELETE CASCADE
);

-- Table for Progressive Brute-Force protection state
CREATE TABLE IF NOT EXISTS login_security_state (
    id INT AUTO_INCREMENT PRIMARY KEY,
    identifier VARCHAR(255) NOT NULL,       -- IP (ex: 192.168.1.1) or Email
    type ENUM('IP', 'EMAIL') NOT NULL,      -- IP or EMAIL based tracking
    current_attempts INT DEFAULT 0,         -- Failed attempts at current level
    current_level INT DEFAULT 1,            -- Current Security Level (1 to 4)
    last_attempt_at DATETIME,               -- Last attempt timestamp
    blocked_until DATETIME DEFAULT NULL,    -- Block expiration
    must_reset_password BOOLEAN DEFAULT FALSE, -- Level 4 flag
    UNIQUE KEY uq_identifier_type (identifier, type)
) ENGINE=InnoDB;

-- Table for Security Audit Logs
CREATE TABLE IF NOT EXISTS security_audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    event_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),                 -- Source IP
    email VARCHAR(255),                     -- Target Email
    level_assigned INT,                     -- Level at event time
    attempt_result VARCHAR(50),             -- 'SUCCESS', 'WRONG_PASSWORD', 'WRONG_CAPTCHA', 'BLOCKED'
    captcha_passed BOOLEAN,                 -- Captcha validation result
    INDEX idx_ip (ip_address),
    INDEX idx_email (email)
) ENGINE=InnoDB;

-- Table for Password Reset Codes (OTP)
CREATE TABLE IF NOT EXISTS password_reset_codes (
    email VARCHAR(191) PRIMARY KEY,        -- One active code per email
    code_OPT_hash VARCHAR(255) NOT NULL,   -- BCrypt hash of the 6-digit OTP
    expires_at DATETIME NOT NULL,          -- Expiration (e.g., 10 minutes)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
