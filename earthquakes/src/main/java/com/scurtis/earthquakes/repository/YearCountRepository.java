package com.scurtis.earthquakes.repository;

import com.scurtis.earthquakes.entity.YearCount;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface YearCountRepository extends ReactiveCrudRepository<YearCount, Integer> {

    Mono<YearCount> findByYear(String year);

    Flux<YearCount> findByYearBetweenOrderByYear(String fromDate, String toDate);

}
