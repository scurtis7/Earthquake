# Earthquakes

Earthquakes will handle making calls to the USGS site for earthquakes and
save the data locally so other calls and analytics can be done.

This data comes from the USGS [Earthquake Hazards Program](https://earthquake.usgs.gov/fdsnws/event/1/).

Here is an explanation of the [data](https://earthquake.usgs.gov/data/comcat/index.php).

## Stack

Earthquakes application is built on top of a few core technologies:

* Java 21
* [Maven](https://maven.apache.org) (3.0.0+)
* [Spring Boot](https://spring.io/projects/spring-boot) (4.0.0+)
* [Elastic](https://www.elastic.co/docs/reference/elasticsearch/rest-apis) (9.2.8+)

# Technologies

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?logo=springboot&logoColor=white&style=plastic)
![Java](https://img.shields.io/badge/Java-ED8B00?style=plastic&logo=java&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white&style=plastic)
![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?logo=apachemaven&logoColor=white&style=plastic)
![Elastic](https://img.shields.io/badge/Elastic-005571?logo=elastic&logoColor=white&style=plastic)


<!--
  https://badges.pages.dev
  https://github.com/alexandresanlim/Badges4-README.md-Profile#%E2%80%8D-static 
-->

## Build

To build the Earthquakes application without running tests:

`mvn clean compile`

To build the Earthquakes application and run tests:

`mvn clean test`

To build the Earthquakes application and generate code coverage:

`mvn clean verify`

## Elasticsearch & Kibana

Elasticsearch and Kibana run locally via Docker Compose (`docker-compose.yml` at the repo root).
Postgres is not included here — it's expected to be installed locally rather than containerized.

Start both services:

```
docker compose up -d
```

* Elasticsearch: [http://localhost:9200](http://localhost:9200)
* Kibana: [http://localhost:5601](http://localhost:5601) (use **Dev Tools** under Management to
  query Elasticsearch directly)

To stop them:

```
docker compose down
```

The `es-data` volume persists across `docker compose down`/`up`, so indexed data survives restarts.
Elasticsearch and Kibana are pinned to the same version in `docker-compose.yml` — keep both tags
in sync if you ever bump the version, since a Kibana/Elasticsearch version mismatch (or a client
library major-version mismatch after a Spring Boot upgrade) will break the connection.

## Environment

In order to run the Earthquakes application, the following environment variables must be set:

| VARIABLE                   | Description                          |
|:---------------------------|:-------------------------------------|
| SPRING_DATASOURCE_URL      | The URL to the Postgresql database   |
| SPRING_DATASOURCE_USERNAME | Username for the Postgresql database |
| SPRING_DATASOURCE_PASSWORD | Password for the Postgresql database |

## Endpoints

A brief description of the endpoints can be found in this section.

### REST Endpoint

This endpoint will be called to...

| Method | Url                    | Parameters          | Description                                                                                 |
|:-------|:-----------------------|:--------------------|:--------------------------------------------------------------------------------------------|
| GET    | .../earthquakes/       | year=YYYY, month=MM | Calls the USGS site to get earthquakes for that year and month and loads it to the database |
| GET    | .../earthquakes/range  | year=YYYY           | Reads from the database and returns earthquakes for that year                               |
| GET    | .../earthquakes/counts |                     | Reads and returns everything in the database at the moment.  (need to change this)          |

### Health Check Endpoint

To verify the app is up and running you can call the health endpoint below

| Method | Url                  | Description                                      |
|:-------|:---------------------|:-------------------------------------------------|
| GET    | .../actuator/health/ | Returns `up` or `down` status of the application |
