package com.scurtis.earthquakes.repository;

import com.scurtis.earthquakes.entity.MonthCount;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MonthCountRepository extends ReactiveCrudRepository<MonthCount, Integer> {

    Mono<MonthCount> findByMonth(String month);

    Flux<MonthCount> findByMonthBetweenOrderByMonth(String fromDate, String toDate);

}
