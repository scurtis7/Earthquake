package com.scurtis.earthquakes.service;

import com.scurtis.earthquakes.exception.ElasticsearchIndexException;
import com.scurtis.earthquakes.repository.EarthquakeRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    public Mono<String> recreateIndex() {
        return Mono.defer(() -> {
            log.info("ElasticsearchIndexService -> recreateIndex(index:{})", ES_INDEX_NAME);
            StopWatch stopWatch = new StopWatch("ElasticsearchIndexService.recreateIndex");
            stopWatch.start();
            ReactiveIndexOperations indexOps = reactiveElasticsearchOperations.indexOps(IndexCoordinates.of(ES_INDEX_NAME));
            Document mapping = loadMapping();
            return recreateIndex(indexOps, mapping)
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

    private Mono<Boolean> recreateIndex(ReactiveIndexOperations indexOps, Document mapping) {
        return indexOps.exists()
            .flatMap(exists -> exists ? indexOps.delete() : Mono.just(false))
            .then(Mono.defer(indexOps::create))
            .then(Mono.defer(() -> indexOps.putMapping(Mono.just(mapping))));
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
