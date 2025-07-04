package grk.galgan.dziopek;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;

public class ShaderProgram {

    private final int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    private int geometryShaderId;
    private static FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public ShaderProgram() {
        programId = glCreateProgram();
        if (programId == 0) {
            throw new RuntimeException("Nie udało się utworzyć programu shadera!");
        }
    }

    public int createShader(String filename, int shaderType) throws IOException {
        String shaderSource = loadResource(filename);
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new RuntimeException("Nie udało się utworzyć shadera! Typ: " + shaderType);
        }

        glShaderSource(shaderId, shaderSource);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Błąd kompilacji shadera: " + filename);
            System.err.println(glGetShaderInfoLog(shaderId, 1024));
            throw new RuntimeException("Błąd kompilacji shadera!");
        }

        glAttachShader(programId, shaderId);

        switch (shaderType) {
            case GL_VERTEX_SHADER:
                this.vertexShaderId = shaderId;
                break;
            case GL_FRAGMENT_SHADER:
                this.fragmentShaderId = shaderId;
                break;
            case GL_GEOMETRY_SHADER:
                this.geometryShaderId = shaderId;
                break;
        }

        return shaderId;
    }

    private String loadResource(String filename) throws IOException {
        StringBuilder result = new StringBuilder();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }

    public void compile() {
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            System.err.println("Błąd linkowania programu shadera:");
            System.err.println(glGetProgramInfoLog(programId, 1024));
            throw new RuntimeException("Błąd linkowania programu shadera!");
        }

        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == GL_FALSE) {
            System.err.println("Błąd walidacji programu shadera:");
            System.err.println(glGetProgramInfoLog(programId, 1024));
        }
    }

    public void use() {
        glUseProgram(programId);
    }

    public void unuse() {
        glUseProgram(0);
    }

    public void setUniformMatrix4f(String uniformName, Matrix4f matrix) {
        int location = glGetUniformLocation(programId, uniformName);
        if (location == -1) {
            return;
        }
        matrix.get(matrixBuffer);
        glUniformMatrix4fv(location, false, matrixBuffer);
    }

    public void setUniform1i(String uniformName, int value) {
        int location = glGetUniformLocation(programId, uniformName);
        if (location == -1) return;
        glUniform1i(location, value);
    }

    public void setUniform1f(String uniformName, float value) {
        int location = glGetUniformLocation(programId, uniformName);
        if (location == -1) return;
        glUniform1f(location, value);
    }

    public void setUniform3f(String uniformName, float x, float y, float z) {
        int location = glGetUniformLocation(programId, uniformName);
        if (location == -1) return;
        glUniform3f(location, x, y, z);
    }

    public void setUniform3f(String uniformName, Vector3f value) {
        int location = glGetUniformLocation(programId, uniformName);
        if (location == -1) return;
        glUniform3f(location, value.x, value.y, value.z);
    }

    public void setUniformBool(String uniformName, boolean value) {
        setUniform1i(uniformName, value ? 1 : 0);
    }

    public void cleanup() {
        unuse();
        if (programId != 0) {
            if (vertexShaderId != 0) {
                glDetachShader(programId, vertexShaderId);
                glDeleteShader(vertexShaderId);
            }
            if (fragmentShaderId != 0) {
                glDetachShader(programId, fragmentShaderId);
                glDeleteShader(fragmentShaderId);
            }
            glDeleteProgram(programId);
        }
    }
}