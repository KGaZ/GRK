package grk.galgan.dziopek.models;

import grk.galgan.dziopek.utils.GeoJsonParser;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class HistoricalBoundry {

    private final int startYear;
    private final int endYear;
    private final String countryName;
    private final List<Vector2f> geometries;


    public HistoricalBoundry(int startYear, int endYear, String countryName, List<Vector2f> geometries) {
        this.startYear = startYear;
        this.endYear = endYear;
        this.countryName = countryName;
        this.geometries = geometries;
    }

    public int getStartYear() {
        return startYear;
    }

    public int getEndYear() {
        return endYear;
    }

    public List<Vector2f> getGeometries() {
        return geometries;
    }

    public void setGeometries(List<Vector2f> geometries) {
        this.geometries.clear();
        this.geometries.addAll(geometries);
    }

    public List<Vector2f> getBoundries(int x, int y) {
        List<Vector2f> verticies = new ArrayList<>(getGeometries());

        float minLat = 300, maxLat = 0, minLon = 300, maxLon = 0;
        for (Vector2f vertex : verticies) {
            if (vertex.x < minLon) minLon = vertex.x;
            if (vertex.x > maxLon) maxLon = vertex.x;
            if(vertex.y < minLat) minLat = vertex.y;
            if (vertex.y > maxLat) maxLat = vertex.y;
        }

        float finalMinLon = minLon - 2;
        float finalMaxLon = maxLon + 2;
        float finalMinLat = minLat - 2;
        float finalMaxLat = maxLat + 2;
        return verticies.stream().map(point -> GeoJsonParser.convertGeoToOpenGL(point, finalMinLon, finalMaxLon, finalMinLat, finalMaxLat)).toList();
    }
}
