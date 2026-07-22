package com.scurtis.earthquakes.controller;

import com.scurtis.earthquakes.exception.ElasticsearchIndexException;
import com.scurtis.earthquakes.service.EarthquakeService;
import com.scurtis.earthquakes.service.ElasticsearchIndexService;
import com.scurtis.earthquakes.service.ValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(EarthquakeController.class)
class EarthquakeControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private EarthquakeService earthquakeService;

    @MockitoBean
    private ValidationService validationService;

    @MockitoBean
    private ElasticsearchIndexService elasticsearchIndexService;

    @Test
    void recreateEarthquakeIndex_returnsServiceResult() {
        when(elasticsearchIndexService.recreateIndex()).thenReturn(Mono.just("Index 'earthquake_index' has been recreated"));

        webTestClient.put().uri("/earthquake/index")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .isEqualTo("Index 'earthquake_index' has been recreated");

        verify(elasticsearchIndexService).recreateIndex();
    }

    @Test
    void recreateEarthquakeIndex_whenServiceThrows_returns500() {
        when(elasticsearchIndexService.recreateIndex()).thenReturn(Mono.error(new ElasticsearchIndexException("boom")));

        webTestClient.put().uri("/earthquake/index")
            .exchange()
            .expectStatus().is5xxServerError();
    }

}
