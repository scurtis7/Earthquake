package com.scurtis.earthquakes.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({"type", "id", "geometry", "properties"})
public class Feature {

    private String type;
    private String id;
    private Geometry geometry;
    private Properties properties;

}
