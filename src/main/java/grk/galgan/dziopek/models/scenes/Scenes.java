package grk.galgan.dziopek.models.scenes;

import grk.galgan.dziopek.MapRenderer;
import grk.galgan.dziopek.models.Country;

import java.util.Map;

public class Scenes {

    private MapRenderer[] renderers;
    private Map<String, Country> data;
    public float morph[];
    public int selected[];

    public Scenes(Map<String, Country> data) {
        this.morph = new float[]{0.0f};
        this.data = data;
        this.selected = new int[]{0};
        this.renderers = new MapRenderer[]{
                generateRenderer("Poland", 1935, 1960, "II Rzeczpospolita Polska -> III Rzeczpospolita Polska"),
                generateRenderer("Germany (Prussia)", 1887, 1919, "Prusy (Przed 1918) -> Niemcy (Po 1918)"),
                generateRenderer("Yugoslavia", 1930, 2015, "Zmiany Jugoslawi 1930 -> 1990"),
                generateRenderer("Ukraine", 2011, 2025, "Ukraina po utranie Kievu"),
                generateRenderer("Czechoslovakia", 1920, 1938, "Czechosłowacja Robiąca taniec w latach 1920-1938"),
                generateRenderer("Israel", 1950, 2019, "Izrael 1950-2019")
        };

    }

    private MapRenderer generateRenderer(String country, int startYear, int endYear, String sceneName) {
        return new MapRenderer(data.get(country).getBoundry(startYear).getBoundries(0, 0),
                                data.get(country).getBoundry(endYear).getBoundries(0, 0), this, sceneName);
    }

    public void render() {
        renderers[selected[0]].render();
    }
    public void update(double ups) {
        renderers[selected[0]].update(ups);
    }

    public float getMorph() {
        return morph[0];
    }

    public int getMaxScenes() {
        return renderers.length;
    }

    public String getSceneName() {
        return renderers[selected[0]].getSceneName();
    }
}
