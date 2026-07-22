package com.scurtis.earthquakes.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({"code", "ids", "title", "place", "time", "updated", "tz", "type", "types",
    "tsunami", "sig", "detail", "url", "mag", "magType", "status", "sources", "net"})
public class Properties {

    private String code;
    private String ids;
    private String title;
    private String place;
    private Long time;
    private Long updated;
    private String tz;
    private String type;
    private String types;
    private Integer tsunami;
    private Integer sig;
    private String detail;
    private String url;
    private Float mag;
    private String magType;
    private String status;
    private String sources;
    private String net;
    private String felt;
    private String cdi;
    private String mmi;
    private String alert;
    private Integer nst;
    private Float gap;
    private Float dmin;
    private Float rms;

}
