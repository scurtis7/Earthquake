package com.scurtis.earthquakes;

import com.scurtis.earthquakes.entity.Earthquake;
import com.scurtis.earthquakes.model.Feature;

public class TestUtils {

    private TestUtils() {
        // prevent instantiation
    }

    public static final String FEATURE_ID_NEW = "new";
    public static final String FEATURE_ID_EXISTING = "existing";
    public static final String FROM_DATE = "2024-01-15";
    public static final String TO_DATE = "2024-02-15";
    public static final String EXPECTED_QUERY_URI = "/fdsnws/event/1/query?format=geojson&starttime=" + FROM_DATE + "&endtime=" + TO_DATE;

    public static final String USGS_DOWN_MSG = "USGS is down";
    public static final String INVALID_DATE_MSG = "Invalid date";

    public static Feature getNewFeature() {
        return getFeature(FEATURE_ID_NEW);
    }

    public static Feature getExistingFeature() {
        return getFeature(FEATURE_ID_EXISTING);
    }

    private static Feature getFeature(String id) {
        Feature feature = new Feature();
        feature.setId(id);
        feature.setType("type");
        feature.setGeometry(null);
        feature.setProperties(null);
        return feature;
    }

    public static Earthquake getNewEarthquake() {
        return getEarthquake(FEATURE_ID_NEW);
    }

    public static Earthquake getExistingEarthquake() {
        return getEarthquake(FEATURE_ID_EXISTING);
    }

    private static Earthquake getEarthquake(String id) {
        Earthquake earthquake = new Earthquake();
        earthquake.setId(null);
        earthquake.setFeatureId(id);
        return earthquake;
    }

}
