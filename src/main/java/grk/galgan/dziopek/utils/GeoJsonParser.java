package grk.galgan.dziopek.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import grk.galgan.dziopek.models.Country;
import grk.galgan.dziopek.models.HistoricalBoundry;
import org.joml.Vector2f;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class GeoJsonParser {

    private Map<String, Country> boundries = new HashMap<>();
    private List<String> loadCountires = Arrays.asList("Poland", "Germany (Prussia)", "Yugoslavia", "Ukraine", "Czechoslovakia", "Israel");

    public Map<String, Country> parseGeoJson(String filePath) {

        ObjectMapper mapper = new ObjectMapper();
        try(InputStream in = getClass().getClassLoader().getResourceAsStream(filePath)) {

            if(Objects.isNull(in)) {
                System.err.println("Couldn't find file " + filePath);
                return boundries;
            }

            JsonNode root = mapper.readTree(in);
            JsonNode features = root.get("features");

            if(Objects.nonNull(features) && features.isArray()) {
                for(JsonNode feature : features) {

                    JsonNode properties = feature.get("properties");
                    JsonNode geometry = feature.get("geometry");

                    if(Objects.nonNull(properties) && Objects.nonNull(geometry)) {

                        int startYear = properties.has("gwsyear") ? properties.get("gwsyear").asInt() : -1;
                        int endYear = properties.has("gweyear") ? properties.get("gweyear").asInt() : -1;
                        String countryName = properties.has("cntry_name") ? properties.get("cntry_name").asText() : "Unknown";

                        if(!loadCountires.contains(countryName)) continue;

                        List<List<List<Vector2f>>> currentGeometries = new ArrayList<>();

                        String geometryType = geometry.get("type").asText();
                        JsonNode coordinates = geometry.get("coordinates");

                        if (geometryType.equals("Polygon")) {
                            List<List<Vector2f>> polygon = parseRings(coordinates);
                            List<List<List<Vector2f>>> multiPolygon = new ArrayList<>();
                            multiPolygon.add(polygon);
                            currentGeometries.addAll(multiPolygon);

                        } else if (geometryType.equals("MultiPolygon")) {
                            for (JsonNode polyNode : coordinates) {
                                List<List<Vector2f>> polygon = parseRings(polyNode);
                                List<List<List<Vector2f>>> multiPolygon = new ArrayList<>();
                                multiPolygon.add(polygon);
                                currentGeometries.addAll(multiPolygon);
                            }
                        }

                        HistoricalBoundry snapshot = new HistoricalBoundry(startYear, endYear, countryName, currentGeometries.getFirst().getFirst());
                        boundries.computeIfAbsent(countryName, Country::new).add(snapshot);

                    }

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        boundries.forEach((key, country) -> country.normalize());
        boundries.forEach((key, country) -> country.assertEquals());
        return boundries;

    }

    private List<List<Vector2f>> parseRings(JsonNode ringsNode) {
        List<List<Vector2f>> polygonRings = new ArrayList<>();
        if (ringsNode.isArray()) {
            for (JsonNode ringNode : ringsNode) {
                List<Vector2f> currentRing = new ArrayList<>();
                if (ringNode.isArray()) {
                    for (JsonNode coordNode : ringNode) {
                        float lon = (float) coordNode.get(0).asDouble();
                        float lat = (float) coordNode.get(1).asDouble();
                        currentRing.add(new Vector2f(lon, lat));
                    }
                }
                polygonRings.add(currentRing);
            }
        }
        return polygonRings;
    }


    public static Vector2f convertGeoToOpenGL(Vector2f geoCoord, float countryMinLon, float countryMaxLon, float countryMinLat, float countryMaxLat) {

        float rangeLon = countryMaxLon - countryMinLon;
        float rangeLat = countryMaxLat - countryMinLat;

        if (rangeLon == 0) rangeLon = 0.0001f;
        if (rangeLat == 0) rangeLat = 0.0001f;

        float normalizedX = (geoCoord.x - countryMinLon) / rangeLon;
        float normalizedY = (geoCoord.y - countryMinLat) / rangeLat;

        float aspectRatioCountry = rangeLon / rangeLat;
        float aspectRatioTarget = 1.0f;
        float scaleX = 1.0f;
        float scaleY = 1.0f;

        if (aspectRatioCountry > aspectRatioTarget) {
            scaleY = aspectRatioTarget / aspectRatioCountry;
        } else {
            scaleX = aspectRatioCountry / aspectRatioTarget;
        }

        float openGLX = (normalizedX * 2.0f - 1.0f) * scaleX;
        float openGLY = (normalizedY * 2.0f - 1.0f) * scaleY;

        return new Vector2f(openGLX, openGLY);
    }
}
