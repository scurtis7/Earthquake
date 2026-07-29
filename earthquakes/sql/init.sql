-- Fresh database init script for the Earthquakes app.
-- Run this against a new Postgres instance to create the schema/table the app expects.
--
-- Usage:
--   psql -h localhost -U postgres -d earthquakeDB -f sql/init.sql
--
-- If the earthquakeDB database itself doesn't exist yet, create it first:
--   createdb -h localhost -U postgres earthquakeDB

CREATE SCHEMA IF NOT EXISTS earthquake;

CREATE TABLE IF NOT EXISTS earthquake.day_count
(
    id    SERIAL PRIMARY KEY,
    day   VARCHAR(10) UNIQUE NOT NULL,
    count INTEGER,
    created_date date default CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS earthquake.month_count
(
    id    SERIAL PRIMARY KEY,
    month VARCHAR(10) UNIQUE NOT NULL,
    count INTEGER,
    created_date date default CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS earthquake.year_count
(
    id    SERIAL PRIMARY KEY,
    year  VARCHAR(10) UNIQUE NOT NULL,
    count INTEGER,
    created_date date default CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS earthquake.earthquake
(
    id              SERIAL PRIMARY KEY,
    feature_id      VARCHAR(50) UNIQUE NOT NULL,
    coordinate_type VARCHAR(10),
    coordinates     VARCHAR(255),
    code            VARCHAR(255),
    ids             VARCHAR(255),
    title           VARCHAR(511),
    place           VARCHAR(511),
    date_time       VARCHAR(100),
    updated_date    VARCHAR(100),
    tz              VARCHAR(100),
    type            VARCHAR(255),
    types           VARCHAR(511),
    tsunami         INTEGER,
    sig             INTEGER,
    detail          VARCHAR(511),
    url             VARCHAR(511),
    mag             DOUBLE PRECISION,
    mag_type        VARCHAR(255),
    status          VARCHAR(255),
    sources         VARCHAR(255),
    net             VARCHAR(255),
    felt            VARCHAR(255),
    cdi             VARCHAR(255),
    mmi             VARCHAR(255),
    alert           VARCHAR(255),
    nst             INTEGER,
    gap             DOUBLE PRECISION,
    dmin            DOUBLE PRECISION,
    rms             DOUBLE PRECISION,
    created_date    date default CURRENT_DATE
);
