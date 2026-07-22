

select * from "earthquakeDB".earthquake.year_count order by year;
select * from "earthquakeDB".earthquake.year_count order by year DESC;
select * from "earthquakeDB".earthquake.year_count where year >= '1900-01-01' and year <= '1910-02-01' order by year;
select count(*) from "earthquakeDB".earthquake.year_count

select * from "earthquakeDB".earthquake.month_count order by month;
select * from "earthquakeDB".earthquake.month_count order by month DESC;
select * from "earthquakeDB".earthquake.month_count order by count DESC;
select count(*) from "earthquakeDB".earthquake.month_count

select * from "earthquakeDB".earthquake.day_count order by day;
select * from "earthquakeDB".earthquake.day_count order by day DESC;
select * from "earthquakeDB".earthquake.day_count where day like '1910%' order by day;
select count(*) from "earthquakeDB".earthquake.day_count;

select * from "earthquakeDB".earthquake.earthquake order by date_time;
select * from "earthquakeDB".earthquake.earthquake order by date_time DESC;
select id, code, date_time, mag, type, title, place from "earthquakeDB".earthquake.earthquake order by date_time DESC;
select id, code, date_time, mag, type, title, place from "earthquakeDB".earthquake.earthquake where type != 'earthquake';
select id, code, date_time, mag, type, title, place from "earthquakeDB".earthquake.earthquake where mag notnull order by mag DESC;
select count(*) from "earthquakeDB".earthquake.earthquake;
select count(*) from "earthquakeDB".earthquake.earthquake where mag >= 8;

SELECT date_time FROM "earthquakeDB".earthquake.earthquake
                 WHERE date_time !~ '^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$' LIMIT 20;


/***********************************************************************
    Init scripts
************************************************************************/
BEGIN;
--     CREATE SCHEMA IF NOT EXISTS earthquake;


--     CREATE TABLE earthquake.day_count (
--         id           SERIAL PRIMARY KEY,
--         day          VARCHAR(10) UNIQUE NOT NULL,
--         count        INTEGER,
--         created_date DATE DEFAULT CURRENT_DATE
--     );

--     CREATE TABLE earthquake.month_count (
--         id           SERIAL PRIMARY KEY,
--         month        VARCHAR(10) UNIQUE NOT NULL,
--         count        INTEGER,
--         created_date DATE DEFAULT CURRENT_DATE
--     );

-- drop table earthquake.earthquake;

-- CREATE TABLE earthquake.earthquake
-- (
--     id              SERIAL PRIMARY KEY,
--     feature_id      VARCHAR(50) UNIQUE NOT NULL,
--     coordinate_type VARCHAR(10),
--     coordinates     VARCHAR(255),
--     code            VARCHAR(255),
--     ids             VARCHAR(255),
--     title           VARCHAR(511),
--     place           VARCHAR(511),
--     date_time       VARCHAR(100),
--     updated_date    VARCHAR(100),
--     tz              VARCHAR(100),
--     type            VARCHAR(255),
--     types           VARCHAR(511),
--     tsunami         INTEGER,
--     sig             INTEGER,
--     detail          VARCHAR(511),
--     url             VARCHAR(511),
--     mag             DOUBLE PRECISION,
--     mag_type        VARCHAR(255),
--     status          VARCHAR(255),
--     sources         VARCHAR(255),
--     net             VARCHAR(255),
--     felt            VARCHAR(255),
--     cdi             VARCHAR(255),
--     mmi             VARCHAR(255),
--     alert           VARCHAR(255),
--     nst             INTEGER,
--     gap             DOUBLE PRECISION,
--     dmin            DOUBLE PRECISION,
--     rms             DOUBLE PRECISION,
--     created_date    DATE DEFAULT CURRENT_DATE
-- );

-- ALTER TABLE earthquake.earthquake ALTER COLUMN types TYPE VARCHAR(511);

-- ALTER TABLE earthquake.earthquake ALTER COLUMN rms TYPE DOUBLE PRECISION;


ROLLBACK









