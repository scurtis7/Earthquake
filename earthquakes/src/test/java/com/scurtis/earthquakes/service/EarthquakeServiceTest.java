package com.scurtis.earthquakes.service;

import com.scurtis.earthquakes.converter.EarthquakeConverter;
import com.scurtis.earthquakes.dto.EarthquakeCountDto;
import com.scurtis.earthquakes.entity.DayCount;
import com.scurtis.earthquakes.entity.Earthquake;
import com.scurtis.earthquakes.entity.MonthCount;
import com.scurtis.earthquakes.entity.PeriodCount;
import com.scurtis.earthquakes.entity.YearCount;
import com.scurtis.earthquakes.exception.ValidationException;
import com.scurtis.earthquakes.model.Feature;
import com.scurtis.earthquakes.model.FeatureCollection;
import com.scurtis.earthquakes.model.Metadata;
import com.scurtis.earthquakes.repository.DayCountRepository;
import com.scurtis.earthquakes.repository.EarthquakeRepository;
import com.scurtis.earthquakes.repository.MonthCountRepository;
import com.scurtis.earthquakes.repository.YearCountRepository;
import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.scurtis.earthquakes.TestUtils.EXPECTED_QUERY_URI;
import static com.scurtis.earthquakes.TestUtils.FEATURE_ID_EXISTING;
import static com.scurtis.earthquakes.TestUtils.FEATURE_ID_NEW;
import static com.scurtis.earthquakes.TestUtils.FROM_DATE;
import static com.scurtis.earthquakes.TestUtils.TO_DATE;
import static com.scurtis.earthquakes.TestUtils.USGS_DOWN_MSG;
import static com.scurtis.earthquakes.TestUtils.getExistingEarthquake;
import static com.scurtis.earthquakes.TestUtils.getExistingFeature;
import static com.scurtis.earthquakes.TestUtils.getNewEarthquake;
import static com.scurtis.earthquakes.TestUtils.getNewFeature;
import static com.scurtis.earthquakes.common.AppConstants.DATE_FORMATTER;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_DAY;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_LENGTH_DAY;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_LENGTH_MONTH;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_LENGTH_YEAR;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_MONTH;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_YEAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class EarthquakeServiceTest {

    private EarthquakeService earthquakeService;

    @Mock
    private WebClient webClientMock;
    @Mock
    private RestClient restClientMock;
    @Mock
    private YearCountRepository yearCountRepositoryMock;
    @Mock
    private MonthCountRepository monthCountRepositoryMock;
    @Mock
    private DayCountRepository dayCountRepositoryMock;
    @Mock
    private EarthquakeRepository earthquakeRepositoryMock;
    @Mock
    private EarthquakeConverter earthquakeConverterMock;
    @Mock
    private WebClient.RequestHeadersUriSpec webClientUriSpecMock;
    @Mock
    private WebClient.RequestHeadersSpec webClientHeadersSpecMock;
    @Mock
    private WebClient.ResponseSpec webClientResponseSpecMock;
    @Mock
    private RestClient.RequestHeadersUriSpec restClientUriSpecMock;
    @Mock
    private RestClient.RequestHeadersSpec restClientHeadersSpecMock;
    @Mock
    private RestClient.ResponseSpec restClientResponseSpecMock;

    @BeforeEach
    void setUp() {
        earthquakeService = spy(new EarthquakeService(webClientMock, restClientMock, yearCountRepositoryMock,
            monthCountRepositoryMock, dayCountRepositoryMock, earthquakeRepositoryMock, earthquakeConverterMock));
    }

    @AfterEach
    void tearDown() {
        Mockito.verifyNoMoreInteractions(webClientMock);
        Mockito.verifyNoMoreInteractions(restClientMock);
        Mockito.verifyNoMoreInteractions(yearCountRepositoryMock);
        Mockito.verifyNoMoreInteractions(monthCountRepositoryMock);
        Mockito.verifyNoMoreInteractions(dayCountRepositoryMock);
        Mockito.verifyNoMoreInteractions(earthquakeRepositoryMock);
        Mockito.verifyNoMoreInteractions(earthquakeConverterMock);
        Mockito.verifyNoMoreInteractions(earthquakeService);
    }

    @Test
    void getUSGSRawData_returnsFeatureCollectionFromUSGS() {
        FeatureCollection featureCollection = featureCollection(OK.value(), List.of());
        stubWebClientQuery(Mono.just(featureCollection));

        StepVerifier.create(earthquakeService.getUSGSRawData(FROM_DATE, TO_DATE))
            .expectNext(featureCollection)
            .verifyComplete();

        verify(earthquakeService).getUSGSRawData(FROM_DATE, TO_DATE);
        verify(webClientMock).get();
        assertThat(capturedWebClientUri()).isEqualTo(EXPECTED_QUERY_URI);
    }

    @Test
    void getUSGSRawData_whenUSGSCallFails_propagatesError() {
        stubWebClientQuery(Mono.error(new RuntimeException(USGS_DOWN_MSG)));

        StepVerifier.create(earthquakeService.getUSGSRawData(FROM_DATE, TO_DATE))
            .expectErrorMessage(USGS_DOWN_MSG)
            .verify();

        verify(earthquakeService).getUSGSRawData(FROM_DATE, TO_DATE);
        verify(webClientMock).get();
    }

    @Test
    void saveUSGSRawData_insertsNewFeaturesAndUpdatesExistingOnes() {
        Feature newFeature = getNewFeature();
        Feature existingFeature = getExistingFeature();
        stubRestClientQuery(featureCollection(OK.value(), List.of(newFeature, existingFeature)));

        Earthquake newEntity = getNewEarthquake();
        Earthquake existingEntity = getExistingEarthquake();
        when(earthquakeConverterMock.toEntity(newFeature)).thenReturn(newEntity);
        when(earthquakeConverterMock.toEntity(existingFeature)).thenReturn(existingEntity);
        when(earthquakeRepositoryMock.findByFeatureId(FEATURE_ID_NEW)).thenReturn(Mono.empty());
        when(earthquakeRepositoryMock.findByFeatureId(FEATURE_ID_EXISTING)).thenReturn(Mono.just(earthquake(42, FEATURE_ID_EXISTING)));
        when(earthquakeRepositoryMock.save(any(Earthquake.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        String result = earthquakeService.saveUSGSRawData(FROM_DATE, TO_DATE);

        assertThat(result).isEqualTo("Saved 2 records of raw earthquake data between the dates of '" + FROM_DATE + "' and '" + TO_DATE + "'");
        assertThat(newEntity.getId()).isNull();
        assertThat(existingEntity.getId()).isEqualTo(42);
        verify(earthquakeService).saveUSGSRawData(FROM_DATE, TO_DATE);
        verify(restClientMock).get();
        assertThat(capturedRestClientUri()).isEqualTo(EXPECTED_QUERY_URI);
        verify(earthquakeConverterMock).toEntity(newFeature);
        verify(earthquakeConverterMock).toEntity(existingFeature);
        verify(earthquakeRepositoryMock).findByFeatureId(FEATURE_ID_NEW);
        verify(earthquakeRepositoryMock).findByFeatureId(FEATURE_ID_EXISTING);
        verify(earthquakeRepositoryMock).save(newEntity);
        verify(earthquakeRepositoryMock).save(existingEntity);
    }

    @Test
    void saveUSGSRawData_whenResponseIsNull_throwsValidationException() {
        stubRestClientQuery(null);

        assertSaveUSGSRawDataThrows();
    }

    @Test
    void saveUSGSRawData_whenMetadataIsNull_throwsValidationException() {
        FeatureCollection featureCollection = new FeatureCollection();
        stubRestClientQuery(featureCollection);

        assertSaveUSGSRawDataThrows();
    }

    @Test
    void saveUSGSRawData_whenStatusIsNot200_throwsValidationException() {
        stubRestClientQuery(featureCollection(SERVICE_UNAVAILABLE.value(), List.of()));

        assertSaveUSGSRawDataThrows();
    }

    @Test
    void saveUSGSRawDataFromStartDate_savesEachMonthFromStartOfMonthOfStartDate() {
        LocalDate today = LocalDate.now();
        LocalDate firstMonth = today.minusMonths(3).withDayOfMonth(1);
        String startDate = firstMonth.plusDays(9).format(DATE_FORMATTER);
        int expectedMonths = (int) ChronoUnit.MONTHS.between(firstMonth, today.plusDays(1));
        doReturn("saved").when(earthquakeService).saveUSGSRawData(anyString(), anyString());

        earthquakeService.saveUSGSRawDataFromStartDate(startDate);

        verify(earthquakeService).saveUSGSRawDataFromStartDate(startDate);
        verify(earthquakeService, times(expectedMonths)).saveUSGSRawData(anyString(), anyString());
        verify(earthquakeService).saveUSGSRawData(firstMonth.format(DATE_FORMATTER), firstMonth.plusMonths(1).format(DATE_FORMATTER));
        verify(earthquakeService).saveUSGSRawData(firstMonth.plusMonths(1).format(DATE_FORMATTER), firstMonth.plusMonths(2).format(DATE_FORMATTER));
        verify(earthquakeService).saveUSGSRawData(firstMonth.plusMonths(2).format(DATE_FORMATTER), firstMonth.plusMonths(3).format(DATE_FORMATTER));
    }

    @Test
    void saveUSGSRawDataFromStartDate_whenStartDateIsInFuture_savesNothing() {
        String startDate = LocalDate.now().plusMonths(2).format(DATE_FORMATTER);

        earthquakeService.saveUSGSRawDataFromStartDate(startDate);

        verify(earthquakeService).saveUSGSRawDataFromStartDate(startDate);
    }

    @Test
    void saveEarthquakeCounts_year_insertsNewUpdatesChangedAndSkipsUnchanged() {
        when(earthquakeRepositoryMock.getEarthquakeCountsByPeriod(PERIOD_LENGTH_YEAR, FROM_DATE, TO_DATE))
            .thenReturn(Flux.just(periodCount("2022", 10), periodCount("2023", 20), periodCount("2024", 30)));
        YearCount changed = new YearCount(2, "2023", 15);
        YearCount unchanged = new YearCount(3, "2024", 30);
        when(yearCountRepositoryMock.findByYear("2022")).thenReturn(Mono.empty());
        when(yearCountRepositoryMock.findByYear("2023")).thenReturn(Mono.just(changed));
        when(yearCountRepositoryMock.findByYear("2024")).thenReturn(Mono.just(unchanged));
        when(yearCountRepositoryMock.save(any(YearCount.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        earthquakeService.saveEarthquakeCounts(PERIOD_YEAR, FROM_DATE, TO_DATE);

        verify(earthquakeService).saveEarthquakeCounts(PERIOD_YEAR, FROM_DATE, TO_DATE);
        verify(earthquakeRepositoryMock).getEarthquakeCountsByPeriod(PERIOD_LENGTH_YEAR, FROM_DATE, TO_DATE);
        verify(yearCountRepositoryMock).findByYear("2022");
        verify(yearCountRepositoryMock).findByYear("2023");
        verify(yearCountRepositoryMock).findByYear("2024");
        verify(yearCountRepositoryMock).save(new YearCount(null, "2022", 10));
        verify(yearCountRepositoryMock).save(new YearCount(2, "2023", 20));
    }

    @Test
    void saveEarthquakeCounts_month_insertsNewUpdatesChangedAndSkipsUnchanged() {
        when(earthquakeRepositoryMock.getEarthquakeCountsByPeriod(PERIOD_LENGTH_MONTH, FROM_DATE, TO_DATE))
            .thenReturn(Flux.just(periodCount("2024-01", 10), periodCount("2024-02", 20), periodCount("2024-03", 30)));
        MonthCount changed = new MonthCount(2, "2024-02", 15);
        MonthCount unchanged = new MonthCount(3, "2024-03", 30);
        when(monthCountRepositoryMock.findByMonth("2024-01")).thenReturn(Mono.empty());
        when(monthCountRepositoryMock.findByMonth("2024-02")).thenReturn(Mono.just(changed));
        when(monthCountRepositoryMock.findByMonth("2024-03")).thenReturn(Mono.just(unchanged));
        when(monthCountRepositoryMock.save(any(MonthCount.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        earthquakeService.saveEarthquakeCounts("MONTH", FROM_DATE, TO_DATE);

        verify(earthquakeService).saveEarthquakeCounts("MONTH", FROM_DATE, TO_DATE);
        verify(earthquakeRepositoryMock).getEarthquakeCountsByPeriod(PERIOD_LENGTH_MONTH, FROM_DATE, TO_DATE);
        verify(monthCountRepositoryMock).findByMonth("2024-01");
        verify(monthCountRepositoryMock).findByMonth("2024-02");
        verify(monthCountRepositoryMock).findByMonth("2024-03");
        verify(monthCountRepositoryMock).save(new MonthCount(null, "2024-01", 10));
        verify(monthCountRepositoryMock).save(new MonthCount(2, "2024-02", 20));
    }

    @Test
    void saveEarthquakeCounts_day_insertsNewUpdatesChangedAndSkipsUnchanged() {
        when(earthquakeRepositoryMock.getEarthquakeCountsByPeriod(PERIOD_LENGTH_DAY, FROM_DATE, TO_DATE))
            .thenReturn(Flux.just(periodCount("2024-01-15", 10), periodCount("2024-01-16", 20), periodCount("2024-01-17", 30)));
        DayCount changed = new DayCount(2, "2024-01-16", 15);
        DayCount unchanged = new DayCount(3, "2024-01-17", 30);
        when(dayCountRepositoryMock.findByDay("2024-01-15")).thenReturn(Mono.empty());
        when(dayCountRepositoryMock.findByDay("2024-01-16")).thenReturn(Mono.just(changed));
        when(dayCountRepositoryMock.findByDay("2024-01-17")).thenReturn(Mono.just(unchanged));
        when(dayCountRepositoryMock.save(any(DayCount.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        earthquakeService.saveEarthquakeCounts(PERIOD_DAY, FROM_DATE, TO_DATE);

        verify(earthquakeService).saveEarthquakeCounts(PERIOD_DAY, FROM_DATE, TO_DATE);
        verify(earthquakeRepositoryMock).getEarthquakeCountsByPeriod(PERIOD_LENGTH_DAY, FROM_DATE, TO_DATE);
        verify(dayCountRepositoryMock).findByDay("2024-01-15");
        verify(dayCountRepositoryMock).findByDay("2024-01-16");
        verify(dayCountRepositoryMock).findByDay("2024-01-17");
        verify(dayCountRepositoryMock).save(new DayCount(null, "2024-01-15", 10));
        verify(dayCountRepositoryMock).save(new DayCount(2, "2024-01-16", 20));
    }

    @Test
    void saveEarthquakeCounts_whenNoCountsFound_savesNothing() {
        when(earthquakeRepositoryMock.getEarthquakeCountsByPeriod(PERIOD_LENGTH_DAY, FROM_DATE, TO_DATE)).thenReturn(Flux.empty());

        earthquakeService.saveEarthquakeCounts(PERIOD_DAY, FROM_DATE, TO_DATE);

        verify(earthquakeService).saveEarthquakeCounts(PERIOD_DAY, FROM_DATE, TO_DATE);
        verify(earthquakeRepositoryMock).getEarthquakeCountsByPeriod(PERIOD_LENGTH_DAY, FROM_DATE, TO_DATE);
    }

    // ---------- getEarthquakeCounts ----------

    @Test
    void getEarthquakeCounts_year_queriesByYearAndConvertsToDtos() {
        YearCount first = new YearCount(1, "2023", 10);
        YearCount second = new YearCount(2, "2024", 20);
        EarthquakeCountDto firstDto = new EarthquakeCountDto(PERIOD_YEAR, "2023", 10);
        EarthquakeCountDto secondDto = new EarthquakeCountDto(PERIOD_YEAR, "2024", 20);
        when(yearCountRepositoryMock.findByYearBetweenOrderByYear("2023", "2024")).thenReturn(Flux.just(first, second));
        when(earthquakeConverterMock.toDto(first)).thenReturn(firstDto);
        when(earthquakeConverterMock.toDto(second)).thenReturn(secondDto);

        StepVerifier.create(earthquakeService.getEarthquakeCounts(PERIOD_YEAR, "2023-05-01", "2024-03-01"))
            .expectNext(List.of(firstDto, secondDto))
            .verifyComplete();

        verify(earthquakeService).getEarthquakeCounts(PERIOD_YEAR, "2023-05-01", "2024-03-01");
        verify(yearCountRepositoryMock).findByYearBetweenOrderByYear("2023", "2024");
        verify(earthquakeConverterMock).toDto(first);
        verify(earthquakeConverterMock).toDto(second);
    }

    @Test
    void getEarthquakeCounts_month_queriesByMonthAndConvertsToDtos() {
        MonthCount monthCount = new MonthCount(1, "2023-05", 10);
        EarthquakeCountDto dto = new EarthquakeCountDto(PERIOD_MONTH, "2023-05", 10);
        when(monthCountRepositoryMock.findByMonthBetweenOrderByMonth("2023-05", "2024-03")).thenReturn(Flux.just(monthCount));
        when(earthquakeConverterMock.toDto(monthCount)).thenReturn(dto);

        StepVerifier.create(earthquakeService.getEarthquakeCounts(PERIOD_MONTH, "2023-05-01", "2024-03-01"))
            .expectNext(List.of(dto))
            .verifyComplete();

        verify(earthquakeService).getEarthquakeCounts(PERIOD_MONTH, "2023-05-01", "2024-03-01");
        verify(monthCountRepositoryMock).findByMonthBetweenOrderByMonth("2023-05", "2024-03");
        verify(earthquakeConverterMock).toDto(monthCount);
    }

    @Test
    void getEarthquakeCounts_day_queriesByDayAndConvertsToDtos() {
        DayCount dayCount = new DayCount(1, "2023-05-01", 10);
        EarthquakeCountDto dto = new EarthquakeCountDto(PERIOD_DAY, "2023-05-01", 10);
        when(dayCountRepositoryMock.findByDayBetweenOrderByDay("2023-05-01", "2023-05-31")).thenReturn(Flux.just(dayCount));
        when(earthquakeConverterMock.toDto(dayCount)).thenReturn(dto);

        StepVerifier.create(earthquakeService.getEarthquakeCounts(PERIOD_DAY, "2023-05-01", "2023-05-31"))
            .expectNext(List.of(dto))
            .verifyComplete();

        verify(earthquakeService).getEarthquakeCounts(PERIOD_DAY, "2023-05-01", "2023-05-31");
        verify(dayCountRepositoryMock).findByDayBetweenOrderByDay("2023-05-01", "2023-05-31");
        verify(earthquakeConverterMock).toDto(dayCount);
    }

    @Test
    void getEarthquakeCounts_whenNoCountsFound_returnsEmptyList() {
        when(dayCountRepositoryMock.findByDayBetweenOrderByDay("2023-05-01", "2023-05-31")).thenReturn(Flux.empty());

        StepVerifier.create(earthquakeService.getEarthquakeCounts(PERIOD_DAY, "2023-05-01", "2023-05-31"))
            .expectNext(List.of())
            .verifyComplete();

        verify(earthquakeService).getEarthquakeCounts(PERIOD_DAY, "2023-05-01", "2023-05-31");
        verify(dayCountRepositoryMock).findByDayBetweenOrderByDay("2023-05-01", "2023-05-31");
    }

    private void assertSaveUSGSRawDataThrows() {
        assertThatThrownBy(() -> earthquakeService.saveUSGSRawData(FROM_DATE, TO_DATE))
            .isInstanceOf(ValidationException.class)
            .hasMessage("USGS raw data could not be saved...the call returned a null or invalid result");

        verify(earthquakeService).saveUSGSRawData(FROM_DATE, TO_DATE);
        verify(restClientMock).get();
    }

    private void stubWebClientQuery(Mono<FeatureCollection> response) {
        when(webClientMock.get()).thenReturn(webClientUriSpecMock);
        when(webClientUriSpecMock.uri(any(Function.class))).thenReturn(webClientHeadersSpecMock);
        when(webClientHeadersSpecMock.retrieve()).thenReturn(webClientResponseSpecMock);
        when(webClientResponseSpecMock.bodyToMono(FeatureCollection.class)).thenReturn(response);
    }

    private void stubRestClientQuery(FeatureCollection response) {
        when(restClientMock.get()).thenReturn(restClientUriSpecMock);
        when(restClientUriSpecMock.uri(any(Function.class))).thenReturn(restClientHeadersSpecMock);
        when(restClientHeadersSpecMock.retrieve()).thenReturn(restClientResponseSpecMock);
        when(restClientResponseSpecMock.body(FeatureCollection.class)).thenReturn(response);
    }

    private String capturedWebClientUri() {
        ArgumentCaptor<Function<UriBuilder, URI>> captor = ArgumentCaptor.forClass(Function.class);
        verify(webClientUriSpecMock).uri(captor.capture());
        return captor.getValue().apply(UriComponentsBuilder.newInstance()).toString();
    }

    private String capturedRestClientUri() {
        ArgumentCaptor<Function<UriBuilder, URI>> captor = ArgumentCaptor.forClass(Function.class);
        verify(restClientUriSpecMock).uri(captor.capture());
        return captor.getValue().apply(UriComponentsBuilder.newInstance()).toString();
    }

    private static FeatureCollection featureCollection(int status, List<Feature> features) {
        Metadata metadata = new Metadata();
        metadata.setStatus(status);
        metadata.setCount(features.size());
        FeatureCollection featureCollection = new FeatureCollection();
        featureCollection.setMetadata(metadata);
        featureCollection.setFeatures(features);
        return featureCollection;
    }

    private static Feature feature(String id) {
        Feature feature = new Feature();
        feature.setId(id);
        return feature;
    }

    private static Earthquake earthquake(Integer id, String featureId) {
        Earthquake earthquake = new Earthquake();
        earthquake.setId(id);
        earthquake.setFeatureId(featureId);
        return earthquake;
    }

    private static PeriodCount periodCount(String period, int count) {
        PeriodCount periodCount = new PeriodCount();
        periodCount.setPeriod(period);
        periodCount.setCount(count);
        return periodCount;
    }

}
