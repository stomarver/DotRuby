package engine.visual;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public final class Overlay {

    private static final String VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec2 aPosition;
            layout (location = 1) in vec2 aUv;

            uniform vec2 uResolution;

            out vec2 vUv;

            void main() {
                vec2 clip = (aPosition / uResolution) * 2.0 - 1.0;
                gl_Position = vec4(clip.x, -clip.y, 0.0, 1.0);
                vUv = aUv;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 330 core
            in vec2 vUv;

            uniform sampler2D uTexture;
            uniform vec4 uColor;
            uniform bool uUseTexture;
            uniform bool uDiscardBlack;

            out vec4 fragColor;

            void main() {
                vec4 base = uUseTexture ? texture(uTexture, vUv) : vec4(1.0);
                if (uUseTexture && uDiscardBlack && max(base.r, max(base.g, base.b)) <= 0.01) {
                    discard;
                }
                fragColor = base * uColor;
            }
            """;

    private final float[] vertices = new float[24];

    private int vaoId;
    private int vboId;
    private int programId;
    private int resolutionLocation;
    private int colorLocation;
    private int useTextureLocation;
    private int discardBlackLocation;
    private boolean started;

    public void init() {
        if (programId != 0) {
            return;
        }

        int vertexShader = compileShader(GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        programId = glCreateProgram();
        glAttachShader(programId, vertexShader);
        glAttachShader(programId, fragmentShader);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new IllegalStateException("Unable to link overlay shader: " + glGetProgramInfoLog(programId));
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertices.length * Float.BYTES, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        resolutionLocation = glGetUniformLocation(programId, "uResolution");
        colorLocation = glGetUniformLocation(programId, "uColor");
        useTextureLocation = glGetUniformLocation(programId, "uUseTexture");
        discardBlackLocation = glGetUniformLocation(programId, "uDiscardBlack");
    }

    public void begin(int virtualWidth, int virtualHeight) {
        if (programId == 0) {
            throw new IllegalStateException("Overlay is not initialized");
        }

        started = true;
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glUseProgram(programId);
        glUniform2f(resolutionLocation, virtualWidth, virtualHeight);
        glBindVertexArray(vaoId);
    }

    public void drawTexturedQuad(int textureId, float x, float y, float width, float height) {
        drawTexturedQuadRegion(textureId, x, y, width, height, 0f, 0f, 1f, 1f, false);
    }

    public void drawTexturedQuadRegion(int textureId,
                                       float x,
                                       float y,
                                       float width,
                                       float height,
                                       float minU,
                                       float minV,
                                       float maxU,
                                       float maxV,
                                       boolean discardBlack) {
        ensureStarted();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glUniform4f(colorLocation, 1f, 1f, 1f, 1f);
        glUniform1i(useTextureLocation, 1);
        glUniform1i(discardBlackLocation, discardBlack ? 1 : 0);
        uploadQuad(x, y, x + width, y + height, minU, minV, maxU, maxV);
    }

    public void drawOutlineRect(float minX, float minY, float maxX, float maxY, float thickness) {
        ensureStarted();
        glBindTexture(GL_TEXTURE_2D, 0);
        glUniform4f(colorLocation, 1f, 1f, 1f, 1f);
        glUniform1i(useTextureLocation, 0);
        glUniform1i(discardBlackLocation, 0);
        drawSolidQuad(minX, minY, maxX, minY + thickness);
        drawSolidQuad(minX, maxY - thickness, maxX, maxY);
        drawSolidQuad(minX, minY, minX + thickness, maxY);
        drawSolidQuad(maxX - thickness, minY, maxX, maxY);
    }

    public void end() {
        if (!started) {
            return;
        }
        started = false;
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindVertexArray(0);
        glUseProgram(0);
    }

    public void destroy() {
        end();
        if (vboId != 0) {
            glDeleteBuffers(vboId);
            vboId = 0;
        }
        if (vaoId != 0) {
            glDeleteVertexArrays(vaoId);
            vaoId = 0;
        }
        if (programId != 0) {
            glDeleteProgram(programId);
            programId = 0;
        }
    }

    private void drawSolidQuad(float minX, float minY, float maxX, float maxY) {
        uploadQuad(minX, minY, maxX, maxY, 0f, 0f, 1f, 1f);
    }

    private void uploadQuad(float minX, float minY, float maxX, float maxY, float minU, float minV, float maxU, float maxV) {
        vertices[0] = minX;
        vertices[1] = minY;
        vertices[2] = minU;
        vertices[3] = minV;

        vertices[4] = maxX;
        vertices[5] = minY;
        vertices[6] = maxU;
        vertices[7] = minV;

        vertices[8] = maxX;
        vertices[9] = maxY;
        vertices[10] = maxU;
        vertices[11] = maxV;

        vertices[12] = minX;
        vertices[13] = minY;
        vertices[14] = minU;
        vertices[15] = minV;

        vertices[16] = maxX;
        vertices[17] = maxY;
        vertices[18] = maxU;
        vertices[19] = maxV;

        vertices[20] = minX;
        vertices[21] = maxY;
        vertices[22] = minU;
        vertices[23] = maxV;

        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDrawArrays(GL_TRIANGLES, 0, 6);
    }

    private static int compileShader(int type, String source) {
        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new IllegalStateException("Unable to compile overlay shader: " + glGetShaderInfoLog(shaderId));
        }
        return shaderId;
    }

    private void ensureStarted() {
        if (!started) {
            throw new IllegalStateException("Overlay.begin must be called before drawing");
        }
    }
}
