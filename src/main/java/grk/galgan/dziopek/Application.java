package grk.galgan.dziopek;

import grk.galgan.dziopek.models.scenes.Scenes;
import grk.galgan.dziopek.utils.GeoJsonParser;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13C.GL_MULTISAMPLE;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Application {

    private final int width;
    private final int height;
    private final String title;
    private long window;
    private Scenes scenes;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    public Application(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public void run() {
        init();
        loop();
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
        Callbacks.glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public void init() {

        GLFWErrorCallback.createPrint(System.err).set();

        if(!glfwInit()) {
            throw new IllegalStateException("Nie udalo sie zaladowac silnika graficznego.");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        this.window = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);

        glfwSetKeyCallback(this.window, (window, key, scancode, action, mods) -> {
           if(key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
               glfwSetWindowShouldClose(this.window, true);
           }
        });

        glfwSetWindowPos(
                window,
                (glfwGetVideoMode(glfwGetPrimaryMonitor()).width() - width) / 2,
                (glfwGetVideoMode(glfwGetPrimaryMonitor()).height() - height) / 2);

        glfwMakeContextCurrent(this.window);
        glfwSwapInterval(1);
        glfwShowWindow(this.window);
        GL.createCapabilities();
        glEnable(GL_MULTISAMPLE);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glLineWidth(20.0f);
        glEnable(GL_LINE_SMOOTH);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        ImGui.createContext();
        imGuiGlfw.init(this.window, true);
        imGuiGl3.init("#version 330");

        // inicjalizacja innych klas
        this.scenes = new Scenes(new GeoJsonParser().parseGeoJson("dane.geojson"));

    }

    public void loop() {
        glClearColor(1f, 1f, 1f, 1.0f);

        long lastTime = System.nanoTime();
        double acc = 0.0;
        final double UPS_CAP = 1.0 / 60.0;

        while(!glfwWindowShouldClose(this.window)) {
            long now = System.nanoTime();
            double delta = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;
            acc += delta;

            while(acc >= UPS_CAP) {
                update(UPS_CAP);
                acc -= UPS_CAP;
            }

            render();
            glfwPollEvents();
        }
    }

    public void update(double ups) {
        scenes.update(ups);
    }

    public void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // render
        scenes.render();

        imGuiGlfw.newFrame();
        ImGui.newFrame();

        ImGui.begin("Panel Sterowania");
        ImGui.sliderFloat("Morph", scenes.morph, 0.0f, 1.0f);
        ImGui.sliderInt("Scena", scenes.selected, 0, scenes.getMaxScenes()-1);
        ImGui.text("Wybrana Scena: "+scenes.getSceneName());
        ImGui.end();

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

        glfwSwapBuffers(this.window);
    }


}
