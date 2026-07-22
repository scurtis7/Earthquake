package com.scurtis.earthquakes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EarthquakeCountDto {

    private String period;
    private String date;
    private int count;

}
