package com.scurtis.earthquakes.service;

import com.scurtis.earthquakes.exception.ValidationException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.scurtis.earthquakes.TestUtils.FROM_DATE;
import static com.scurtis.earthquakes.TestUtils.TO_DATE;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationServiceTest {

    private final ValidationService validationService = new ValidationService();

    @ParameterizedTest
    @ValueSource(strings = {PERIOD_YEAR, PERIOD_MONTH, PERIOD_DAY, "YEAR", "Month"})
    void validateInputs_withValidPeriod_doesNotThrow(String period) {
        assertThatCode(() -> validationService.validateInputs(period, FROM_DATE, TO_DATE)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "week", "years"})
    void validateInputs_withInvalidPeriod_throws(String period) {
        assertValidationException(() -> validationService.validateInputs(period, FROM_DATE, TO_DATE), PERIOD_ERROR_MSG);
    }

    @Test
    void validateInputs_withInvalidDates_throws() {
        assertValidationException(() -> validationService.validateInputs(PERIOD_YEAR, TO_DATE, FROM_DATE), DATE_RANGE_ERROR_MSG);
    }

    @Test
    void validateInputs_dayPeriodAtMaxDays_doesNotThrow() {
        String toDate = LocalDate.parse(FROM_DATE).plusDays(MAX_DAYS).format(DATE_FORMATTER);

        assertThatCode(() -> validationService.validateInputs(PERIOD_DAY, FROM_DATE, toDate)).doesNotThrowAnyException();
    }

    @Test
    void validateInputs_dayPeriodOverMaxDays_throws() {
        String toDate = LocalDate.parse(FROM_DATE).plusDays(MAX_DAYS + 1).format(DATE_FORMATTER);

        assertValidationException(() -> validationService.validateInputs(PERIOD_DAY, FROM_DATE, toDate), MAX_DAYS_ERROR_MSG);
    }

    @Test
    void validateInputs_yearPeriodOverMaxDays_doesNotThrow() {
        String toDate = LocalDate.parse(FROM_DATE).plusDays(MAX_DAYS + 1).format(DATE_FORMATTER);

        assertThatCode(() -> validationService.validateInputs(PERIOD_YEAR, FROM_DATE, toDate)).doesNotThrowAnyException();
    }

    @Test
    void validateDateInputs_withValidRange_doesNotThrow() {
        assertThatCode(() -> validationService.validateDateInputs(FROM_DATE, TO_DATE)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "2024/01/15", "2024-13-01", "2023-02-29", "not-a-date"})
    void validateDateInputs_withInvalidFromDate_throws(String fromDate) {
        assertValidationException(() -> validationService.validateDateInputs(fromDate, TO_DATE), FROM_DATE_ERROR_MSG);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "2024/02/15", "2024-02-30", "not-a-date"})
    void validateDateInputs_withInvalidToDate_throws(String toDate) {
        assertValidationException(() -> validationService.validateDateInputs(FROM_DATE, toDate), TO_DATE_ERROR_MSG);
    }

    @Test
    void validateDateInputs_whenToDateEqualsFromDate_throws() {
        assertValidationException(() -> validationService.validateDateInputs(FROM_DATE, FROM_DATE), DATE_RANGE_ERROR_MSG);
    }

    @Test
    void validateDateInputs_whenToDateBeforeFromDate_throws() {
        assertValidationException(() -> validationService.validateDateInputs(TO_DATE, FROM_DATE), DATE_RANGE_ERROR_MSG);
    }

    @Test
    void validateStartDate_withPastDate_doesNotThrow() {
        String yesterday = LocalDate.now().minusDays(1).format(DATE_FORMATTER);

        assertThatCode(() -> validationService.validateStartDate(yesterday)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "2024/01/15", "2024-02-30"})
    void validateStartDate_withInvalidDate_throws(String startDate) {
        assertValidationException(() -> validationService.validateStartDate(startDate), FROM_DATE_ERROR_MSG);
    }

    @Test
    void validateStartDate_whenToday_throws() {
        String today = LocalDate.now().format(DATE_FORMATTER);

        assertValidationException(() -> validationService.validateStartDate(today), DATE_RANGE_ERROR_MSG);
    }

    @Test
    void validateStartDate_whenInFuture_throws() {
        String tomorrow = LocalDate.now().plusDays(1).format(DATE_FORMATTER);

        assertValidationException(() -> validationService.validateStartDate(tomorrow), DATE_RANGE_ERROR_MSG);
    }

    private static void assertValidationException(Runnable call, String expectedMessage) {
        assertThatThrownBy(call::run)
            .isInstanceOf(ValidationException.class)
            .hasMessage(expectedMessage);
    }

}
