package com.scurtis.earthquakes.repository;


import com.scurtis.earthquakes.entity.Earthquake;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface EarthquakeRepository extends ReactiveCrudRepository<Earthquake, Integer> {

    Mono<Earthquake> findByFeatureId(String featureId);

}
