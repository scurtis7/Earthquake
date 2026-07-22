package com.scurtis.earthquakes.converter;

import com.scurtis.earthquakes.dto.EarthquakeCountDto;
import com.scurtis.earthquakes.entity.DayCount;
import com.scurtis.earthquakes.entity.Earthquake;
import com.scurtis.earthquakes.entity.MonthCount;
import com.scurtis.earthquakes.entity.YearCount;
import com.scurtis.earthquakes.model.Feature;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import static com.scurtis.earthquakes.common.AppConstants.DATE_TIME_FORMATTER;

@Log4j2
@Service
public class EarthquakeConverter {

    public EarthquakeCountDto toDto(YearCount entity) {
        return new EarthquakeCountDto("year", entity.getYear(), entity.getCount());
    }

    public EarthquakeCountDto toDto(MonthCount entity) {
        return new EarthquakeCountDto("month", entity.getMonth(), entity.getCount());
    }

    public EarthquakeCountDto toDto(DayCount entity) {
        return new EarthquakeCountDto("day", entity.getDay(), entity.getCount());
    }

    public Earthquake toEntity(Feature feature) {
        Earthquake earthquake = new Earthquake();
        earthquake.setFeatureId(feature.getId());
        if (feature.getGeometry() != null) {
            earthquake.setCoordinateType(feature.getGeometry().getType());
            earthquake.setCoordinates(getCoordinates(feature.getGeometry().getCoordinates()));
        }
        if (feature.getProperties() != null) {
            earthquake.setCode(feature.getProperties().getCode());
            earthquake.setIds(feature.getProperties().getIds());
            earthquake.setTitle(feature.getProperties().getTitle());
            earthquake.setPlace(feature.getProperties().getPlace());
            earthquake.setDateTime(convertEpoch(feature.getProperties().getTime()));
            earthquake.setUpdatedDate(convertEpoch(feature.getProperties().getUpdated()));
            earthquake.setTz(feature.getProperties().getTz());
            earthquake.setType(feature.getProperties().getType());
            earthquake.setTypes(feature.getProperties().getTypes());
            earthquake.setTsunami(feature.getProperties().getTsunami());
            earthquake.setSig(feature.getProperties().getSig());
            earthquake.setDetail(feature.getProperties().getDetail());
            earthquake.setUrl(feature.getProperties().getUrl());
            earthquake.setMag(feature.getProperties().getMag());
            earthquake.setMagType(feature.getProperties().getMagType());
            earthquake.setStatus(feature.getProperties().getStatus());
            earthquake.setSources(feature.getProperties().getSources());
            earthquake.setNet(feature.getProperties().getNet());
            earthquake.setFelt(feature.getProperties().getFelt());
            earthquake.setCdi(feature.getProperties().getCdi());
            earthquake.setMmi(feature.getProperties().getMmi());
            earthquake.setAlert(feature.getProperties().getAlert());
            earthquake.setNst(feature.getProperties().getNst());
            earthquake.setGap(feature.getProperties().getGap());
            earthquake.setDmin(feature.getProperties().getDmin());
            earthquake.setRms(feature.getProperties().getRms());
        }
        return earthquake;
    }

    private String getCoordinates(List<Float> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            return "";
        }
        return coordinates.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(", "));
    }

    private String convertEpoch(Long time) {
        if (time == null) {
            return "";
        }
        return Instant.ofEpochMilli(time)
            .atZone(ZoneOffset.UTC)
            .toLocalDateTime()
            .format(DATE_TIME_FORMATTER);
    }

}
