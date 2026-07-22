package com.scurtis.earthquakes.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "earthquake", name = "earthquake")
public class Earthquake implements Persistable<Integer> {

    @Id
    private Integer id;
    private String featureId;
    private String coordinateType;
    private String coordinates;
    private String code;
    private String ids;
    private String title;
    private String place;
    private String dateTime;
    private String updatedDate;
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

    @Override
    @Transient
    public boolean isNew() {
        return id == null || id == 0;
    }

}
