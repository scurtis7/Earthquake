package com.scurtis.earthquakes.service;

import com.scurtis.earthquakes.exception.ValidationException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import static com.scurtis.earthquakes.common.AppConstants.DATE_FORMATTER;
import static com.scurtis.earthquakes.common.AppConstants.DATE_RANGE_ERROR_MSG;
import static com.scurtis.earthquakes.common.AppConstants.FROM_DATE_ERROR_MSG;
import static com.scurtis.earthquakes.common.AppConstants.MAX_DAYS;
import static com.scurtis.earthquakes.common.AppConstants.MAX_DAYS_ERROR_MSG;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_DAY;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_ERROR_MSG;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_MONTH;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_YEAR;
import static com.scurtis.earthquakes.common.AppConstants.TO_DATE_ERROR_MSG;

@Service
public class ValidationService {

    public void ValidateInputs(String period, String fromDate, String toDate) {
        validatePeriodInput(period);
        validateDateInputs(fromDate, toDate);
        if (PERIOD_DAY.equals(period)) {
            validateNumberOfDays(fromDate, toDate);
        }
    }

    private void validatePeriodInput(String period) {
        if (StringUtils.isBlank(period)) {
            throw new ValidationException(PERIOD_ERROR_MSG);
        }
        if (PERIOD_YEAR.equalsIgnoreCase(period) || PERIOD_MONTH.equalsIgnoreCase(period) || PERIOD_DAY.equalsIgnoreCase(period)) {
            return;
        }
        throw new ValidationException(PERIOD_ERROR_MSG);
    }

    public void validateDateInputs(String fromDate, String toDate) {
        if (StringUtils.isBlank(fromDate)) {
            throw new ValidationException(FROM_DATE_ERROR_MSG);
        }
        if (StringUtils.isBlank(toDate)) {
            throw new ValidationException(TO_DATE_ERROR_MSG);
        }
        LocalDate startDate;
        LocalDate endDate;
        try {
            startDate = LocalDate.parse(fromDate, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException(FROM_DATE_ERROR_MSG);
        }
        try {
            endDate = LocalDate.parse(toDate, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException(TO_DATE_ERROR_MSG);
        }
        if (endDate.isBefore(startDate) || endDate.isEqual(startDate)) {
            throw new ValidationException(DATE_RANGE_ERROR_MSG);
        }
    }

    public void validateStartDate(String fromDate) {
        if (StringUtils.isBlank(fromDate)) {
            throw new ValidationException(FROM_DATE_ERROR_MSG);
        }
        LocalDate startDate;
        LocalDate currentDate = LocalDate.now();
        try {
            startDate = LocalDate.parse(fromDate, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException(FROM_DATE_ERROR_MSG);
        }
        if (currentDate.isBefore(startDate) || currentDate.isEqual(startDate)) {
            throw new ValidationException(DATE_RANGE_ERROR_MSG);
        }
    }

    private void validateNumberOfDays(String fromDate, String toDate) {
        LocalDate startDate = LocalDate.parse(fromDate, DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(toDate, DATE_FORMATTER);
        int totalDays = (int) ChronoUnit.DAYS.between(startDate, endDate);
        if (totalDays > MAX_DAYS) {
            throw new ValidationException(MAX_DAYS_ERROR_MSG);
        }
    }

}
