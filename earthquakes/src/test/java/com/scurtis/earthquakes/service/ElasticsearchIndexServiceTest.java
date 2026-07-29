package com.scurtis.earthquakes.service;

import com.scurtis.earthquakes.entity.Earthquake;
import com.scurtis.earthquakes.exception.ElasticsearchIndexException;
import com.scurtis.earthquakes.repository.EarthquakeRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.scurtis.earthquakes.common.AppConstants.ES_INDEX_NAME;
import static com.scurtis.earthquakes.common.AppConstants.ES_LOAD_BATCH_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElasticsearchIndexServiceTest {

    @Mock
    private ReactiveElasticsearchOperations reactiveElasticsearchOperations;

    @Mock
    private ReactiveIndexOperations indexOperations;

    @Mock
    private EarthquakeRepository earthquakeRepository;

    @InjectMocks
    private ElasticsearchIndexService elasticsearchIndexService;

    @Test
    void recreateIndex_whenIndexExists_deletesThenRecreatesItAndLoadsData() {
        when(reactiveElasticsearchOperations.indexOps(any(IndexCoordinates.class))).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(Mono.just(true));
        when(indexOperations.delete()).thenReturn(Mono.just(true));
        when(indexOperations.create()).thenReturn(Mono.just(true));
        when(indexOperations.putMapping(any(Mono.class))).thenReturn(Mono.just(true));
        List<Earthquake> earthquakes = List.of(new Earthquake(), new Earthquake());
        when(earthquakeRepository.findAll()).thenReturn(Flux.fromIterable(earthquakes));
        when(reactiveElasticsearchOperations.save(any(Flux.class), eq(IndexCoordinates.of(ES_INDEX_NAME)), eq(ES_LOAD_BATCH_SIZE)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        String result = elasticsearchIndexService.recreateESIndex().block();

        verify(indexOperations, times(1)).delete();
        verify(indexOperations, times(1)).create();
        verify(indexOperations, times(1)).putMapping(any(Mono.class));
        verify(reactiveElasticsearchOperations, times(1)).save(any(Flux.class), eq(IndexCoordinates.of(ES_INDEX_NAME)), eq(ES_LOAD_BATCH_SIZE));
        assertThat(result).contains(ES_INDEX_NAME).contains("2");
    }

    @Test
    void recreateIndex_whenIndexDoesNotExist_skipsDeleteAndCreatesIt() {
        when(reactiveElasticsearchOperations.indexOps(any(IndexCoordinates.class))).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(Mono.just(false));
        when(indexOperations.create()).thenReturn(Mono.just(true));
        when(indexOperations.putMapping(any(Mono.class))).thenReturn(Mono.just(true));
        when(earthquakeRepository.findAll()).thenReturn(Flux.empty());
        when(reactiveElasticsearchOperations.save(any(Flux.class), eq(IndexCoordinates.of(ES_INDEX_NAME)), eq(ES_LOAD_BATCH_SIZE)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        String result = elasticsearchIndexService.recreateESIndex().block();

        verify(indexOperations, never()).delete();
        verify(indexOperations, times(1)).create();
        verify(indexOperations, times(1)).putMapping(any(Mono.class));
        assertThat(result).contains(ES_INDEX_NAME).contains("0");
    }

    @Test
    void recreateIndex_whenIndexOperationFails_throwsElasticsearchIndexException() {
        when(reactiveElasticsearchOperations.indexOps(any(IndexCoordinates.class))).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(Mono.error(new RuntimeException("boom")));

        assertThatThrownBy(() -> elasticsearchIndexService.recreateESIndex().block())
            .isInstanceOf(ElasticsearchIndexException.class)
            .hasMessageContaining(ES_INDEX_NAME);
    }

}
