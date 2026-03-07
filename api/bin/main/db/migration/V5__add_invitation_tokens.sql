-- Create invitation tokens table
CREATE TABLE invitation_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    token VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    church_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    invited_by UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_invitation_church 
        FOREIGN KEY (church_id) REFERENCES churches(id) ON DELETE CASCADE,
    CONSTRAINT fk_invitation_invited_by 
        FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_invitation_token ON invitation_tokens(token);
CREATE INDEX idx_invitation_email ON invitation_tokens(email);
CREATE INDEX idx_invitation_church_id ON invitation_tokens(church_id);
CREATE INDEX idx_invitation_expires_at ON invitation_tokens(expires_at);

-- Add constraint to ensure valid roles
ALTER TABLE invitation_tokens ADD CONSTRAINT chk_invitation_role 
    CHECK (role IN ('CHURCH_ADMIN', 'WORSHIP_LEADER', 'TEAM_MEMBER', 'SUPER_ADMIN'));