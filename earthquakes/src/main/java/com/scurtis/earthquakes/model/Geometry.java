package com.scurtis.earthquakes.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.Data;

@Data
@JsonPropertyOrder({"type", "coordinates"})
public class Geometry {

    private String type;
    private List<Float> coordinates;

}
