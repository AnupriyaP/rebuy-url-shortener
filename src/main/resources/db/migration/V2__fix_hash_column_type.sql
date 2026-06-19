-- Fix hash column from CHAR(4)/bpchar to VARCHAR(4)
-- Hibernate expects varchar not bpchar
ALTER TABLE shortened_urls
    ALTER COLUMN hash TYPE VARCHAR(4);