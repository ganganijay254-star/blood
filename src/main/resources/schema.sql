-- Remove a stale legacy table that can break Hibernate metadata reads.
DROP TABLE IF EXISTS donation_visit;
DROP TABLE IF EXISTS roles;
