package com.scurtis.earthquakes.exception.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record ErrorResponse(int code, String status, String exception, String message,
                            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date) {
}
