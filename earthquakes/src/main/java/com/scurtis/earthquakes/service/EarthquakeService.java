package com.scurtis.earthquakes.service;

import com.scurtis.earthquakes.converter.EarthquakeConverter;
import com.scurtis.earthquakes.dto.EarthquakeCountDto;
import com.scurtis.earthquakes.entity.DayCount;
import com.scurtis.earthquakes.entity.Earthquake;
import com.scurtis.earthquakes.entity.MonthCount;
import com.scurtis.earthquakes.entity.YearCount;
import com.scurtis.earthquakes.exception.ValidationException;
import com.scurtis.earthquakes.model.FeatureCollection;
import com.scurtis.earthquakes.repository.DayCountRepository;
import com.scurtis.earthquakes.repository.EarthquakeRepository;
import com.scurtis.earthquakes.repository.MonthCountRepository;
import com.scurtis.earthquakes.repository.YearCountRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static com.scurtis.earthquakes.common.AppConstants.DATE_FORMATTER;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_LENGTH_DAY;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_LENGTH_MONTH;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_LENGTH_YEAR;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_MONTH;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_YEAR;
import static com.scurtis.earthquakes.common.AppConstants.YEAR_FORMATTER;
import static com.scurtis.earthquakes.common.AppConstants.YEAR_MONTH_FORMATTER;

@Slf4j
@Service
@RequiredArgsConstructor
public class EarthquakeService {

    private final WebClient webClient;
    private final RestClient restClient;
    private final YearCountRepository yearCountRepository;
    private final MonthCountRepository monthCountRepository;
    private final DayCountRepository dayCountRepository;
    private final EarthquakeRepository earthquakeRepository;
    private final EarthquakeConverter earthquakeConverter;

    public Mono<FeatureCollection> getUSGSRawData(String fromDate, String toDate) {
        log.info("EarthquakeService -> getUSGSRawData(fromDate:{}, toDate:{})", fromDate, toDate);
        return queryUSGS(fromDate, toDate);
    }

