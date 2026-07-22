package com.scurtis.earthquakes.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.Data;

@Data
@JsonPropertyOrder({"type", "metadata", "features"})
public class FeatureCollection {

    private String type;
    private Metadata metadata;
    private List<Feature> features;

}
