package com.scurtis.earthquakes.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({"generated", "url", "title", "status", "api", "count"})
public class Metadata {

    private Long generated;
    private String url;
    private String title;
    private Integer status;
    private String api;
    private Integer count;

}