    private Mono<FeatureCollection> queryUSGS(String fromDate, String toDate) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/fdsnws/event/1/query")
                .queryParam("format", "geojson")
                .queryParam("starttime", fromDate)
                .queryParam("endtime", toDate)
                .build())
            .retrieve()
            .bodyToMono(FeatureCollection.class)
            .doOnError(throwable -> log.error(throwable.getMessage(), throwable));
    }

    public void saveEarthquakeCounts(String period, String fromDate, String toDate) {
        log.info("EarthquakeService -> saveEarthquakeCounts(period:{}, fromDate:{}, toDate:{})", period, fromDate, toDate);
        StopWatch stopWatch = new StopWatch("EarthquakeService.saveEarthquakeCounts");
        stopWatch.start();
        // switched to getting counts from database so I can filter out any counts not from earthquakes
        if (PERIOD_YEAR.equalsIgnoreCase(period)) {
            saveCountsByYear(fromDate, toDate);
        } else if (PERIOD_MONTH.equalsIgnoreCase(period)) {
            saveCountsByMonth(fromDate, toDate);
        } else {
            saveCountsByDay(fromDate, toDate);
        }
        stopWatch.stop();
        log.debug(stopWatch.shortSummary());
    }

    private void saveCountsByYear(String fromDate, String toDate) {
        earthquakeRepository.getEarthquakeCountsByPeriod(PERIOD_LENGTH_YEAR, fromDate, toDate)
            .flatMap(periodCount -> yearCountRepository.findByYear(periodCount.getPeriod())
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(existing -> {
                    if (existing.isEmpty()) {
                        YearCount yearCount = new YearCount();
                        yearCount.setYear(periodCount.getPeriod());
                        yearCount.setCount(periodCount.getCount());
                        log.debug("Saving earthquake count by year: {}", yearCount.getYear());
                        return yearCountRepository.save(yearCount);
                    }
                    YearCount original = existing.get();
                    if (original.getCount() != periodCount.getCount()) {
                        original.setCount(periodCount.getCount());
                        log.debug("Updating earthquake count by year: {}", original.getYear());
                        return yearCountRepository.save(original);
                    } else {
                        return Mono.just(original);
                    }
                }))
            .subscribe();
    }

    private void saveCountsByMonth(String fromDate, String toDate) {
        earthquakeRepository.getEarthquakeCountsByPeriod(PERIOD_LENGTH_MONTH, fromDate, toDate)
            .flatMap(periodCount -> monthCountRepository.findByMonth(periodCount.getPeriod())
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(existing -> {
                    if (existing.isEmpty()) {
                        MonthCount monthCount = new MonthCount();
                        monthCount.setMonth(periodCount.getPeriod());
                        monthCount.setCount(periodCount.getCount());
                        log.debug("Saving earthquake count by month: {}", monthCount.getMonth());
                        return monthCountRepository.save(monthCount);
                    }
                    MonthCount original = existing.get();
                    if (original.getCount() != periodCount.getCount()) {
                        original.setCount(periodCount.getCount());
                        log.debug("Updating earthquake count by month: {}", original.getMonth());
                        return monthCountRepository.save(original);
                    } else {
                        return Mono.just(original);
                    }
                }))
            .subscribe();
    }

    private void saveCountsByDay(String fromDate, String toDate) {
        earthquakeRepository.getEarthquakeCountsByPeriod(PERIOD_LENGTH_DAY, fromDate, toDate)
            .flatMap(periodCount -> dayCountRepository.findByDay(periodCount.getPeriod())
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(existing -> {
                    if (existing.isEmpty()) {
                        DayCount dayCount = new DayCount();
                        dayCount.setDay(periodCount.getPeriod());
                        dayCount.setCount(periodCount.getCount());
                        log.debug("Saving earthquake count by day: {}", dayCount.getDay());
                        return dayCountRepository.save(dayCount);
                    }
                    DayCount original = existing.get();
                    if (original.getCount() != periodCount.getCount()) {
                        original.setCount(periodCount.getCount());
                        log.debug("Updating earthquake count by day: {}", original.getDay());
                        return dayCountRepository.save(original);
                    } else {
                        return Mono.just(original);
                    }
                }))
            .subscribe();
    }

    public Mono<List<EarthquakeCountDto>> getEarthquakeCounts(String period, String fromDate, String toDate) {
        log.info("EarthquakeService -> getEarthquakeCounts(period:{}, fromDate:{}, toDate:{})", period, fromDate, toDate);
        if (PERIOD_YEAR.equalsIgnoreCase(period)) {
            String startYear = LocalDate.parse(fromDate, DATE_FORMATTER).format(YEAR_FORMATTER);
            String endYear = LocalDate.parse(toDate, DATE_FORMATTER).format(YEAR_FORMATTER);
            return yearCountRepository.findByYearBetweenOrderByYear(startYear, endYear)
                .map(earthquakeConverter::toDto)
                .collectList();
        } else if (PERIOD_MONTH.equalsIgnoreCase(period)) {
            String startYearMonth = LocalDate.parse(fromDate, DATE_FORMATTER).format(YEAR_MONTH_FORMATTER);
            String endYearMonth = LocalDate.parse(toDate, DATE_FORMATTER).format(YEAR_MONTH_FORMATTER);
            return monthCountRepository.findByMonthBetweenOrderByMonth(startYearMonth, endYearMonth)
                .map(earthquakeConverter::toDto)
                .collectList();
        }
        String startDate = LocalDate.parse(fromDate, DATE_FORMATTER).format(DATE_FORMATTER);
        String endDate = LocalDate.parse(toDate, DATE_FORMATTER).format(DATE_FORMATTER);
        return dayCountRepository.findByDayBetweenOrderByDay(startDate, endDate)
            .map(earthquakeConverter::toDto)
            .collectList();
    }

    public void saveUSGSRawDataFromStartDate(String fromDate) {
        String currentDate = LocalDate.now().format(DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(currentDate, DATE_FORMATTER).plusDays(1);
        LocalDate startDate = LocalDate.parse(fromDate, DATE_FORMATTER).withDayOfMonth(1);
        int totalMonths = (int) ChronoUnit.MONTHS.between(startDate, endDate);
        log.debug("Raw Months between {} and {} is: {}", startDate, endDate, totalMonths);
        LocalDate endingDate = startDate.plusMonths(1);
        for (int i = 0; i < totalMonths; i++) {
            log.debug("calling saveUSGSRawData(startDate:{}, endingDate:{})", startDate, endingDate);
            saveUSGSRawData(startDate.format(DATE_FORMATTER), endingDate.format(DATE_FORMATTER));

            startDate = startDate.plusMonths(1);
            endingDate = endingDate.plusMonths(1);
            log.debug("   new startDate:{}, new endingDate:{})", startDate, endingDate);

            if (startDate.isEqual(endDate) || startDate.isAfter(endDate)) {
                break;
            }
            if (endingDate.isAfter(endDate)) {
                endingDate = endDate;
            }
        }
    }

    public String saveUSGSRawData(String fromDate, String toDate) {
        log.info("EarthquakeService -> saveUSGSRawData(fromDate:{}, toDate:{})", fromDate, toDate);
        StopWatch stopWatch = new StopWatch("EarthquakeService.saveUSGSRawData");
        stopWatch.start();
        FeatureCollection rawData = restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/fdsnws/event/1/query")
                .queryParam("format", "geojson")
                .queryParam("starttime", fromDate)
                .queryParam("endtime", toDate)
                .build())
            .retrieve()
            .body(FeatureCollection.class);
        if (rawData == null || rawData.getMetadata() == null || rawData.getMetadata().getStatus() != 200) {
            String msg = "USGS raw data could not be saved...the call returned a null or invalid result";
            log.error(msg);
            throw new ValidationException(msg);
        }
        log.debug("   status: {}", rawData.getMetadata().getStatus());
        log.debug("    count: {}", rawData.getMetadata().getCount());
        saveRawData(rawData);
        stopWatch.stop();
        String msg = String.format("Saved %d records of raw earthquake data between the dates of '%s' and '%s'",
            rawData.getMetadata().getCount(), fromDate, toDate);
        log.debug(msg);
        log.debug(stopWatch.shortSummary());
        return msg;
    }

    private void saveRawData(FeatureCollection featureCollection) {
        featureCollection.getFeatures()
            .forEach(feature -> earthquakeRepository.findByFeatureId(feature.getId())
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(existing -> {
                    Earthquake earthquake = earthquakeConverter.toEntity(feature);
                    existing.ifPresent(original -> earthquake.setId(original.getId()));
                    //log.debug("Saving earthquake: {}", earthquake);
                    return earthquakeRepository.save(earthquake);
                }).subscribe());
    }

}
