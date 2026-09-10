-- Aerospace-ERP: create one database per service on the VPS's existing Postgres.
-- Run as: sudo -u postgres psql -f setup-databases.sql
--
-- All 11 owned by the existing `postgres` user, matching this project's existing
-- convention (see spring.datasource.username in every backend/*/application.yml) --
-- not creating per-service DB users here, to keep this a drop-in match for the
-- config that's already externalized via DB_PASSWORD. Tightening this to
-- per-service least-privilege users is a reasonable future hardening step, just
-- a bigger change than this deployment pass (every service's datasource.username
-- would need externalizing too, not just the password).

CREATE DATABASE auth_db;
CREATE DATABASE inventory_db;
CREATE DATABASE qa_db;
CREATE DATABASE notification_db;
CREATE DATABASE workorder_db;
CREATE DATABASE production_db;
CREATE DATABASE procurement_db;
CREATE DATABASE reporting_db;
CREATE DATABASE production_resource_db;
CREATE DATABASE sales_db;
CREATE DATABASE finance_db;
