package com.scurtis.earthquakes.repository;

import com.scurtis.earthquakes.entity.DayCount;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DayCountRepository extends ReactiveCrudRepository<DayCount, Integer> {

    Mono<DayCount> findByDay(String day);

    Flux<DayCount> findByDayBetweenOrderByDay(String fromDate, String toDate);

//    @Query("SELECT * FROM earthquake.DayCount ORDER BY day")
//    Flux<DayCount> findAll();
//
//    @Query("SELECT * FROM earthquake.DayCount WHERE day LIKE '?1'")
//    Flux<DayCount> findSumByYear(String year);

}
