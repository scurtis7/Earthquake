package com.scurtis.earthquakes.controller;

import com.scurtis.earthquakes.dto.EarthquakeCountDto;
import com.scurtis.earthquakes.model.FeatureCollection;
import com.scurtis.earthquakes.service.EarthquakeService;
import com.scurtis.earthquakes.service.ElasticsearchIndexService;
import com.scurtis.earthquakes.service.ValidationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EarthquakeController {

    private final EarthquakeService earthquakeService;
    private final ValidationService validationService;
    private final ElasticsearchIndexService elasticsearchIndexService;

    /**
     * This method will call the USGS earthquake API to get the raw earthquake data for the specified time
     * period and return it (nothing is saved to the database).
     *
     * @param fromDate the date to start querying from in uuuu-MM-dd format.
     * @param toDate   the ending date of the query in uuuu-MM-dd format.
     * @return collection of Feature objects which contain raw daily earthquake data
     */
    @GetMapping("earthquake/raw")
    public Mono<FeatureCollection> getUSGSRawData(@RequestParam() String fromDate, @RequestParam() String toDate) {
        log.info("EarthquakeController -> getUSGSByYearAndMonth");
        return earthquakeService.getUSGSRawData(fromDate, toDate);
    }

    /**
     * This method calls the USGS earthquake API to get the raw earthquake data for every day in the range
     * specified and save them to the database.
     *
     * @param fromDate the date to start querying from in uuuu-MM-dd format.
     * @param toDate   the ending date of the query in uuuu-MM-dd format.
     * @return success message indicating the call has finished.
     */
    @GetMapping("earthquake/raw/save")
    public String saveUSGSRawData(@RequestParam() String fromDate, @RequestParam() String toDate) {
        log.info("EarthquakeController -> saveEarthquakesRawDataByYearAndMonth");
        validationService.validateDateInputs(fromDate, toDate);
        return earthquakeService.saveUSGSRawData(fromDate, toDate);
    }

    /**
     * This method calls the USGS earthquake API to get the raw earthquake data for everyday
     * starting with the start date until the current date and save them to the database.
     *
     * @param startDate the date to start querying from in uuuu-MM-dd format.
     * @return success message indicating the call has finished.
     */
    @GetMapping("earthquake/raw/save/from")
    public String saveUSGSRawDataFromStartDate(@RequestParam() String startDate) {
        log.info("EarthquakeController -> saveUSGSRawDataFromStartDate");
        validationService.validateStartDate(startDate);
        earthquakeService.saveUSGSRawDataFromStartDate(startDate);
        return "Success";
    }

    /**
     * This method will call the USGS earthquake API to get the number of earthquakes for the specified time
     * period and save them to the database.
     *
     * @param period   string value of either 'year', 'month' or 'day'.
     * @param fromDate the date to start querying from in uuuu-MM-dd format.
     * @param toDate   the ending date of the query in uuuu-MM-dd format.
     * @return success message indicating the call has finished.
     */
    @GetMapping("earthquake/counts/save")
    public String saveEarthquakeCounts(@RequestParam() String period, @RequestParam() String fromDate, @RequestParam() String toDate) {
        log.info("EarthquakeController -> saveEarthquakeCounts");
        validationService.ValidateInputs(period, fromDate, toDate);
        earthquakeService.saveEarthquakeCounts(period, fromDate, toDate);
        String msg = String.format("Counts have been saved for the '%s' period from '%s' to '%s'", period, fromDate, toDate);
        log.debug(msg);
        return msg;
    }

    /**
     * This method will query the database in the appropriate table to get the number of earthquakes
     * for the specified time period.
     *
     * @param period   string value of either 'year', 'month' or 'day'.
     * @param fromDate the date to start querying from in uuuu-MM-dd format.
     * @param toDate   the ending date of the query in uuuu-MM-dd format.
     * @return success message indicating the call has finished.
     */
    @GetMapping("earthquake/counts")
    public Mono<List<EarthquakeCountDto>> getEarthquakeCounts(@RequestParam() String period, @RequestParam() String fromDate, @RequestParam() String toDate) {
        log.info("EarthquakeController -> getEarthquakeCounts");
        validationService.validateDateInputs(fromDate, toDate);
        return earthquakeService.getEarthquakeCounts(period, fromDate, toDate);
    }

    /**
     * This method (re)creates the earthquake Elasticsearch index from its fixed mapping and reloads it
     * with every earthquake record currently stored in Postgres. If the index already exists it is
     * deleted first, so every call leaves a fresh index containing a full copy of the database.
     *
     * @return success message indicating the index has been recreated and the record count loaded.
     */
    @PutMapping("earthquake/es-index")
    public Mono<String> recreateEarthquakeESIndex() {
        log.info("EarthquakeController -> recreateEarthquakeIndex");
        return elasticsearchIndexService.recreateESIndex();
    }

}
