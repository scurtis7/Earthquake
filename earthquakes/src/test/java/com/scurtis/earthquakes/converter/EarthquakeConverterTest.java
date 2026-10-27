package com.scurtis.earthquakes.converter;

import com.scurtis.earthquakes.dto.EarthquakeCountDto;
import com.scurtis.earthquakes.entity.DayCount;
import com.scurtis.earthquakes.entity.Earthquake;
import com.scurtis.earthquakes.entity.MonthCount;
import com.scurtis.earthquakes.entity.YearCount;
import com.scurtis.earthquakes.model.Feature;
import com.scurtis.earthquakes.model.Geometry;
import com.scurtis.earthquakes.model.Properties;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static com.scurtis.earthquakes.TestUtils.FEATURE_ID_NEW;
import static com.scurtis.earthquakes.TestUtils.getNewFeature;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_DAY;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_MONTH;
import static com.scurtis.earthquakes.common.AppConstants.PERIOD_YEAR;
import static org.assertj.core.api.Assertions.assertThat;

class EarthquakeConverterTest {

    private final EarthquakeConverter earthquakeConverter = new EarthquakeConverter();

    @Test
    void toDto_yearCount() {
        assertThat(earthquakeConverter.toDto(new YearCount(1, "2024", 10)))
            .isEqualTo(new EarthquakeCountDto(PERIOD_YEAR, "2024", 10));
    }

    @Test
    void toDto_monthCount() {
        assertThat(earthquakeConverter.toDto(new MonthCount(1, "2024-01", 20)))
            .isEqualTo(new EarthquakeCountDto(PERIOD_MONTH, "2024-01", 20));
    }

    @Test
    void toDto_dayCount() {
        assertThat(earthquakeConverter.toDto(new DayCount(1, "2024-01-15", 30)))
            .isEqualTo(new EarthquakeCountDto(PERIOD_DAY, "2024-01-15", 30));
    }

    @Test
    void toEntity_mapsAllFields() {
        Geometry geometry = new Geometry();
        geometry.setType("Point");
        geometry.setCoordinates(List.of(-117.5f, 35.25f, 8.0f));

        Properties properties = new Properties();
        properties.setCode("12345");
        properties.setIds(",ci12345,");
        properties.setTitle("M 4.2 - 10km N of Ridgecrest, CA");
        properties.setPlace("10km N of Ridgecrest, CA");
        properties.setTime(epochMillis(LocalDateTime.of(2024, 1, 15, 9, 30, 45)));
        properties.setUpdated(epochMillis(LocalDateTime.of(2024, 1, 16, 22, 5, 0)));
        properties.setTz("-480");
        properties.setType("earthquake");
        properties.setTypes(",origin,phase-data,");
        properties.setTsunami(0);
        properties.setSig(271);
        properties.setDetail("https://earthquake.usgs.gov/detail");
        properties.setUrl("https://earthquake.usgs.gov/url");
        properties.setMag(4.2f);
        properties.setMagType("ml");
        properties.setStatus("reviewed");
        properties.setSources(",ci,");
        properties.setNet("ci");
        properties.setFelt("12");
        properties.setCdi("3.1");
        properties.setMmi("4.0");
        properties.setAlert("green");
        properties.setNst(45);
        properties.setGap(30.5f);
        properties.setDmin(0.05f);
        properties.setRms(0.18f);

        Feature feature = getNewFeature();
        feature.setGeometry(geometry);
        feature.setProperties(properties);

        Earthquake earthquake = earthquakeConverter.toEntity(feature);

        assertThat(earthquake.getId()).isNull();
        assertThat(earthquake.getFeatureId()).isEqualTo(FEATURE_ID_NEW);
        assertThat(earthquake.getCoordinateType()).isEqualTo("Point");
        assertThat(earthquake.getCoordinates()).isEqualTo("-117.5, 35.25, 8.0");
        assertThat(earthquake.getCode()).isEqualTo("12345");
        assertThat(earthquake.getIds()).isEqualTo(",ci12345,");
        assertThat(earthquake.getTitle()).isEqualTo("M 4.2 - 10km N of Ridgecrest, CA");
        assertThat(earthquake.getPlace()).isEqualTo("10km N of Ridgecrest, CA");
        assertThat(earthquake.getDateTime()).isEqualTo("2024-01-15 09:30:45");
        assertThat(earthquake.getUpdatedDate()).isEqualTo("2024-01-16 22:05:00");
        assertThat(earthquake.getTz()).isEqualTo("-480");
        assertThat(earthquake.getType()).isEqualTo("earthquake");
        assertThat(earthquake.getTypes()).isEqualTo(",origin,phase-data,");
        assertThat(earthquake.getTsunami()).isZero();
        assertThat(earthquake.getSig()).isEqualTo(271);
        assertThat(earthquake.getDetail()).isEqualTo("https://earthquake.usgs.gov/detail");
        assertThat(earthquake.getUrl()).isEqualTo("https://earthquake.usgs.gov/url");
        assertThat(earthquake.getMag()).isEqualTo(4.2f);
        assertThat(earthquake.getMagType()).isEqualTo("ml");
        assertThat(earthquake.getStatus()).isEqualTo("reviewed");
        assertThat(earthquake.getSources()).isEqualTo(",ci,");
        assertThat(earthquake.getNet()).isEqualTo("ci");
        assertThat(earthquake.getFelt()).isEqualTo("12");
        assertThat(earthquake.getCdi()).isEqualTo("3.1");
        assertThat(earthquake.getMmi()).isEqualTo("4.0");
        assertThat(earthquake.getAlert()).isEqualTo("green");
        assertThat(earthquake.getNst()).isEqualTo(45);
        assertThat(earthquake.getGap()).isEqualTo(30.5f);
        assertThat(earthquake.getDmin()).isEqualTo(0.05f);
        assertThat(earthquake.getRms()).isEqualTo(0.18f);
    }

    @Test
    void toEntity_withoutGeometryOrProperties_onlyMapsFeatureId() {
        Earthquake earthquake = earthquakeConverter.toEntity(getNewFeature());

        Earthquake expected = new Earthquake();
        expected.setFeatureId(FEATURE_ID_NEW);
        assertThat(earthquake).isEqualTo(expected);
    }

    @Test
    void toEntity_withNullOrEmptyCoordinatesAndTimes_usesEmptyStrings() {
        Geometry nullCoordinates = new Geometry();
        nullCoordinates.setType("Point");
        Geometry emptyCoordinates = new Geometry();
        emptyCoordinates.setCoordinates(List.of());

        Feature feature = getNewFeature();
        feature.setGeometry(nullCoordinates);
        feature.setProperties(new Properties());
        Earthquake earthquake = earthquakeConverter.toEntity(feature);

        assertThat(earthquake.getCoordinateType()).isEqualTo("Point");
        assertThat(earthquake.getCoordinates()).isEmpty();
        assertThat(earthquake.getDateTime()).isEmpty();
        assertThat(earthquake.getUpdatedDate()).isEmpty();

        feature.setGeometry(emptyCoordinates);
        assertThat(earthquakeConverter.toEntity(feature).getCoordinates()).isEmpty();
    }

    private static long epochMillis(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

}
