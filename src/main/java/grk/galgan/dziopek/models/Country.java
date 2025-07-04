package grk.galgan.dziopek.models;

import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Country {

    private Map<Integer, HistoricalBoundry> boundries;
    private String country;
    private int lastUsed;

    public Country(String country) {
        this.country = country;
        this.boundries = new HashMap<Integer, HistoricalBoundry>();
        this.lastUsed = -1;
    }

    public HistoricalBoundry getBoundry(int year) {
        HistoricalBoundry bestFit = null;
        int difference = 100000;
        for(Integer key : boundries.keySet()) {
            if(year == key) return boundries.get(key);
            if(year >= key) {
                if (year - key < difference) {
                    bestFit = boundries.get(key);
                    difference = year - key;
                }
            }
        }
        return bestFit;
    }

    public void normalize() {

        int maxVertices = 0;
        for (HistoricalBoundry boundry : this.boundries.values()) {
            if (boundry.getGeometries().size() > maxVertices) {
                maxVertices = boundry.getGeometries().size();
            }
        }
        if (maxVertices == 0) return;
        for (HistoricalBoundry boundry : this.boundries.values()) {
            List<Vector2f> geometry = new ArrayList<>(boundry.getGeometries());

            while (geometry.size() < maxVertices) {
                float longestEdgeLength = -1.0f;
                int insertIndex = -1;
                for (int i = 0; i < geometry.size(); i++) {
                    Vector2f p1 = geometry.get(i);
                    Vector2f p2 = geometry.get((i + 1) % geometry.size());

                    float edgeLength = p1.distance(p2);

                    if (edgeLength > longestEdgeLength) {
                        longestEdgeLength = edgeLength;
                        insertIndex = i + 1;
                    }
                }

                if (insertIndex == -1) {
                    break;
                }

                Vector2f p1 = geometry.get((insertIndex - 1 + geometry.size()) % geometry.size());
                Vector2f p2 = geometry.get(insertIndex % geometry.size());

                Vector2f newPoint = new Vector2f(
                        (p1.x + p2.x) / 2.0f,
                        (p1.y + p2.y) / 2.0f
                );

                geometry.add(insertIndex, newPoint);
            }
            boundry.setGeometries(geometry);
        }
    }

    public void assertEquals() {
//        System.out.println(country);
//        System.out.println("Boundries: " + boundries.size());
//        boundries.forEach((key, value) -> {
//            System.out.println(value.getStartYear() + " - " + value.getEndYear());
//        });
    }


    public void add(HistoricalBoundry boundry) {
        boundries.put(boundry.getStartYear(), boundry);
    }

}
