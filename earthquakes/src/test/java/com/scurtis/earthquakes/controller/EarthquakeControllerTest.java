package com.scurtis.earthquakes.controller;

import com.scurtis.earthquakes.dto.EarthquakeCountDto;
import com.scurtis.earthquakes.exception.ElasticsearchIndexException;
import com.scurtis.earthquakes.exception.ValidationException;
import com.scurtis.earthquakes.model.FeatureCollection;
import com.scurtis.earthquakes.model.Metadata;
import com.scurtis.earthquakes.service.EarthquakeService;
import com.scurtis.earthquakes.service.ElasticsearchIndexService;
import com.scurtis.earthquakes.service.ValidationService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static com.scurtis.earthquakes.TestUtils.FROM_DATE;
import static com.scurtis.earthquakes.TestUtils.INVALID_DATE_MSG;
import static com.scurtis.earthquakes.TestUtils.TO_DATE;
import static com.scurtis.earthquakes.TestUtils.USGS_DOWN_MSG;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_DAY;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_YEAR;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest(EarthquakeController.class)
public class EarthquakeControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private EarthquakeService earthquakeService;

    @MockitoBean
    private ValidationService validationService;

    @MockitoBean
    private ElasticsearchIndexService elasticsearchIndexService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(earthquakeService, validationService, elasticsearchIndexService);
    }

    @Test
    void getUSGSRawData_returnsServiceResult() {
        Metadata metadata = new Metadata();
        metadata.setStatus(200);
        metadata.setCount(0);
        FeatureCollection featureCollection = new FeatureCollection();
        featureCollection.setType("FeatureCollection");
        featureCollection.setMetadata(metadata);
        featureCollection.setFeatures(List.of());
        when(earthquakeService.getUSGSRawData(FROM_DATE, TO_DATE)).thenReturn(Mono.just(featureCollection));

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/earthquake/raw")
                .queryParam("fromDate", FROM_DATE)
                .queryParam("toDate", TO_DATE)
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody(FeatureCollection.class)
            .isEqualTo(featureCollection);

        verify(earthquakeService).getUSGSRawData(FROM_DATE, TO_DATE);
    }

    @Test
    void getUSGSRawData_whenServiceErrors_returns500() {
        when(earthquakeService.getUSGSRawData(FROM_DATE, TO_DATE)).thenReturn(Mono.error(new RuntimeException(USGS_DOWN_MSG)));

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/earthquake/raw")
                .queryParam("fromDate", FROM_DATE)
                .queryParam("toDate", TO_DATE)
                .build())
            .exchange()
            .expectStatus().is5xxServerError();

        verify(earthquakeService).getUSGSRawData(FROM_DATE, TO_DATE);
    }

    @Test
    void getUSGSRawData_whenParamMissing_returns400() {
        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/earthquake/raw")
                .queryParam("fromDate", FROM_DATE)
                .build())
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void saveUSGSRawData_validatesAndReturnsServiceResult() {
        String message = "Saved 2 records";
        when(earthquakeService.saveUSGSRawData(FROM_DATE, TO_DATE)).thenReturn(message);

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/earthquake/raw/save")
                .queryParam("fromDate", FROM_DATE)
                .queryParam("toDate", TO_DATE)
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .isEqualTo(message);

        verify(validationService).validateDateInputs(FROM_DATE, TO_DATE);
        verify(earthquakeService).saveUSGSRawData(FROM_DATE, TO_DATE);
    }

    @Test
    void saveUSGSRawData_whenValidationFails_returns400() {
        doThrow(new ValidationException(INVALID_DATE_MSG)).when(validationService).validateDateInputs(FROM_DATE, TO_DATE);

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/earthquake/raw/save")
                .queryParam("fromDate", FROM_DATE)
                .queryParam("toDate", TO_DATE)
                .build())
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.code").isEqualTo(400)
            .jsonPath("$.exception").isEqualTo("ValidationException")
            .jsonPath("$.message").isEqualTo(INVALID_DATE_MSG);

        verify(validationService).validateDateInputs(FROM_DATE, TO_DATE);
    }

    @Test
    void saveUSGSRawDataFromStartDate_validatesAndReturnsSuccess() {
        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/earthquake/raw/save/from")
                .queryParam("startDate", FROM_DATE)
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .isEqualTo("Success");

        verify(validationService).validateStartDate(FROM_DATE);
        verify(earthquakeService).saveUSGSRawDataFromStartDate(FROM_DATE);
    }

    @Test
    void saveUSGSRawDataFromStartDate_whenValidationFails_returns400() {
        doThrow(new ValidationException(INVALID_DATE_MSG)).when(validationService).validateStartDate(FROM_DATE);

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/earthquake/raw/save/from")
                .queryParam("startDate", FROM_DATE)
                .build())
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.message").isEqualTo(INVALID_DATE_MSG);

        verify(validationService).validateStartDate(FROM_DATE);
    }

    @Test
    void saveEarthquakeCounts_validatesSavesAndReturnsMessage() {
        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/earthquake/counts/save")
                .queryParam("period", PERIOD_DAY)
                .queryParam("fromDate", FROM_DATE)
                .queryParam("toDate", TO_DATE)
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .isEqualTo("Counts have been saved for the '" + PERIOD_DAY + "' period from '" + FROM_DATE + "' to '" + TO_DATE + "'");

        verify(validationService).validateInputs(PERIOD_DAY, FROM_DATE, TO_DATE);
        verify(earthquakeService).saveEarthquakeCounts(PERIOD_DAY, FROM_DATE, TO_DATE);
    }

    @Test
    void saveEarthquakeCounts_whenValidationFails_returns400() {
        String message = "Invalid period";
        doThrow(new ValidationException(message)).when(validationService).validateInputs("week", FROM_DATE, TO_DATE);

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/earthquake/counts/save")
                .queryParam("period", "week")
                .queryParam("fromDate", FROM_DATE)
                .queryParam("toDate", TO_DATE)
                .build())
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.message").isEqualTo(message);

        verify(validationService).validateInputs("week", FROM_DATE, TO_DATE);
    }

    @Test
    void getEarthquakeCounts_validatesAndReturnsServiceResult() {
        List<EarthquakeCountDto> counts = List.of(
            new EarthquakeCountDto(PERIOD_YEAR, "2023", 10),
            new EarthquakeCountDto(PERIOD_YEAR, "2024", 20));
        when(earthquakeService.getEarthquakeCounts(PERIOD_YEAR, FROM_DATE, TO_DATE)).thenReturn(Mono.just(counts));

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/earthquake/counts")
                .queryParam("period", PERIOD_YEAR)
                .queryParam("fromDate", FROM_DATE)
                .queryParam("toDate", TO_DATE)
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(EarthquakeCountDto.class)
            .isEqualTo(counts);

        verify(validationService).validateDateInputs(FROM_DATE, TO_DATE);
        verify(earthquakeService).getEarthquakeCounts(PERIOD_YEAR, FROM_DATE, TO_DATE);
    }

    @Test
    void getEarthquakeCounts_whenValidationFails_returns400() {
        doThrow(new ValidationException(INVALID_DATE_MSG)).when(validationService).validateDateInputs(FROM_DATE, TO_DATE);

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/earthquake/counts")
                .queryParam("period", PERIOD_YEAR)
                .queryParam("fromDate", FROM_DATE)
                .queryParam("toDate", TO_DATE)
                .build())
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.message").isEqualTo(INVALID_DATE_MSG);

        verify(validationService).validateDateInputs(FROM_DATE, TO_DATE);
    }

    @Test
    void recreateEarthquakeIndex_returnsServiceResult() {
        when(elasticsearchIndexService.recreateESIndex()).thenReturn(Mono.just("Index 'earthquake_index' has been recreated"));

        webTestClient.put().uri("/earthquake/es-index")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .isEqualTo("Index 'earthquake_index' has been recreated");

        verify(elasticsearchIndexService).recreateESIndex();
    }

    @Test
    void recreateEarthquakeIndex_whenServiceThrows_returns500() {
        when(elasticsearchIndexService.recreateESIndex()).thenReturn(Mono.error(new ElasticsearchIndexException("boom")));

        webTestClient.put().uri("/earthquake/es-index")
            .exchange()
            .expectStatus().is5xxServerError()
            .expectBody()
            .jsonPath("$.code").isEqualTo(500)
            .jsonPath("$.exception").isEqualTo("ElasticsearchIndexException")
            .jsonPath("$.message").isEqualTo("boom");

        verify(elasticsearchIndexService).recreateESIndex();
    }

}
