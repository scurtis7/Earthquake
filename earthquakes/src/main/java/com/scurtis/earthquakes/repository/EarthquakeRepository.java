package com.scurtis.earthquakes.repository;

import com.scurtis.earthquakes.entity.Earthquake;
import com.scurtis.earthquakes.entity.PeriodCount;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface EarthquakeRepository extends ReactiveCrudRepository<Earthquake, Integer> {

    Mono<Earthquake> findByFeatureId(String featureId);

    @Query(value =
        "SELECT LEFT(date_time, :length) AS period, COUNT(*) AS count"
            + "    FROM earthquake.earthquake"
            + "    WHERE type = 'earthquake'"
            + "        AND date_time >= :startDate"
            + "        AND date_time <  :endDate"
            + "    GROUP BY LEFT(date_time, :length)"
            + "    ORDER BY period")
    Flux<PeriodCount> getEarthquakeCountsByPeriod(int length, String startDate, String endDate);

}
