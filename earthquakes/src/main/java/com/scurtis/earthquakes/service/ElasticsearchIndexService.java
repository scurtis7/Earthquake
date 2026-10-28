package com.scurtis.earthquakes.service;

import com.scurtis.earthquakes.entity.Earthquake;
import com.scurtis.earthquakes.exception.ElasticsearchIndexException;
import com.scurtis.earthquakes.repository.EarthquakeRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.scurtis.earthquakes.common.AppConstants.ES_INDEX_NAME;
import static com.scurtis.earthquakes.common.AppConstants.ES_LOAD_BATCH_SIZE;
import static com.scurtis.earthquakes.common.AppConstants.ES_MAPPING_RESOURCE_PATH;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchIndexService {

    private final ReactiveElasticsearchOperations reactiveElasticsearchOperations;
    private final EarthquakeRepository earthquakeRepository;

    public Mono<String> recreateESIndex() {
        return Mono.defer(() -> {
            log.info("ElasticsearchIndexService -> recreateIndex(index:{})", ES_INDEX_NAME);
            StopWatch stopWatch = new StopWatch("ElasticsearchIndexService.recreateIndex");
            stopWatch.start();
            ReactiveIndexOperations indexOps = reactiveElasticsearchOperations.indexOps(IndexCoordinates.of(ES_INDEX_NAME));
            Document mapping = loadMapping();
            return recreateESIndex(indexOps, mapping)
                .then(Mono.defer(this::loadData))
                .map(loaded -> String.format("Index '%s' has been recreated and %d record(s) have been loaded", ES_INDEX_NAME, loaded))
                .doOnNext(log::debug)
                .doFinally(signal -> {
                    stopWatch.stop();
                    log.debug(stopWatch.shortSummary());
                })
                .onErrorMap(exception -> !(exception instanceof ElasticsearchIndexException),
                    exception -> new ElasticsearchIndexException("Failed to recreate index '" + ES_INDEX_NAME + "'", exception));
        });
    }

    private Mono<Boolean> recreateESIndex(ReactiveIndexOperations indexOps, Document mapping) {
        return indexOps.exists()
            .flatMap(exists -> exists ? indexOps.delete() : Mono.just(false))
            .then(Mono.defer(indexOps::create))
            .then(Mono.defer(() -> indexOps.putMapping(Mono.just(mapping))));
    }

    /**
     * Upserts the given rows into the index (documents use the Postgres id as _id), creating the
     * index from the mapping first if it's missing so the write doesn't fall back to dynamic mapping.
     */
    public Mono<Long> indexEarthquakes(List<Earthquake> earthquakes) {
        if (earthquakes.isEmpty()) {
            return Mono.just(0L);
        }
        return Mono.defer(() -> {
            log.debug("ElasticsearchIndexService -> indexEarthquakes(count:{})", earthquakes.size());
            ReactiveIndexOperations indexOps = reactiveElasticsearchOperations.indexOps(IndexCoordinates.of(ES_INDEX_NAME));
            return createESIndexIfMissing(indexOps)
                .then(Mono.defer(() -> reactiveElasticsearchOperations
                    .save(Flux.fromIterable(earthquakes), IndexCoordinates.of(ES_INDEX_NAME), ES_LOAD_BATCH_SIZE)
                    .count()))
                .onErrorMap(exception -> !(exception instanceof ElasticsearchIndexException),
                    exception -> new ElasticsearchIndexException("Failed to index earthquakes into '" + ES_INDEX_NAME + "'", exception));
        });
    }

    /**
     * Create the index and mapping in one call, so a concurrent write can't slip in between them
     * and dynamically map the fields; if another save created it first, the create fails but the
     * index now exists.
     */
    private Mono<Boolean> createESIndexIfMissing(ReactiveIndexOperations indexOps) {
        return indexOps.exists()
            .flatMap(exists -> exists ? Mono.just(true) : indexOps.create(Document.create(), loadMapping())
                .onErrorResume(exception -> indexOps.exists()
                    .filter(Boolean::booleanValue)
                    .switchIfEmpty(Mono.error(exception))));
    }

    private Mono<Long> loadData() {
        return reactiveElasticsearchOperations.save(earthquakeRepository.findAll(), IndexCoordinates.of(ES_INDEX_NAME), ES_LOAD_BATCH_SIZE)
            .count();
    }

    private Document loadMapping() {
        try {
            String json = StreamUtils.copyToString(new ClassPathResource(ES_MAPPING_RESOURCE_PATH).getInputStream(), StandardCharsets.UTF_8);
            return Document.parse(json);
        } catch (IOException exception) {
            throw new ElasticsearchIndexException("Failed to load Elasticsearch mapping resource '" + ES_MAPPING_RESOURCE_PATH + "'", exception);
        }
    }

}
