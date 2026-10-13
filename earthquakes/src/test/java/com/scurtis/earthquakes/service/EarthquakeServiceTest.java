package com.scurtis.earthquakes.service;

import com.scurtis.earthquakes.converter.EarthquakeConverter;
import com.scurtis.earthquakes.repository.DayCountRepository;
import com.scurtis.earthquakes.repository.EarthquakeRepository;
import com.scurtis.earthquakes.repository.MonthCountRepository;
import com.scurtis.earthquakes.repository.YearCountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
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

}
