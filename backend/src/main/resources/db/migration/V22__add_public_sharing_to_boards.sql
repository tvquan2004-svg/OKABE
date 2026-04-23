-- Add public sharing columns to boards table
ALTER TABLE boards ADD COLUMN is_public BOOLEAN DEFAULT FALSE;
ALTER TABLE boards ADD COLUMN public_token VARCHAR(64) UNIQUE;
