package grk.galgan.dziopek;

import grk.galgan.dziopek.models.Country;
import grk.galgan.dziopek.models.HistoricalBoundry;
import grk.galgan.dziopek.models.scenes.Scenes;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class MapRenderer {

    private ShaderProgram lineShader;

    private int lineVaoId;
    private int lineVboId;
    private int lineVertexCount;

    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
    private Matrix4f modelMatrix;

    private List<Vector2f> points;
    private List<Vector2f> change;

    private Scenes owner;
    private String sceneName;


    public MapRenderer(List<Vector2f> points, List<Vector2f> change, Scenes owner, String sceneName) {
        this.projectionMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        this.modelMatrix = new Matrix4f();
        this.points = new ArrayList<>(points);
        this.change = new ArrayList<>(change);
        this.owner = owner;
        this.sceneName = sceneName;
        init();
    }

    public void init() {
        try {
            lineShader = new ShaderProgram();
            lineShader.createShader("shaders/line.vert", GL_VERTEX_SHADER);
            lineShader.createShader("shaders/line.frag", GL_FRAGMENT_SHADER);
            lineShader.compile();
        } catch (IOException e) {
            System.err.println("Błąd ładowania lub kompilacji shaderów linii: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Nie udało się zainicjalizować shadera linii.");
        }

        lineVaoId = glGenVertexArrays();
        glBindVertexArray(lineVaoId);

        lineVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, lineVboId);

        glBufferData(GL_ARRAY_BUFFER, matchPoints(), GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        lineVertexCount = points.size();
        projectionMatrix.identity().ortho(-1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f);
        viewMatrix.identity();
        modelMatrix.identity();
    }

    public void update(double deltaTime) {
    }

    public void render() {
        lineShader.use();

        lineShader.setUniformMatrix4f("projectionMatrix", projectionMatrix);
        lineShader.setUniformMatrix4f("viewMatrix", viewMatrix);
        lineShader.setUniformMatrix4f("modelMatrix", modelMatrix);
        lineShader.setUniform3f("u_lineColor", 0.0f, 1.0f, 0.0f);
        lineShader.setUniform1f("u_morphFactor", owner.getMorph());
        glLineWidth(20.0f);

        glBindVertexArray(lineVaoId);
        glDrawArrays(GL_LINE_LOOP, 0, lineVertexCount);
        glBindVertexArray(0);

        lineShader.unuse();
    }

    private float[] matchPoints() {
        float[] result = new float[this.points.size() * 3 * 2];
        int pointerIndex = 0;

        for (int i = 0; i < this.points.size(); i++) {
            Vector2f p1 = this.points.get(i);
            Vector2f p2 = this.change.get(i);
            result[pointerIndex++] = p1.x;
            result[pointerIndex++] = p1.y;
            result[pointerIndex++] = 0.0f; // z = 0
            result[pointerIndex++] = p2.x;
            result[pointerIndex++] = p2.y;
            result[pointerIndex++] = 0.0f; // z = 0
        }

        return result;
    }

    public String getSceneName() {
        return sceneName;
    }
}
