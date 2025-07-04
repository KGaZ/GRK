package grk.galgan.dziopek;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.opengl.ARBTextureSwizzle.GL_TEXTURE_SWIZZLE_RGBA;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class TextRenderer {

    private ShaderProgram textShader;
    private int vaoId, vboId;

    private int fontTextureId;
    private STBTTBakedChar.Buffer charData;
    private final int FONT_HEIGHT = 24; // Wysokość fontu w pikselach

    public TextRenderer(String fontPath) throws IOException {
        textShader = new ShaderProgram();
        loadFont(fontPath);
    }

    public void init() {
        try {
            textShader.createShader("shaders/text.vert", GL_VERTEX_SHADER);
            textShader.createShader("shaders/text.frag", GL_FRAGMENT_SHADER);
            textShader.compile();
        } catch (IOException e) {
            throw new RuntimeException("Nie udało się załadować shaderów tekstu.", e);
        }

        // Konfiguracja VAO i VBO dla tekstu
        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        // Bufor na 4 wierzchołki (jeden quad), każdy wierzchołek to 4 floaty (x,y,s,t)
        glBufferData(GL_ARRAY_BUFFER, 4 * 4 * Float.BYTES, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 4, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void loadFont(String fontPath) throws IOException {
        // Alokacja bufora na dane o znakach (ASCII 32-127)
        charData = STBTTBakedChar.malloc(96);

        // Generowanie tekstury dla atlasu fontu
        fontTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, fontTextureId);

        // Wczytanie pliku .ttf
        ByteBuffer fontBuffer = ioResourceToByteBuffer(fontPath, 150 * 1024); // 150kb buffer

        // Stworzenie bitmapy 512x512 pikseli dla naszego atlasu
        int bitmapWidth = 512;
        int bitmapHeight = 512;
        ByteBuffer bitmap = BufferUtils.createByteBuffer(bitmapWidth * bitmapHeight);

        // "Wypiekanie" znaków do bitmapy
        STBTruetype.stbtt_BakeFontBitmap(fontBuffer, FONT_HEIGHT, bitmap, bitmapWidth, bitmapHeight, 32, charData);

        // Przesłanie bitmapy jako tekstury do GPU
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, bitmapWidth, bitmapHeight, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        // Ustawienie, aby tekstura nie była czarna (ważne dla jednokanałowych tekstur)
        int[] swizzleMask = {GL_ONE, GL_ONE, GL_ONE, GL_RED};
        glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_RGBA, swizzleMask);

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void renderText(String text, float x, float y, Vector3f color, Matrix4f projection) {
        textShader.use();
        textShader.setUniform3f("text_color", color);
        textShader.setUniformMatrix4f("projection", projection);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, fontTextureId);
        textShader.setUniform1i("text_texture", 0);

        glBindVertexArray(vaoId);

        // Używamy MemoryStack, aby uniknąć problemów z zarządzaniem pamięcią
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xpos = stack.floats(x);
            FloatBuffer ypos = stack.floats(y);

            // Iterujemy po każdym znaku w tekście
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c >= 32 && c < 128) {
                    STBTTAlignedQuad q = STBTTAlignedQuad.mallocStack(stack);
                    STBTruetype.stbtt_GetBakedQuad(charData, 512, 512, c - 32, xpos, ypos, q, true);

                    // Przygotowanie wierzchołków dla jednego znaku (quada)
                    float[] vertices = new float[]{
                            q.x0(), q.y0(), q.s0(), q.t0(),
                            q.x1(), q.y0(), q.s1(), q.t0(),
                            q.x0(), q.y1(), q.s0(), q.t1(),
                            q.x1(), q.y1(), q.s1(), q.t1()
                    };

                    // Aktualizacja danych w VBO i rysowanie
                    glBindBuffer(GL_ARRAY_BUFFER, vboId);
                    glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
                    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
                }
            }
        }

        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);
        textShader.unuse();
    }

    public void cleanup() {
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
        glDeleteTextures(fontTextureId);
        textShader.cleanup();
        if (charData != null) {
            charData.free();
        }
    }

    // Pomocnicza funkcja do wczytywania zasobów
    private ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;
        Path path = Paths.get(resource);
        if (Files.isReadable(path)) {
            try (SeekableByteChannel fc = Files.newByteChannel(path)) {
                buffer = BufferUtils.createByteBuffer((int) fc.size() + 1);
                while (fc.read(buffer) != -1) ;
            }
        } else {
            try (
                    InputStream source = TextRenderer.class.getClassLoader().getResourceAsStream(resource);
                    ReadableByteChannel rbc = Channels.newChannel(source)) {
                buffer = BufferUtils.createByteBuffer(bufferSize);
                while (true) {
                    int bytes = rbc.read(buffer);
                    if (bytes == -1) break;
                    if (buffer.remaining() == 0) buffer = resizeBuffer(buffer, buffer.capacity() * 2);
                }
            }
        }
        buffer.flip();
        return buffer;
    }

    private ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }
}