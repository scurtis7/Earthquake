package com.scurtis.earthquakes.common;

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public class AppConstants {

    public static final String USGS_BASE_URL = "https://earthquake.usgs.gov";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd")
        .withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm:ss")
        .withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    public static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    // Valid periods
    public static final String PERIOD_YEAR = "year";
    public static final String PERIOD_MONTH = "month";
    public static final String PERIOD_DAY = "day";
    // Error messages
    public static final String PERIOD_ERROR_MSG = "period is required and valid values are 'year', 'month' or 'day'";
    public static final String FROM_DATE_ERROR_MSG = "fromDate is required and must be in yyyy-MM-dd format";
    public static final String TO_DATE_ERROR_MSG = "toDate is required and must be in yyyy-MM-dd format";
    public static final String DATE_RANGE_ERROR_MSG = "toDate must be greater than fromDate";
    public static final String MAX_DAYS_ERROR_MSG = "The maximum number of days allowed to process is 999";
    public static final int MAX_DAYS = 8000;
    // Elasticsearch
    public static final String ES_INDEX_NAME = "earthquake_index";
    public static final String ES_MAPPING_RESOURCE_PATH = "elasticsearch/earthquake-mapping.json";
    public static final int ES_LOAD_BATCH_SIZE = 1000;

    private AppConstants() {
        // Make the constructor private to prevent instantiation
    }

}
